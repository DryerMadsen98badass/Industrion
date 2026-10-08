package net.mads.industron.material.forging;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.mads.industron.recipe.recipetypes.assembly.input.AssemblyUseState;
import net.mads.industron.network.AssemblyNextStepPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/** Manual hot-metal forging performed directly on the three vanilla anvil blocks. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class AnvilForgingRuntime {
    /** Vanilla anvil stages are intentionally long-lived because this workstation is used heavily. */
    private static final int WEAR_PER_STAGE = 256;
    /** Vanilla Bucket is a material-agnostic forged target made from exactly three ingot-units. */
    private static final int BUCKET_MATERIAL_AMOUNT_MB = 432;
    /** Fixed irregular target for the three-ingot vanilla Bucket route. */
    private static final int BUCKET_FORGE_VALUE = 743;
    private static final String VISUAL_TAG = "industron_anvil_forge_visual";
    private static final String POSITION_TAG_PREFIX = "industron_anvil_pos_";
    /** One timed forge action per player, mirroring Assembly's hold-right-click tool timing. */
    private static final Map<UUID, ActiveToolUse> ACTIVE_TOOL_USES = new HashMap<>();

    private AnvilForgingRuntime() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof AnvilBlock)) return;

        // Industron owns vanilla anvils completely: never open the vanilla repair/enchantment GUI.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = event.getPos();
        AnvilForgeSavedData data = AnvilForgeSavedData.get(level);
        AnvilForgeSavedData.Session session = data.session(pos).orElse(null);
        ItemStack held = player.getMainHandItem();

        if (player.isShiftKeyDown()) {
            clearActiveToolUse(player);
            if (session != null) cancelAndRefund(level, pos, player, data, session);
            return;
        }

        if (session == null) {
            clearActiveToolUse(player);
            if (net.mads.industron.tool.ToolRepairRuntime.tryRepair(level, pos, player)) return;
            insertFirst(level, pos, player, held, data);
            return;
        }

        ensureVisual(level, pos, session);

        if (held.isEmpty()) {
            clearActiveToolUse(player);
            claimStableResult(level, pos, player, data, session);
            return;
        }

        if (tryAddMatchingInput(level, pos, player, held, data, session)) {
            clearActiveToolUse(player);
            return;
        }
        beginToolUse(level, pos, player, held, session);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getState().getBlock() instanceof AnvilBlock)) return;
        BlockPos pos = event.getPos();
        AnvilForgeSavedData data = AnvilForgeSavedData.get(level);
        AnvilForgeSavedData.Session session = data.session(pos).orElse(null);
        if (session != null) dropRefund(level, pos, session);
        cancelToolUsesAt(level, pos);
        removeVisual(level, pos);
        data.clearAll(pos);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ActiveToolUse active = ACTIVE_TOOL_USES.get(player.getUUID());
        if (active == null) return;
        if (!(player.level() instanceof ServerLevel level)) {
            clearActiveToolUse(player);
            return;
        }

        if (!active.dimension().equals(level.dimension().location().toString())
                || !(level.getBlockState(active.pos()).getBlock() instanceof AnvilBlock)
                || player.distanceToSqr(active.pos().getX() + 0.5D, active.pos().getY() + 0.5D, active.pos().getZ() + 0.5D) > 36.0D) {
            clearActiveToolUse(player);
            return;
        }

        // Give the use-key sync packet a tiny grace period after the initial block interaction.
        if (!AssemblyUseState.isHeld(player)) {
            if (level.getGameTime() - active.startedAtGameTime() <= 2L) return;
            clearActiveToolUse(player);
            return;
        }

        ItemStack held = player.getMainHandItem();
        ToolVariantDefinition currentTool = AssemblyTools.findAny(held);
        if (currentTool == null || !currentTool.equals(active.tool())) {
            clearActiveToolUse(player);
            return;
        }

        AnvilForgeSavedData data = AnvilForgeSavedData.get(level);
        AnvilForgeSavedData.Session session = data.session(active.pos()).orElse(null);
        ForgeOperation operation = session == null ? null : resolveForgeOperation(held, session);
        if (operation == null
                || !operation.tool().equals(active.tool())
                || operation.nextForgeValue() != active.nextForgeValue()) {
            clearActiveToolUse(player);
            return;
        }

        int duration = Math.max(1, active.tool().useTimeTicks());
        int progress = Math.min(duration, active.progressTicks() + 1);
        active = active.withProgress(progress);
        ACTIVE_TOOL_USES.put(player.getUUID(), active);
        syncToolProgress(player, active);
        if (progress < duration) return;

        ACTIVE_TOOL_USES.remove(player.getUUID());
        commitToolUse(level, active.pos(), player, held, data, session, active.nextForgeValue());
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
    }

    private static void insertFirst(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            ItemStack held,
            AnvilForgeSavedData data
    ) {
        MaterialLookup.MaterialTarget target = MaterialLookup.find(held);
        if (!validHotInput(target)) return;

        MaterialPart cold = target.part().coldForgePart();
        AnvilForgeSavedData.Session session = data.start(pos, target.material().id(), cold);
        if (!player.isCreative()) held.shrink(1);
        ensureVisual(level, pos, session);
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.45F, 1.3F);
    }

    private static boolean tryAddMatchingInput(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            ItemStack held,
            AnvilForgeSavedData data,
            AnvilForgeSavedData.Session session
    ) {
        MaterialLookup.MaterialTarget target = MaterialLookup.find(held);
        if (!validHotInput(target)) return false;
        MaterialPart cold = target.part().coldForgePart();
        if (!target.material().id().equals(session.materialId()) || cold != session.originalPart()) return false;
        // Material can only be added before the first shape-changing operation.
        if (session.worked()) return true;

        data.addOriginalItem(pos);
        if (!player.isCreative()) held.shrink(1);
        ensureVisual(level, pos, data.session(pos).orElseThrow());
        level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.35F, 1.45F);
        return true;
    }

    private static void beginToolUse(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            ItemStack held,
            AnvilForgeSavedData.Session session
    ) {
        ForgeOperation operation = resolveForgeOperation(held, session);
        if (operation == null) {
            clearActiveToolUse(player);
            return;
        }

        String dimension = level.dimension().location().toString();
        ActiveToolUse existing = ACTIVE_TOOL_USES.get(player.getUUID());
        if (existing != null
                && existing.dimension().equals(dimension)
                && existing.pos().equals(pos)
                && existing.tool().equals(operation.tool())
                && existing.nextForgeValue() == operation.nextForgeValue()) {
            syncToolProgress(player, existing);
            return;
        }

        ActiveToolUse active = new ActiveToolUse(
                dimension,
                pos.immutable(),
                operation.tool(),
                operation.definition().displayName(),
                operation.nextForgeValue(),
                0,
                level.getGameTime()
        );
        ACTIVE_TOOL_USES.put(player.getUUID(), active);
        syncToolProgress(player, active);
    }

    private static ForgeOperation resolveForgeOperation(ItemStack held, AnvilForgeSavedData.Session session) {
        ToolVariantDefinition variant = AssemblyTools.findAny(held);
        if (variant == null) return null;
        ToolDefinition definition = AssemblyTools.definition(variant.type());
        if (definition == null || !definition.canForgeOnAnvil()) return null;

        OptionalInt evaluated = definition.applyForgeFormula(session.forgeValue());
        if (evaluated.isEmpty()) return null;
        int next = evaluated.getAsInt();
        if (next == session.forgeValue()) return null;

        // Values may overshoot 1000, but while above 1000 only decreasing operations are legal.
        if (session.forgeValue() > 1000 && next > session.forgeValue()) return null;
        return new ForgeOperation(variant, definition, next);
    }

    private static void commitToolUse(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            ItemStack held,
            AnvilForgeSavedData data,
            AnvilForgeSavedData.Session session,
            int next
    ) {
        IndustrialMaterial material = material(session.materialId());
        if (material == null) return;
        int totalAmount = Math.multiplyExact(session.originalPart().materialAmountMb(), session.originalCount());
        MaterialPart stable = stablePart(material, totalAmount, next);
        data.setForgeValue(pos, next, stable);
        AnvilForgeSavedData.Session updated = data.session(pos).orElseThrow();
        ensureVisual(level, pos, updated);

        if (!player.isCreative() && held.isDamageableItem()) {
            held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35F, 1.5F);

        int wear = wearCost(material);
        if (damageAnvil(level, pos, data, wear)) {
            // The anvil broke under the work. Material is conserved, but shape progress is lost.
            dropRefund(level, pos, updated);
            removeVisual(level, pos);
            data.clearAll(pos);
        }
    }

    private static void syncToolProgress(ServerPlayer player, ActiveToolUse active) {
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.showToolProgress(
                "Anvil Forging",
                active.dimension(),
                active.pos(),
                active.toolName(),
                active.progressTicks(),
                active.tool().useTimeTicks()
        ));
    }

    private static void clearActiveToolUse(ServerPlayer player) {
        if (player == null) return;
        if (ACTIVE_TOOL_USES.remove(player.getUUID()) != null) {
            PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
        }
    }

    private static void cancelToolUsesAt(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();
        for (UUID playerId : java.util.Set.copyOf(ACTIVE_TOOL_USES.keySet())) {
            ActiveToolUse active = ACTIVE_TOOL_USES.get(playerId);
            if (active == null || !active.dimension().equals(dimension) || !active.pos().equals(pos)) continue;
            ACTIVE_TOOL_USES.remove(playerId);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
        }
    }

    private record ForgeOperation(ToolVariantDefinition tool, ToolDefinition definition, int nextForgeValue) {
    }

    private record ActiveToolUse(
            String dimension,
            BlockPos pos,
            ToolVariantDefinition tool,
            String toolName,
            int nextForgeValue,
            int progressTicks,
            long startedAtGameTime
    ) {
        private ActiveToolUse withProgress(int progress) {
            return new ActiveToolUse(
                    dimension, pos, tool, toolName, nextForgeValue, progress, startedAtGameTime
            );
        }
    }

    private static void claimStableResult(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            AnvilForgeSavedData data,
            AnvilForgeSavedData.Session session
    ) {
        if (!session.worked()) return;
        IndustrialMaterial material = material(session.materialId());
        if (material == null) return;
        int totalAmount = Math.multiplyExact(session.originalPart().materialAmountMb(), session.originalCount());
        if (isBucketTarget(material, totalAmount, session.forgeValue())) {
            giveOrDrop(player, new ItemStack(Items.BUCKET));
            removeVisual(level, pos);
            data.clearSession(pos);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.25F);
            return;
        }

        MaterialPart stable = stablePart(material, totalAmount, session.forgeValue());
        if (stable == null) return;

        MaterialPart hot = stable.hotForgePart();
        ItemStack result = stack(material, hot, 1);
        if (result.isEmpty()) return;
        giveOrDrop(player, result);
        removeVisual(level, pos);
        data.clearSession(pos);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.25F);
    }

    private static void cancelAndRefund(
            ServerLevel level,
            BlockPos pos,
            Player player,
            AnvilForgeSavedData data,
            AnvilForgeSavedData.Session session
    ) {
        IndustrialMaterial material = material(session.materialId());
        MaterialPart refundPart = data.removeOneAndReset(pos);
        if (material != null && refundPart != null) {
            ItemStack refund = stack(material, refundPart.hotForgePart(), 1);
            if (!refund.isEmpty()) giveOrDrop(player, refund);
        }

        AnvilForgeSavedData.Session remaining = data.session(pos).orElse(null);
        if (remaining == null) {
            removeVisual(level, pos);
        } else {
            ensureVisual(level, pos, remaining);
        }
    }

    private static void dropRefund(ServerLevel level, BlockPos pos, AnvilForgeSavedData.Session session) {
        IndustrialMaterial material = material(session.materialId());
        if (material == null) return;
        ItemStack refund = stack(material, session.originalPart().hotForgePart(), session.originalCount());
        if (refund.isEmpty()) return;
        ItemEntity dropped = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D, refund);
        dropped.setDefaultPickUpDelay();
        level.addFreshEntity(dropped);
    }

    private static boolean validHotInput(MaterialLookup.MaterialTarget target) {
        if (target == null || target.magnetic()) return false;
        MaterialPart hot = target.part();
        if (!hot.isHotForgePart()) return false;
        MaterialPart cold = hot.coldForgePart();
        return cold.isForgeableForm()
                && target.material().has(cold)
                && target.material().has(hot);
    }

    private static boolean isBucketTarget(IndustrialMaterial material, int amountMb, int forgeValue) {
        return material != null
                && material.properties().metal()
                && amountMb == BUCKET_MATERIAL_AMOUNT_MB
                && forgeValue == BUCKET_FORGE_VALUE;
    }

    private static MaterialPart stablePart(IndustrialMaterial material, int amountMb, int forgeValue) {
        for (MaterialPart part : MaterialPart.values()) {
            if (part.isHotForgePart() || !part.isForgeableForm()) continue;
            if (part.materialAmountMb() != amountMb || part.forgeValue() != forgeValue) continue;
            MaterialPart hot = part.hotForgePart();
            if (hot != null && material.has(part) && material.has(hot)) return part;
        }
        return null;
    }

    private static int wearCost(IndustrialMaterial material) {
        MaterialProperties properties = material.properties();
        int hardness = properties.hasProperty("hardness") ? properties.hardness() : 0;
        int formability = properties.formability();
        // Only 1 or 2 wear per successful operation. Difficult/hard metals use 2.
        return hardness >= 70 || formability <= 30 ? 2 : 1;
    }

    /** @return true if the final damaged anvil broke. */
    private static boolean damageAnvil(ServerLevel level, BlockPos pos, AnvilForgeSavedData data, int amount) {
        int wear = data.addWear(pos, amount);
        if (wear < WEAR_PER_STAGE) return false;

        BlockState current = level.getBlockState(pos);
        Block nextBlock;
        if (current.is(Blocks.ANVIL)) nextBlock = Blocks.CHIPPED_ANVIL;
        else if (current.is(Blocks.CHIPPED_ANVIL)) nextBlock = Blocks.DAMAGED_ANVIL;
        else if (current.is(Blocks.DAMAGED_ANVIL)) nextBlock = Blocks.AIR;
        else return false;

        data.setWear(pos, wear - WEAR_PER_STAGE);
        if (nextBlock == Blocks.AIR) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
            return true;
        }

        BlockState next = nextBlock.defaultBlockState();
        if (current.hasProperty(AnvilBlock.FACING) && next.hasProperty(AnvilBlock.FACING)) {
            next = next.setValue(AnvilBlock.FACING, current.getValue(AnvilBlock.FACING));
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.8F);
        return false;
    }

    private static IndustrialMaterial material(String id) {
        if (id == null) return null;
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (material.id().equals(id)) return material;
        }
        return null;
    }

    private static ItemStack stack(IndustrialMaterial material, MaterialPart part, int count) {
        if (material == null || part == null || count <= 0 || !material.has(part)) return ItemStack.EMPTY;
        ResourceLocation id = material.hasExistingPart(part)
                ? material.existingPart(part)
                : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    private static void ensureVisual(ServerLevel level, BlockPos pos, AnvilForgeSavedData.Session session) {
        IndustrialMaterial material = material(session.materialId());
        if (material == null) return;
        MaterialPart hotVisual = session.visualPart().hotForgePart();
        int visualCount = session.worked() && session.visualPart() != session.originalPart()
                ? 1
                : session.originalCount();
        ItemStack stack = stack(material, hotVisual, visualCount);
        if (stack.isEmpty()) return;
        AnvilForgeDebugData.write(stack, pos, session.forgeValue());

        ItemEntity entity = findVisual(level, pos);
        if (entity == null) {
            entity = new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.12D,
                    pos.getZ() + 0.5D,
                    stack
            );
            entity.addTag(VISUAL_TAG);
            entity.addTag(positionTag(pos));
            entity.setNoGravity(true);
            entity.setInvulnerable(true);
            entity.setNeverPickUp();
            entity.setUnlimitedLifetime();
            entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level.addFreshEntity(entity);
        } else {
            entity.setItem(stack);
            entity.setPos(pos.getX() + 0.5D, pos.getY() + 1.12D, pos.getZ() + 0.5D);
            entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    private static ItemEntity findVisual(ServerLevel level, BlockPos pos) {
        String positionTag = positionTag(pos);
        AABB box = new AABB(pos).inflate(1.25D).move(0.0D, 0.75D, 0.0D);
        for (ItemEntity entity : level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                candidate -> candidate.getTags().contains(VISUAL_TAG)
                        && candidate.getTags().contains(positionTag)
        )) {
            return entity;
        }
        return null;
    }

    private static void removeVisual(ServerLevel level, BlockPos pos) {
        ItemEntity entity = findVisual(level, pos);
        if (entity != null) entity.discard();
    }

    private static String positionTag(BlockPos pos) {
        return POSITION_TAG_PREFIX + pos.asLong();
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        ItemStack remaining = stack.copy();
        if (player.addItem(remaining)) return;
        player.drop(remaining, false);
    }
}
