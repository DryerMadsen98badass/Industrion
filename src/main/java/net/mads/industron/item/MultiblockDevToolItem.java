package net.mads.industron.item;

import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiblockDevToolItem extends Item {
    private static final String DATA_KEY = "MultiblockDevTool";
    private static final String POS_1_KEY = "Pos1";
    private static final String POS_2_KEY = "Pos2";
    private static final String CONTROLLER_KEY = "Controller";
    private static final String CONTROLLER_FACING_KEY = "ControllerFacing";
    private static final String SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final char IGNORE_SYMBOL = ' ';
    private static final long MAX_VOLUME = 262_144L;

    public MultiblockDevToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Hold Create Tool Menu key + scroll: change mode").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click: use selected mode").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Hold Ctrl: select in air / Ctrl + scroll: adjust").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Air inside selection exports as ' ' (ignored)").withStyle(ChatFormatting.DARK_GRAY));

        Selection selection = selection(stack);
        if (selection.pos1() != null) {
            tooltip.add(Component.literal("Pos 1: " + formatPos(selection.pos1())).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (selection.pos2() != null) {
            tooltip.add(Component.literal("Pos 2: " + formatPos(selection.pos2())).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (selection.controller() != null) {
            tooltip.add(Component.literal(
                    "Controller: " + formatPos(selection.controller()) + " " + selection.controllerFacing().getSerializedName().toUpperCase()
            ).withStyle(ChatFormatting.GOLD));
        }
    }

    public static void setPos1(ItemStack stack, BlockPos pos) {
        updateSelectionTag(stack, tag -> tag.putLong(POS_1_KEY, pos.asLong()));
    }

    public static void setPos2(ItemStack stack, BlockPos pos) {
        updateSelectionTag(stack, tag -> tag.putLong(POS_2_KEY, pos.asLong()));
    }

    public static void setController(ItemStack stack, BlockPos pos, Direction facing) {
        updateSelectionTag(stack, tag -> {
            tag.putLong(CONTROLLER_KEY, pos.asLong());
            tag.putInt(CONTROLLER_FACING_KEY, facing.ordinal());
        });
    }


    public static void clearSelection(ItemStack stack) {
        CompoundTag root = stack.get(DataComponents.CUSTOM_DATA) == null
                ? new CompoundTag()
                : stack.get(DataComponents.CUSTOM_DATA).copyTag();
        root.remove(DATA_KEY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static boolean sendExportIfReady(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Selection selection = selection(stack);
        if (!selection.complete()) {
            player.sendSystemMessage(Component.literal(
                    "Multiblock Dev Tool: select both corners and a controller before saving."
            ).withStyle(ChatFormatting.RED));
            return false;
        }

        ExportResult export = buildExport(level, selection);
        if (export.error() != null) {
            player.sendSystemMessage(Component.literal(export.error()).withStyle(ChatFormatting.RED));
            return false;
        }

        player.sendSystemMessage(Component.literal(
                "Multiblock captured: " + export.width() + " x " + export.height() + " x " + export.length()
        ).withStyle(ChatFormatting.GREEN));

        MutableComponent copyPattern = Component.literal("[Copy Pattern]")
                .setStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, export.pattern())));
        MutableComponent copyFull = Component.literal("[Copy Full Variant]")
                .setStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, export.fullVariant())));
        player.sendSystemMessage(Component.empty().append(copyPattern).append(Component.literal("  ")).append(copyFull));
        return true;
    }

    public static Selection selection(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Selection.empty();
        }

        CompoundTag root = customData.copyTag();
        if (!root.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return Selection.empty();
        }

        CompoundTag tag = root.getCompound(DATA_KEY);
        BlockPos pos1 = tag.contains(POS_1_KEY, Tag.TAG_LONG) ? BlockPos.of(tag.getLong(POS_1_KEY)) : null;
        BlockPos pos2 = tag.contains(POS_2_KEY, Tag.TAG_LONG) ? BlockPos.of(tag.getLong(POS_2_KEY)) : null;
        BlockPos controller = tag.contains(CONTROLLER_KEY, Tag.TAG_LONG) ? BlockPos.of(tag.getLong(CONTROLLER_KEY)) : null;
        int facingOrdinal = tag.getInt(CONTROLLER_FACING_KEY);
        Direction[] directions = Direction.values();
        Direction facing = facingOrdinal >= 0 && facingOrdinal < directions.length ? directions[facingOrdinal] : Direction.NORTH;
        if (facing == Direction.UP || facing == Direction.DOWN) {
            facing = Direction.NORTH;
        }
        return new Selection(pos1, pos2, controller, facing);
    }

    private static void updateSelectionTag(ItemStack stack, java.util.function.Consumer<CompoundTag> updater) {
        CompoundTag root = stack.get(DataComponents.CUSTOM_DATA) == null
                ? new CompoundTag()
                : stack.get(DataComponents.CUSTOM_DATA).copyTag();
        CompoundTag selection = root.contains(DATA_KEY, Tag.TAG_COMPOUND)
                ? root.getCompound(DATA_KEY)
                : new CompoundTag();
        updater.accept(selection);
        root.put(DATA_KEY, selection);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static ExportResult buildExport(ServerLevel level, Selection selection) {
        BlockPos pos1 = selection.pos1();
        BlockPos pos2 = selection.pos2();
        BlockPos controller = selection.controller();
        if (!insideSelection(controller, pos1, pos2)) {
            return ExportResult.error("Multiblock Dev Tool: controller must be inside Pos 1 / Pos 2 selection.");
        }

        int sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > MAX_VOLUME) {
            return ExportResult.error("Multiblock Dev Tool: selection is too large (" + volume + " blocks, max " + MAX_VOLUME + ").");
        }

        Direction facing = selection.controllerFacing();
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        LocalBounds bounds = localBounds(pos1, pos2, controller, right, forward);

        Map<ExportTarget, Character> blockSymbols = new LinkedHashMap<>();
        for (int localX = bounds.minX(); localX <= bounds.maxX(); localX++) {
            for (int localY = bounds.minY(); localY <= bounds.maxY(); localY++) {
                for (int localZ = bounds.minZ(); localZ <= bounds.maxZ(); localZ++) {
                    BlockPos worldPos = worldPos(controller, right, forward, localX, localY, localZ);
                    if (worldPos.equals(controller)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(worldPos);
                    if (state.isAir()) {
                        continue;
                    }
                    ExportTarget target = exportTarget(state);
                    if (!blockSymbols.containsKey(target)) {
                        if (blockSymbols.size() >= SYMBOLS.length() - 1) {
                            return ExportResult.error("Multiblock Dev Tool: more than 51 unique non-air blocks/abilities. Only a-z and A-Z are supported including the controller.");
                        }
                        blockSymbols.put(target, SYMBOLS.charAt(blockSymbols.size()));
                    }
                }
            }
        }

        char controllerSymbol = SYMBOLS.charAt(blockSymbols.size());
        String pattern = buildPattern(level, controller, right, forward, bounds, blockSymbols, controllerSymbol);
        String fullVariant = buildFullVariant(pattern, blockSymbols, controllerSymbol);
        return new ExportResult(
                pattern,
                fullVariant,
                bounds.maxX() - bounds.minX() + 1,
                bounds.maxY() - bounds.minY() + 1,
                bounds.maxZ() - bounds.minZ() + 1,
                null
        );
    }

    private static String buildPattern(
            ServerLevel level,
            BlockPos controller,
            Direction right,
            Direction forward,
            LocalBounds bounds,
            Map<ExportTarget, Character> blockSymbols,
            char controllerSymbol
    ) {
        StringBuilder code = new StringBuilder();
        code.append(".machineDefinition(Option.variant(\"1\", pattern -> pattern\n");
        for (int localX = bounds.minX(); localX <= bounds.maxX(); localX++) {
            code.append("        .layer(\n");
            for (int localY = bounds.minY(); localY <= bounds.maxY(); localY++) {
                code.append("                row(");
                for (int localZ = bounds.minZ(); localZ <= bounds.maxZ(); localZ++) {
                    if (localZ > bounds.minZ()) {
                        code.append(", ");
                    }
                    BlockPos worldPos = worldPos(controller, right, forward, localX, localY, localZ);
                    char symbol;
                    if (worldPos.equals(controller)) {
                        symbol = controllerSymbol;
                    } else {
                        BlockState state = level.getBlockState(worldPos);
                        if (state.isAir()) {
                            symbol = IGNORE_SYMBOL;
                        } else {
                            symbol = blockSymbols.get(exportTarget(state));
                        }
                    }
                    code.append('\'').append(symbol).append('\'');
                }
                code.append(")");
                if (localY < bounds.maxY()) {
                    code.append(',');
                }
                code.append('\n');
            }
            code.append("        )");
            if (localX < bounds.maxX()) {
                code.append('\n');
            }
        }
        code.append("\n))");
        return code.toString();
    }

    private static String buildFullVariant(
            String pattern,
            Map<ExportTarget, Character> blockSymbols,
            char controllerSymbol
    ) {
        StringBuilder code = new StringBuilder();
        code.append("public static final MultiblockControllerDefinition CONTROLLER = MultiblockControllerDefinition.machine()\n")
                .append("        .machineDefinition(MultiblockControllerDefinition.Option.id(\"test\"))\n")
                .append("        .machineDefinition(MultiblockControllerDefinition.Option.displayName(\"Test\"))\n")
                .append("        .build();\n\n")
                .append("public static final MultiblockDefinition DEFINITION = MultiblockDefinition.machine()\n")
                .append("        .machineDefinition(Option.id(\"test\"))\n")
                .append("        .machineDefinition(Option.controller(CONTROLLER))\n")
                .append("        .machineDefinition(Option.displayName(\"Test\"))\n")
                .append("        ")
                .append(pattern.replace("\n", "\n        "));
        for (Map.Entry<ExportTarget, Character> entry : blockSymbols.entrySet()) {
            code.append("\n        .machineDefinition(Option.where('")
                    .append(entry.getValue())
                    .append("', ")
                    .append(entry.getKey().predicateCode())
                    .append("))");
        }
        code.append("\n        .machineDefinition(Option.where('")
                .append(controllerSymbol)
                .append("', controller()))")
                .append("\n        .build();");
        return code.toString();
    }


    private static ExportTarget exportTarget(BlockState state) {
        if (state.getBlock() instanceof MachinePortBlock port) {
            MultiblockAbility ability = primaryExportAbility(port);
            if (ability != null) {
                return ExportTarget.ability(ability);
            }
        }
        return ExportTarget.block(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static MultiblockAbility primaryExportAbility(MachinePortBlock port) {
        if (port.abilities().contains(MultiblockAbility.IO_INTERFACE)) {
            return MultiblockAbility.IO_INTERFACE;
        }
        for (MultiblockAbility ability : MultiblockAbility.values()) {
            if (port.abilities().contains(ability)) {
                return ability;
            }
        }
        return null;
    }

    private static LocalBounds localBounds(
            BlockPos pos1,
            BlockPos pos2,
            BlockPos controller,
            Direction right,
            Direction forward
    ) {
        int minWorldX = Math.min(pos1.getX(), pos2.getX());
        int maxWorldX = Math.max(pos1.getX(), pos2.getX());
        int minWorldY = Math.min(pos1.getY(), pos2.getY());
        int maxWorldY = Math.max(pos1.getY(), pos2.getY());
        int minWorldZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxWorldZ = Math.max(pos1.getZ(), pos2.getZ());

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = minWorldY - controller.getY();
        int maxY = maxWorldY - controller.getY();
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int worldX : new int[]{minWorldX, maxWorldX}) {
            for (int worldZ : new int[]{minWorldZ, maxWorldZ}) {
                int dx = worldX - controller.getX();
                int dz = worldZ - controller.getZ();
                int localX = dx * right.getStepX() + dz * right.getStepZ();
                int localZ = dx * forward.getStepX() + dz * forward.getStepZ();
                minX = Math.min(minX, localX);
                maxX = Math.max(maxX, localX);
                minZ = Math.min(minZ, localZ);
                maxZ = Math.max(maxZ, localZ);
            }
        }
        return new LocalBounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private static BlockPos worldPos(
            BlockPos controller,
            Direction right,
            Direction forward,
            int localX,
            int localY,
            int localZ
    ) {
        return controller.relative(right, localX).above(localY).relative(forward, localZ);
    }

    private static boolean insideSelection(BlockPos pos, BlockPos pos1, BlockPos pos2) {
        return pos.getX() >= Math.min(pos1.getX(), pos2.getX())
                && pos.getX() <= Math.max(pos1.getX(), pos2.getX())
                && pos.getY() >= Math.min(pos1.getY(), pos2.getY())
                && pos.getY() <= Math.max(pos1.getY(), pos2.getY())
                && pos.getZ() >= Math.min(pos1.getZ(), pos2.getZ())
                && pos.getZ() <= Math.max(pos1.getZ(), pos2.getZ());
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public record Selection(BlockPos pos1, BlockPos pos2, BlockPos controller, Direction controllerFacing) {
        private static Selection empty() {
            return new Selection(null, null, null, Direction.NORTH);
        }

        public boolean complete() {
            return pos1 != null && pos2 != null && controller != null;
        }
    }


    private record ExportTarget(ResourceLocation blockId, MultiblockAbility ability) {
        private static ExportTarget block(ResourceLocation blockId) {
            return new ExportTarget(blockId, null);
        }

        private static ExportTarget ability(MultiblockAbility ability) {
            return new ExportTarget(null, ability);
        }

        private String predicateCode() {
            if (ability != null) {
                return "ability(MultiblockAbility." + ability.name() + ")";
            }
            return "block(\"" + blockId + "\")";
        }
    }

    private record LocalBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    }

    private record ExportResult(
            String pattern,
            String fullVariant,
            int width,
            int height,
            int length,
            String error
    ) {
        private static ExportResult error(String error) {
            return new ExportResult("", "", 0, 0, 0, error);
        }
    }
}
