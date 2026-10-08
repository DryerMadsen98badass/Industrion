package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.menu.StoneShapingMenu;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.tool.ToolMaterialRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureMaterialItem extends Item {
    private final StructureMaterial material;
    private final MaterialPart part;

    public StructureMaterialItem(StructureMaterial material, MaterialPart part) {
        super(ToolMaterialRules.isToolPartForm(part)
                ? new Item.Properties().stacksTo(1)
                : new Item.Properties());
        this.material = material;
        this.part = part;
    }

    public StructureMaterial material() {
        return material;
    }

    public MaterialPart part() {
        return part;
    }

    /**
     * Pebbles stay normal StructureMaterialItems in inventories, but can place the matching
     * loose-pebble world block. This keeps one item identity per material while worldgen and
     * player placement use the exact same PebbleWorldgenBlock.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (part != MaterialPart.PEBBLE || !(material instanceof StoneMaterial stone)) {
            return super.useOn(context);
        }

        var pebbleHolder = BlockRegistry.getPebbleWorldgenBlock(stone);
        if (pebbleHolder == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = placeContext.getClickedPos();

        if (player != null && !player.mayUseItemAt(placePos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }

        BlockState replaced = level.getBlockState(placePos);
        if (!replaced.canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }

        BlockState pebbleState = pebbleHolder.get().defaultBlockState();
        if (!pebbleState.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            if (!level.setBlock(placePos, pebbleState, 11)) {
                return InteractionResult.FAIL;
            }

            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (part != MaterialPart.PEBBLE || hand != InteractionHand.MAIN_HAND) {
            return super.use(level, player, hand);
        }

        ItemStack offHand = player.getOffhandItem();
        if (!(offHand.getItem() instanceof StructureMaterialItem other)
                || other.part() != MaterialPart.PEBBLE
                || !other.material().id().equals(material.id())) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide() && !StoneShapingMenu.open(player)) {
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
