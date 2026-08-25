package net.mads.industron.menu;

import net.mads.industron.machine.control.MachineControlContext;
import net.mads.industron.machine.control.MachineControlSchedule;
import net.mads.industron.machine.control.MachineControlScheduleHost;
import net.mads.industron.machine.control.MachineControlScheduleItem;
import net.mads.industron.machine.control.MachineControlTarget;
import net.mads.industron.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.jetbrains.annotations.Nullable;

public class MachineControlScheduleMenu extends AbstractContainerMenu {
    private static final int ACTION_SHIFT = 26;
    private static final int ACTION_ADD_NODE = 1;
    private static final int ACTION_ADD_VARIABLE_REPORTER = 2;
    private static final int ACTION_MOVE_NODE = 3;
    private static final int ACTION_REMOVE_NODE = 4;
    private static final int ACTION_CONNECT = 5;
    private static final int ACTION_CONNECT_VARIABLE = 6;
    private static final int ACTION_DISCONNECT_ALL = 7;
    private static final int ACTION_SET_VALUE = 8;
    private static final int ACTION_SET_OPERATION = 9;
    private static final int ACTION_SET_NODE_VARIABLE = 10;
    private static final int ACTION_BEGIN_VARIABLE = 11;
    private static final int ACTION_APPEND_VARIABLE_CHAR = 12;
    private static final int ACTION_BACKSPACE_VARIABLE = 13;
    private static final int ACTION_FINISH_VARIABLE = 14;
    private static final int ACTION_DELETE_VARIABLE = 15;
    private static final int ACTION_SET_VARIABLE_VALUE = 16;
    private static final int ACTION_BEGIN_NODE_TEXT = 17;
    private static final int ACTION_APPEND_NODE_TEXT_CHAR = 18;
    private static final int ACTION_BACKSPACE_NODE_TEXT = 19;
    private static final int ACTION_FINISH_NODE_TEXT = 20;
    private static final int ACTION_DISCONNECT_INPUT = 21;
    private static final int ACTION_SET_MODE = 22;
    private static final int ACTION_BEGIN_RENAME_VARIABLE = 23;
    private static final int ACTION_FINISH_RENAME_VARIABLE = 24;
    private static final int ACTION_BEGIN_NODE_VALUE = 25;
    private static final int ACTION_BEGIN_VARIABLE_VALUE = 26;
    private static final int ACTION_APPEND_NUMBER_CHAR = 27;
    private static final int ACTION_FINISH_NUMBER_VALUE = 28;

    private static final int DATA_REDSTONE_INPUT = 0;
    private static final int DATA_MACHINE_ENABLED = 1;
    private static final int DATA_RESULT = 2;
    private static final int DATA_REDSTONE_OUTPUT = 3;
    private static final int DATA_BOOLEAN_BASE = 4;
    private static final int DATA_NUMBER_BASE = DATA_BOOLEAN_BASE + MachineControlSchedule.MAX_NODES;
    private static final int DATA_VARIABLE_BASE = DATA_NUMBER_BASE + MachineControlSchedule.MAX_NODES;
    private static final int DATA_COUNT = DATA_VARIABLE_BASE + MachineControlSchedule.MAX_VARIABLES;

    private final BlockPos hostPos;
    private final Direction side;
    private final int itemSlot;
    @Nullable private final BlockEntity blockEntity;
    @Nullable private final MachineControlScheduleHost host;
    private final ContainerData data;
    private final StringBuilder variableNameBuilder = new StringBuilder();
    private final StringBuilder nodeTextBuilder = new StringBuilder();
    private final StringBuilder numberValueBuilder = new StringBuilder();
    private MachineControlSchedule clientSchedule;
    private int editingNodeTextId = -1;
    private int editingVariableId = -1;
    private int editingNumberId = -1;
    private boolean editingVariableNumber;

    public MachineControlScheduleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, readClientContext(inventory, buffer));
    }

    private MachineControlScheduleMenu(int containerId, Inventory inventory, ClientContext context) {
        this(containerId, inventory, context.pos, context.side, context.itemSlot, context.blockEntity,
                context.host, new SimpleContainerData(DATA_COUNT), context.schedule);
    }

    private MachineControlScheduleMenu(int containerId, Inventory inventory, BlockEntity blockEntity,
                                       MachineControlScheduleHost host, Direction side) {
        this(containerId, inventory, blockEntity.getBlockPos(), side, -1, blockEntity, host,
                serverData(host, side), host.machineControlSchedule(side).copy());
    }

    private MachineControlScheduleMenu(int containerId, Inventory inventory, int itemSlot,
                                       MachineControlSchedule schedule) {
        this(containerId, inventory, BlockPos.ZERO, Direction.NORTH, itemSlot, null, null,
                new SimpleContainerData(DATA_COUNT), schedule.copy());
    }

    private MachineControlScheduleMenu(int containerId, Inventory inventory, BlockPos hostPos, Direction side,
                                       int itemSlot, @Nullable BlockEntity blockEntity,
                                       @Nullable MachineControlScheduleHost host, ContainerData data,
                                       MachineControlSchedule clientSchedule) {
        super(MenuRegistry.MACHINE_CONTROL_SCHEDULE.get(), containerId);
        this.hostPos = hostPos;
        this.side = side;
        this.itemSlot = itemSlot;
        this.blockEntity = blockEntity;
        this.host = host;
        this.data = data;
        this.clientSchedule = clientSchedule;
        addDataSlots(data);
    }

    public static void open(Player player, BlockEntity blockEntity, Direction side) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(blockEntity instanceof MachineControlScheduleHost host)
                || !host.hasMachineControlSchedule(side)) return;
        host.synchronizeMachineControlVariables();
        MachineControlSchedule schedule = host.machineControlSchedule(side);
        ((IPlayerExtension) serverPlayer).openMenu(new SimpleMenuProvider(
                        (id, inventory, menuPlayer) -> new MachineControlScheduleMenu(id, inventory, blockEntity, host, side),
                        Component.translatable("gui.industron.machine_control_schedule")),
                buffer -> {
                    buffer.writeByte(0);
                    buffer.writeBlockPos(blockEntity.getBlockPos());
                    buffer.writeByte(side.get3DDataValue());
                    buffer.writeNbt(schedule.save());
                });
    }

    public static void openItem(Player player, InteractionHand hand, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        MachineControlSchedule schedule = MachineControlScheduleItem.scheduleFromStack(stack);
        ((IPlayerExtension) serverPlayer).openMenu(new SimpleMenuProvider(
                        (id, inventory, menuPlayer) -> new MachineControlScheduleMenu(id, inventory, slot, schedule),
                        Component.translatable("gui.industron.machine_control_schedule")),
                buffer -> {
                    buffer.writeByte(1);
                    buffer.writeVarInt(slot);
                    buffer.writeNbt(schedule.save());
                });
    }

    private static ClientContext readClientContext(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        if (buffer == null) return new ClientContext(BlockPos.ZERO, Direction.NORTH, -1, null, null, new MachineControlSchedule());
        int mode = buffer.readUnsignedByte();
        if (mode == 1) {
            int slot = buffer.readVarInt();
            CompoundTag tag = buffer.readNbt();
            return new ClientContext(BlockPos.ZERO, Direction.NORTH, slot, null, null,
                    tag == null ? new MachineControlSchedule() : MachineControlSchedule.load(tag));
        }
        BlockPos pos = buffer.readBlockPos();
        Direction side = Direction.from3DDataValue(buffer.readUnsignedByte());
        CompoundTag tag = buffer.readNbt();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        MachineControlScheduleHost host = blockEntity instanceof MachineControlScheduleHost found ? found : null;
        return new ClientContext(pos, side, -1, blockEntity, host,
                tag == null ? new MachineControlSchedule() : MachineControlSchedule.load(tag));
    }

    private static ContainerData serverData(MachineControlScheduleHost host, Direction side) {
        return new ContainerData() {
            private long cachedTick = Long.MIN_VALUE;
            private int cachedRedstone;
            private boolean cachedMachineEnabled;
            private MachineControlSchedule.ProgramResult cachedResult = emptyResult();

            private void refresh() {
                long tick = host instanceof BlockEntity blockEntity && blockEntity.getLevel() != null
                        ? blockEntity.getLevel().getGameTime() : System.nanoTime();
                if (tick == cachedTick) return;
                cachedTick = tick;
                MachineControlSchedule schedule = host.machineControlSchedule(side);
                if (schedule == null) {
                    cachedResult = emptyResult();
                    cachedRedstone = 0;
                    cachedMachineEnabled = false;
                    return;
                }
                cachedRedstone = host.machineControlRedstoneInput(side);
                MachineControlTarget target = host.machineControlTarget();
                MachineControlContext context = target == null ? () -> cachedRedstone : target.machineControlContext(cachedRedstone);
                cachedResult = schedule.evaluateLive(context, host.machineControlVariables());
                cachedMachineEnabled = target != null && target.isMachineEnabled();
            }

            @Override
            public int get(int index) {
                refresh();
                if (index == DATA_REDSTONE_INPUT) return cachedRedstone;
                if (index == DATA_MACHINE_ENABLED) return cachedMachineEnabled ? 1 : 0;
                if (index == DATA_RESULT) return cachedResult.machineEnabled() ? 1 : 0;
                if (index == DATA_REDSTONE_OUTPUT) return cachedResult.redstoneOutput();
                if (index >= DATA_BOOLEAN_BASE && index < DATA_NUMBER_BASE) {
                    int nodeId = index - DATA_BOOLEAN_BASE;
                    return cachedResult.booleanValues().containsKey(nodeId)
                            ? cachedResult.booleanValues().get(nodeId) ? 1 : 0 : -1;
                }
                if (index >= DATA_NUMBER_BASE && index < DATA_COUNT) {
                    if (index >= DATA_VARIABLE_BASE) {
                        int variableId = index - DATA_VARIABLE_BASE;
                        return cachedResult.variables().getOrDefault(variableId, 0);
                    }
                    int nodeId = index - DATA_NUMBER_BASE;
                    return cachedResult.numberValues().getOrDefault(nodeId, -1);
                }
                return 0;
            }

            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }

    private static MachineControlSchedule.ProgramResult emptyResult() {
        return new MachineControlSchedule.ProgramResult(true, 0, java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
    }

    @Override
    public boolean clickMenuButton(Player player, int packed) {
        MachineControlSchedule schedule = serverSchedule(player);
        if (schedule == null) return false;
        int action = packed >>> ACTION_SHIFT;
        int payload = packed & 0x03FFFFFF;
        boolean changed = switch (action) {
            case ACTION_ADD_NODE -> addNode(schedule, payload);
            case ACTION_ADD_VARIABLE_REPORTER -> addVariableReporter(schedule, payload);
            case ACTION_MOVE_NODE -> moveNode(schedule, payload);
            case ACTION_REMOVE_NODE -> schedule.removeNode(payload & 0x7F);
            case ACTION_CONNECT -> connect(schedule, payload);
            case ACTION_CONNECT_VARIABLE -> connectVariable(schedule, payload);
            case ACTION_DISCONNECT_ALL -> schedule.disconnectAll(payload & 0x7F);
            case ACTION_SET_VALUE -> schedule.setNodeValue(payload >>> 8 & 0x7F, MachineControlSchedule.toScaled(payload & 0xFF));
            case ACTION_SET_OPERATION -> setOperation(schedule, payload);
            case ACTION_SET_NODE_VARIABLE -> schedule.setNodeVariable(payload >>> 6 & 0x7F, payload & 0x3F);
            case ACTION_BEGIN_VARIABLE -> beginVariableName(-1);
            case ACTION_APPEND_VARIABLE_CHAR -> appendVariableCharacter(payload & 0xFF);
            case ACTION_BACKSPACE_VARIABLE -> backspaceVariableName();
            case ACTION_FINISH_VARIABLE -> finishVariable(schedule);
            case ACTION_DELETE_VARIABLE -> host != null
                    ? host.deleteMachineControlVariable(payload & 0x3F)
                    : schedule.deleteVariable(payload & 0x3F);
            case ACTION_SET_VARIABLE_VALUE -> host != null
                    ? host.setMachineControlVariableValue(payload >>> 8 & 0x3F, MachineControlSchedule.toScaled(payload & 0xFF))
                    : schedule.setVariableValue(payload >>> 8 & 0x3F, MachineControlSchedule.toScaled(payload & 0xFF));
            case ACTION_BEGIN_NODE_TEXT -> beginNodeText(schedule, payload & 0x7F);
            case ACTION_APPEND_NODE_TEXT_CHAR -> appendNodeTextCharacter(payload & 0xFF);
            case ACTION_BACKSPACE_NODE_TEXT -> backspaceNodeText();
            case ACTION_FINISH_NODE_TEXT -> finishNodeText(schedule);
            case ACTION_DISCONNECT_INPUT -> disconnectInput(schedule, payload);
            case ACTION_SET_MODE -> setMode(schedule, payload);
            case ACTION_BEGIN_RENAME_VARIABLE -> beginVariableName(payload & 0x3F);
            case ACTION_FINISH_RENAME_VARIABLE -> finishRenameVariable(schedule);
            case ACTION_BEGIN_NODE_VALUE -> beginNumberValue(payload & 0x7F, false);
            case ACTION_BEGIN_VARIABLE_VALUE -> beginNumberValue(payload & 0x3F, true);
            case ACTION_APPEND_NUMBER_CHAR -> appendNumberCharacter(payload & 0xFF);
            case ACTION_FINISH_NUMBER_VALUE -> finishNumberValue(schedule);
            default -> false;
        };
        if (changed) saveServerSchedule(player, schedule);
        return changed;
    }

    private MachineControlSchedule serverSchedule(Player player) {
        if (host != null) return host.machineControlSchedule(side);
        ItemStack stack = stackAt(player, itemSlot);
        return stack.isEmpty() ? null : MachineControlScheduleItem.scheduleFromStack(stack);
    }

    private void saveServerSchedule(Player player, MachineControlSchedule schedule) {
        if (host != null) {
            host.machineControlSchedules().put(side, schedule);
            host.machineControlSchedulesChanged();
        } else {
            ItemStack stack = stackAt(player, itemSlot);
            if (!stack.isEmpty()) MachineControlScheduleItem.setScheduleOnStack(stack, schedule);
        }
    }

    private static ItemStack stackAt(Player player, int slot) {
        if (slot == 40) return player.getOffhandItem();
        return slot >= 0 && slot < player.getInventory().getContainerSize() ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }

    private boolean addNode(MachineControlSchedule schedule, int payload) {
        int type = payload >>> 20 & 0x3F;
        int x = decodeSigned(payload >>> 10 & 0x3FF, 10) * 4;
        int y = decodeSigned(payload & 0x3FF, 10) * 4;
        MachineControlSchedule.NodeType[] values = MachineControlSchedule.NodeType.values();
        return type < values.length && schedule.addNode(values[type], x, y) != null;
    }

    private boolean addVariableReporter(MachineControlSchedule schedule, int payload) {
        int variableId = payload >>> 20 & 0x3F;
        int x = decodeSigned(payload >>> 10 & 0x3FF, 10) * 4;
        int y = decodeSigned(payload & 0x3FF, 10) * 4;
        return schedule.addVariableReporter(variableId, x, y) != null;
    }

    private boolean moveNode(MachineControlSchedule schedule, int payload) {
        int nodeId = payload >>> 19 & 0x7F;
        int x = decodeSigned(payload >>> 9 & 0x3FF, 10) * 4;
        int y = decodeSigned(payload & 0x1FF, 9) * 4;
        return schedule.moveNode(nodeId, x, y);
    }

    private boolean connect(MachineControlSchedule schedule, int payload) {
        int target = payload >>> 19 & 0x7F;
        int source = payload >>> 12 & 0x7F;
        MachineControlSchedule.PortKind kind = (payload >>> 11 & 1) == 0
                ? MachineControlSchedule.PortKind.BOOLEAN : MachineControlSchedule.PortKind.NUMBER;
        int slot = payload >>> 7 & 0xF;
        return schedule.connect(target, source, kind, slot);
    }

    private boolean connectVariable(MachineControlSchedule schedule, int payload) {
        return schedule.connectVariable(payload >>> 6 & 0x7F, payload & 0x3F);
    }

    private boolean disconnectInput(MachineControlSchedule schedule, int payload) {
        int target = payload >>> 5 & 0x7F;
        MachineControlSchedule.PortKind kind = (payload >>> 4 & 1) == 0
                ? MachineControlSchedule.PortKind.BOOLEAN : MachineControlSchedule.PortKind.NUMBER;
        return schedule.disconnectInput(target, kind, payload & 0xF);
    }

    private boolean setOperation(MachineControlSchedule schedule, int payload) {
        int nodeId = payload >>> 6 & 0x7F;
        int operation = payload & 0x3F;
        MachineControlSchedule.Operation[] values = MachineControlSchedule.Operation.values();
        return operation < values.length && schedule.setNodeOperation(nodeId, values[operation]);
    }

    private boolean setMode(MachineControlSchedule schedule, int payload) {
        int nodeId = payload >>> 4 & 0x7F;
        int mode = payload & 0xF;
        MachineControlSchedule.NodeMode[] values = MachineControlSchedule.NodeMode.values();
        return mode < values.length && schedule.setNodeMode(nodeId, values[mode]);
    }

    private boolean beginVariableName(int variableId) {
        variableNameBuilder.setLength(0);
        editingVariableId = variableId;
        return true;
    }

    private boolean appendVariableCharacter(int character) {
        if (variableNameBuilder.length() >= 24 || character < 32 || character > 126) return false;
        variableNameBuilder.append((char) character);
        return true;
    }

    private boolean backspaceVariableName() {
        if (variableNameBuilder.isEmpty()) return false;
        variableNameBuilder.deleteCharAt(variableNameBuilder.length() - 1);
        return true;
    }

    private boolean finishVariable(MachineControlSchedule schedule) {
        boolean result = host != null
                ? host.addMachineControlVariable(variableNameBuilder.toString()) != null
                : schedule.addVariable(variableNameBuilder.toString()) != null;
        variableNameBuilder.setLength(0);
        editingVariableId = -1;
        return result;
    }

    private boolean finishRenameVariable(MachineControlSchedule schedule) {
        boolean result = editingVariableId >= 0 && (host != null
                ? host.renameMachineControlVariable(editingVariableId, variableNameBuilder.toString())
                : schedule.renameVariable(editingVariableId, variableNameBuilder.toString()));
        variableNameBuilder.setLength(0);
        editingVariableId = -1;
        return result;
    }

    private boolean beginNodeText(MachineControlSchedule schedule, int nodeId) {
        MachineControlSchedule.Node node = schedule.node(nodeId);
        if (node == null || !node.type().hasTextValue()) return false;
        editingNodeTextId = nodeId;
        nodeTextBuilder.setLength(0);
        return true;
    }

    private boolean appendNodeTextCharacter(int character) {
        if (editingNodeTextId < 0 || nodeTextBuilder.length() >= 256 || character < 10 || character > 126) return false;
        nodeTextBuilder.append((char) character);
        return true;
    }

    private boolean backspaceNodeText() {
        if (editingNodeTextId < 0 || nodeTextBuilder.isEmpty()) return false;
        nodeTextBuilder.deleteCharAt(nodeTextBuilder.length() - 1);
        return true;
    }

    private boolean finishNodeText(MachineControlSchedule schedule) {
        if (editingNodeTextId < 0) return false;
        boolean result = schedule.setNodeText(editingNodeTextId, nodeTextBuilder.toString());
        editingNodeTextId = -1;
        nodeTextBuilder.setLength(0);
        return result;
    }

    private boolean beginNumberValue(int id, boolean variable) {
        numberValueBuilder.setLength(0);
        editingNumberId = id;
        editingVariableNumber = variable;
        return true;
    }

    private boolean appendNumberCharacter(int character) {
        if (numberValueBuilder.length() >= 12 || (character < '0' || character > '9') && character != '.') return false;
        if (character == '.' && numberValueBuilder.indexOf(".") >= 0) return false;
        numberValueBuilder.append((char) character);
        return true;
    }

    private boolean finishNumberValue(MachineControlSchedule schedule) {
        if (editingNumberId < 0 || numberValueBuilder.isEmpty()) return false;
        try {
            int value = MachineControlSchedule.parseScaledNumber(numberValueBuilder.toString());
            return editingVariableNumber
                    ? (host != null
                    ? host.setMachineControlVariableValue(editingNumberId, value)
                    : schedule.setVariableValue(editingNumberId, value))
                    : schedule.setNodeValue(editingNumberId, value);
        } catch (NumberFormatException ignored) {
            return false;
        } finally {
            numberValueBuilder.setLength(0);
            editingNumberId = -1;
            editingVariableNumber = false;
        }
    }

    public void applyClientAction(int packed) {
        int action = packed >>> ACTION_SHIFT;
        int payload = packed & 0x03FFFFFF;
        switch (action) {
            case ACTION_ADD_NODE -> addNode(clientSchedule, payload);
            case ACTION_ADD_VARIABLE_REPORTER -> addVariableReporter(clientSchedule, payload);
            case ACTION_MOVE_NODE -> moveNode(clientSchedule, payload);
            case ACTION_REMOVE_NODE -> clientSchedule.removeNode(payload & 0x7F);
            case ACTION_CONNECT -> connect(clientSchedule, payload);
            case ACTION_CONNECT_VARIABLE -> connectVariable(clientSchedule, payload);
            case ACTION_DISCONNECT_ALL -> clientSchedule.disconnectAll(payload & 0x7F);
            case ACTION_SET_VALUE -> clientSchedule.setNodeValue(payload >>> 8 & 0x7F, MachineControlSchedule.toScaled(payload & 0xFF));
            case ACTION_SET_OPERATION -> setOperation(clientSchedule, payload);
            case ACTION_SET_NODE_VARIABLE -> clientSchedule.setNodeVariable(payload >>> 6 & 0x7F, payload & 0x3F);
            case ACTION_DELETE_VARIABLE -> clientSchedule.deleteVariable(payload & 0x3F);
            case ACTION_SET_VARIABLE_VALUE -> clientSchedule.setVariableValue(payload >>> 8 & 0x3F, MachineControlSchedule.toScaled(payload & 0xFF));
            case ACTION_DISCONNECT_INPUT -> disconnectInput(clientSchedule, payload);
            case ACTION_SET_MODE -> setMode(clientSchedule, payload);
        }
    }

    public static int addNodeAction(MachineControlSchedule.NodeType type, int x, int y) {
        return ACTION_ADD_NODE << ACTION_SHIFT | (type.ordinal() & 0x3F) << 20
                | encodeSigned(x / 4, 10) << 10 | encodeSigned(y / 4, 10);
    }

    public static int addVariableReporterAction(int variableId, int x, int y) {
        return ACTION_ADD_VARIABLE_REPORTER << ACTION_SHIFT | (variableId & 0x3F) << 20
                | encodeSigned(x / 4, 10) << 10 | encodeSigned(y / 4, 10);
    }

    public static int moveNodeAction(int nodeId, int x, int y) {
        return ACTION_MOVE_NODE << ACTION_SHIFT | (nodeId & 0x7F) << 19
                | encodeSigned(x / 4, 10) << 9 | encodeSigned(y / 4, 9);
    }

    public static int removeNodeAction(int id) { return ACTION_REMOVE_NODE << ACTION_SHIFT | id & 0x7F; }

    public static int connectAction(int target, int source, MachineControlSchedule.PortKind kind, int slot) {
        return ACTION_CONNECT << ACTION_SHIFT | (target & 0x7F) << 19 | (source & 0x7F) << 12
                | (kind == MachineControlSchedule.PortKind.NUMBER ? 1 : 0) << 11 | (slot & 0xF) << 7;
    }

    public static int connectVariableAction(int target, int variable) {
        return ACTION_CONNECT_VARIABLE << ACTION_SHIFT | (target & 0x7F) << 6 | variable & 0x3F;
    }

    public static int disconnectAllAction(int id) { return ACTION_DISCONNECT_ALL << ACTION_SHIFT | id & 0x7F; }

    public static int disconnectInputAction(int target, MachineControlSchedule.PortKind kind, int slot) {
        return ACTION_DISCONNECT_INPUT << ACTION_SHIFT | (target & 0x7F) << 5
                | (kind == MachineControlSchedule.PortKind.NUMBER ? 1 : 0) << 4 | slot & 0xF;
    }

    public static int setValueAction(int id, int value) {
        return ACTION_SET_VALUE << ACTION_SHIFT | (id & 0x7F) << 8 | value & 0xFF;
    }

    public static int setOperationAction(int id, MachineControlSchedule.Operation operation) {
        return ACTION_SET_OPERATION << ACTION_SHIFT | (id & 0x7F) << 6 | operation.ordinal() & 0x3F;
    }

    public static int setModeAction(int id, MachineControlSchedule.NodeMode mode) {
        return ACTION_SET_MODE << ACTION_SHIFT | (id & 0x7F) << 4 | mode.ordinal() & 0xF;
    }

    public static int setNodeVariableAction(int id, int variable) {
        return ACTION_SET_NODE_VARIABLE << ACTION_SHIFT | (id & 0x7F) << 6 | variable & 0x3F;
    }

    public static int beginVariableAction() { return ACTION_BEGIN_VARIABLE << ACTION_SHIFT; }
    public static int beginRenameVariableAction(int id) { return ACTION_BEGIN_RENAME_VARIABLE << ACTION_SHIFT | id & 0x3F; }
    public static int appendVariableCharAction(char c) { return ACTION_APPEND_VARIABLE_CHAR << ACTION_SHIFT | c & 0xFF; }
    public static int backspaceVariableAction() { return ACTION_BACKSPACE_VARIABLE << ACTION_SHIFT; }
    public static int finishVariableAction() { return ACTION_FINISH_VARIABLE << ACTION_SHIFT; }
    public static int finishRenameVariableAction() { return ACTION_FINISH_RENAME_VARIABLE << ACTION_SHIFT; }
    public static int beginNodeValueAction(int id) { return ACTION_BEGIN_NODE_VALUE << ACTION_SHIFT | id & 0x7F; }
    public static int beginVariableValueAction(int id) { return ACTION_BEGIN_VARIABLE_VALUE << ACTION_SHIFT | id & 0x3F; }
    public static int appendNumberCharAction(char c) { return ACTION_APPEND_NUMBER_CHAR << ACTION_SHIFT | c & 0xFF; }
    public static int finishNumberValueAction() { return ACTION_FINISH_NUMBER_VALUE << ACTION_SHIFT; }
    public static int deleteVariableAction(int id) { return ACTION_DELETE_VARIABLE << ACTION_SHIFT | id & 0x3F; }
    public static int setVariableValueAction(int id, int value) {
        return ACTION_SET_VARIABLE_VALUE << ACTION_SHIFT | (id & 0x3F) << 8 | value & 0xFF;
    }
    public static int beginNodeTextAction(int id) { return ACTION_BEGIN_NODE_TEXT << ACTION_SHIFT | id & 0x7F; }
    public static int appendNodeTextCharAction(char c) { return ACTION_APPEND_NODE_TEXT_CHAR << ACTION_SHIFT | c & 0xFF; }
    public static int backspaceNodeTextAction() { return ACTION_BACKSPACE_NODE_TEXT << ACTION_SHIFT; }
    public static int finishNodeTextAction() { return ACTION_FINISH_NODE_TEXT << ACTION_SHIFT; }

    private static int encodeSigned(int value, int bits) {
        int max = (1 << (bits - 1)) - 1;
        int min = -(1 << (bits - 1));
        return Math.max(min, Math.min(max, value)) & (1 << bits) - 1;
    }

    private static int decodeSigned(int value, int bits) {
        int sign = 1 << bits - 1;
        return (value & sign) == 0 ? value : value - (1 << bits);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        if (host == null) return !stackAt(player, itemSlot).isEmpty();
        return blockEntity != null && player.level() == blockEntity.getLevel()
                && player.distanceToSqr(hostPos.getX() + .5, hostPos.getY() + .5, hostPos.getZ() + .5) <= 64
                && host.hasMachineControlSchedule(side);
    }

    public MachineControlSchedule clientSchedule() { return clientSchedule; }
    public Direction side() { return side; }
    public boolean itemMode() { return itemSlot >= 0; }
    public int redstoneInput() { return data.get(DATA_REDSTONE_INPUT); }
    public boolean machineEnabled() { return data.get(DATA_MACHINE_ENABLED) == 1; }
    public boolean result() { return data.get(DATA_RESULT) == 1; }
    public int redstoneOutput() { return data.get(DATA_REDSTONE_OUTPUT); }
    public boolean hasBooleanValue(int nodeId) { return !itemMode() && nodeId >= 0 && nodeId < MachineControlSchedule.MAX_NODES && data.get(DATA_BOOLEAN_BASE + nodeId) >= 0; }
    public boolean booleanValue(int nodeId) { return hasBooleanValue(nodeId) && data.get(DATA_BOOLEAN_BASE + nodeId) == 1; }
    public boolean hasNumberValue(int nodeId) { return !itemMode() && nodeId >= 0 && nodeId < MachineControlSchedule.MAX_NODES && data.get(DATA_NUMBER_BASE + nodeId) >= 0; }
    public int numberValue(int nodeId) { return hasNumberValue(nodeId) ? data.get(DATA_NUMBER_BASE + nodeId) : 0; }
    public int variableValue(int variableId) {
        if (variableId < 0 || variableId >= MachineControlSchedule.MAX_VARIABLES) return 0;
        return itemMode()
                ? (clientSchedule.variable(variableId) == null ? 0 : clientSchedule.variable(variableId).value())
                : data.get(DATA_VARIABLE_BASE + variableId);
    }

    private record ClientContext(BlockPos pos, Direction side, int itemSlot, @Nullable BlockEntity blockEntity,
                                 @Nullable MachineControlScheduleHost host, MachineControlSchedule schedule) { }
}
