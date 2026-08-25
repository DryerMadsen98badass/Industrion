package net.mads.industron.machine.control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MachineControlSchedule {
    public static final int MAX_NODES = 96;
    public static final int MAX_VARIABLES = 32;
    public static final int NUMBER_SCALE = 100;
    public static final int MAX_NUMBER_VALUE = Integer.MAX_VALUE;
    public static final int WORKSPACE_MIN = -4096;
    public static final int WORKSPACE_MAX = 4096;

    private final List<Node> nodes = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();
    private final Node[] nodesById = new Node[MAX_NODES];
    private final Variable[] variablesById = new Variable[MAX_VARIABLES];
    private int nextNodeId;
    private int nextVariableId;
    private boolean runtimeDirty;
    private long programRevision = 1L;
    private CompiledProgram compiledProgram;
    private long cachedRuntimeKey = Long.MIN_VALUE;
    private long cachedLiveKey = Long.MIN_VALUE;
    private ProgramResult cachedRuntimeResult;
    private ProgramResult cachedLiveResult;
    private final EvaluationScratch runtimeScratch = new EvaluationScratch();
    private final EvaluationScratch liveScratch = new EvaluationScratch();

    public MachineControlSchedule() {
        Node input = addNode(NodeType.REDSTONE_INPUT, 20, 30);
        Node not = addNode(NodeType.NOT, 120, 30);
        Node enabled = addNode(NodeType.MACHINE_ENABLED, 220, 30);
        if (input != null && not != null && enabled != null) {
            connect(not.id(), input.id(), PortKind.BOOLEAN, 0);
            connect(enabled.id(), not.id(), PortKind.BOOLEAN, 0);
        }
    }

    private MachineControlSchedule(boolean empty) {
    }

    public List<Node> nodes() { return List.copyOf(nodes); }
    public List<Variable> variables() { return List.copyOf(variables); }
    public Node node(int id) { return id >= 0 && id < MAX_NODES ? nodesById[id] : null; }
    public Variable variable(int id) { return id >= 0 && id < MAX_VARIABLES ? variablesById[id] : null; }

    public Variable addVariable(String requestedName) {
        if (variables.size() >= MAX_VARIABLES) return null;
        int id = nextFreeVariableId();
        if (id < 0) return null;
        Variable variable = new Variable(id, uniqueVariableName(cleanName(requestedName), -1), 0);
        variables.add(variable);
        variablesById[id] = variable;
        nextVariableId = (id + 1) % MAX_VARIABLES;
        invalidateRuntimeCaches();
        return variable;
    }

    public boolean renameVariable(int id, String requestedName) {
        Variable variable = variable(id);
        if (variable == null) return false;
        variable.name = uniqueVariableName(cleanName(requestedName), id);
        return true;
    }

    public boolean deleteVariable(int id) {
        Variable variable = variable(id);
        if (variable == null) return false;
        variables.remove(variable);
        variablesById[id] = null;
        List<Integer> removedNodeIds = new ArrayList<>();
        nodes.removeIf(node -> {
            if (node.variableId != id) return false;
            removedNodeIds.add(node.id);
            nodesById[node.id] = null;
            return true;
        });
        for (Node node : nodes) {
            node.inputs.removeIf(connection -> connection.sourceVariableId == id || removedNodeIds.contains(connection.sourceNodeId));
        }
        markProgramChanged();
        return true;
    }

    public boolean setVariableValue(int id, int value) {
        Variable variable = variable(id);
        if (variable == null) return false;
        int clamped = clampNumber(value);
        if (variable.value == clamped) return false;
        variable.value = clamped;
        invalidateRuntimeCaches();
        return true;
    }

    /** Remaps every node and direct variable connection to host-shared variable IDs. */
    void remapVariables(Map<Integer, Integer> remap) {
        if (remap == null || remap.isEmpty()) return;
        for (Node node : nodes) {
            if (node.variableId >= 0) node.variableId = remap.getOrDefault(node.variableId, node.variableId);
            for (int i = 0; i < node.inputs.size(); i++) {
                Connection connection = node.inputs.get(i);
                if (connection.sourceVariableId < 0) continue;
                int mapped = remap.getOrDefault(connection.sourceVariableId, connection.sourceVariableId);
                if (mapped != connection.sourceVariableId) {
                    node.inputs.set(i, new Connection(connection.sourceNodeId, mapped, connection.kind, connection.targetSlot));
                }
            }
        }
        markProgramChanged();
    }

    /** Replaces local editor definitions with the authoritative host variable namespace. */
    void replaceVariables(List<MachineControlVariableStore.Entry> sharedVariables) {
        variables.clear();
        java.util.Arrays.fill(variablesById, null);
        int highest = -1;
        for (MachineControlVariableStore.Entry shared : sharedVariables) {
            if (shared.id() < 0 || shared.id() >= MAX_VARIABLES || variablesById[shared.id()] != null) continue;
            Variable variable = new Variable(shared.id(), shared.name(), shared.value());
            variables.add(variable);
            variablesById[variable.id] = variable;
            highest = Math.max(highest, variable.id);
        }
        nextVariableId = Math.floorMod(highest + 1, MAX_VARIABLES);
        invalidateRuntimeCaches();
    }

    public Node addNode(NodeType type, int x, int y) {
        if (type == null || nodes.size() >= MAX_NODES) return null;
        int id = nextFreeNodeId();
        if (id < 0) return null;
        Node node = new Node(id, type, clampWorkspace(x), clampWorkspace(y));
        nodes.add(node);
        nodesById[id] = node;
        nextNodeId = (id + 1) % MAX_NODES;
        markProgramChanged();
        return node;
    }

    public Node addVariableReporter(int variableId, int x, int y) {
        if (variable(variableId) == null) return null;
        Node node = addNode(NodeType.VARIABLE_REPORTER, x, y);
        if (node != null) {
            node.variableId = variableId;
            invalidateRuntimeCaches();
        }
        return node;
    }

    public boolean moveNode(int id, int x, int y) {
        Node node = node(id);
        if (node == null) return false;
        node.x = clampWorkspace(x);
        node.y = clampWorkspace(y);
        return true;
    }

    public boolean removeNode(int id) {
        Node removed = node(id);
        if (removed == null) return false;
        nodes.remove(removed);
        nodesById[id] = null;
        for (Node node : nodes) node.inputs.removeIf(connection -> connection.sourceNodeId == id);
        markProgramChanged();
        return true;
    }

    public boolean setNodeValue(int id, int value) {
        Node node = node(id);
        if (node == null) return false;
        int clamped = node.type == NodeType.REDSTONE_OUTPUT
                ? Math.max(0, Math.min(toScaled(15), value))
                : node.type == NodeType.NUMBER ? clampSignedNumber(value) : clampNumber(value);
        if (node.value == clamped) return false;
        node.value = clamped;
        invalidateRuntimeCaches();
        return true;
    }

    public boolean setNodeOperation(int id, Operation operation) {
        Node node = node(id);
        if (node == null || operation == null || !node.type.supportsOperation(operation) || node.operation == operation) return false;
        node.operation = operation;
        invalidateRuntimeCaches();
        return true;
    }

    public boolean setNodeMode(int id, NodeMode mode) {
        Node node = node(id);
        if (node == null || mode == null || !node.type.supportsMode(mode) || node.mode == mode) return false;
        node.mode = mode;
        invalidateRuntimeCaches();
        return true;
    }

    public boolean setNodeVariable(int id, int variableId) {
        Node node = node(id);
        if (node == null || variable(variableId) == null || node.type != NodeType.SET_VARIABLE || node.variableId == variableId) return false;
        node.variableId = variableId;
        invalidateRuntimeCaches();
        return true;
    }

    public boolean setNodeText(int id, String text) {
        Node node = node(id);
        if (node == null || !node.type.hasTextValue()) return false;
        String cleaned = cleanFilterText(text);
        if (node.textValue.equals(cleaned)) return false;
        node.textValue = cleaned;
        invalidateRuntimeCaches();
        return true;
    }

    public boolean connect(int targetId, int sourceId, PortKind kind) {
        Node target = node(targetId);
        if (target == null) return false;
        int slot = target.firstAvailableSlot(kind);
        return slot >= 0 && connect(targetId, sourceId, kind, slot);
    }

    public boolean connect(int targetId, int sourceId, PortKind kind, int targetSlot) {
        Node target = node(targetId);
        Node source = node(sourceId);
        if (target == null || source == null || targetId == sourceId || kind == null
                || !target.accepts(kind) || !source.provides(kind) || !target.validSlot(kind, targetSlot)) return false;
        Connection connection = new Connection(sourceId, -1, kind, targetSlot);
        Connection previous = target.connectionAt(kind, targetSlot);
        if (connection.equals(previous)) return false;
        if (previous != null) target.inputs.remove(previous);
        target.inputs.add(connection);
        if (hasCycle(targetId, new HashSet<>(), new HashSet<>())) {
            target.inputs.remove(connection);
            if (previous != null) target.inputs.add(previous);
            return false;
        }
        markProgramChanged();
        return true;
    }

    public boolean connectVariable(int targetId, int variableId) {
        Node target = node(targetId);
        if (target == null || variable(variableId) == null) return false;
        int slot = target.firstAvailableSlot(PortKind.NUMBER);
        if (slot < 0) return false;
        Connection previous = target.connectionAt(PortKind.NUMBER, slot);
        if (previous != null) target.inputs.remove(previous);
        target.inputs.add(new Connection(-1, variableId, PortKind.NUMBER, slot));
        markProgramChanged();
        return true;
    }

    public boolean disconnectAll(int nodeId) {
        Node node = node(nodeId);
        if (node == null) return false;
        boolean changed = !node.inputs.isEmpty();
        node.inputs.clear();
        for (Node other : nodes) changed |= other.inputs.removeIf(connection -> connection.sourceNodeId == nodeId);
        if (changed) markProgramChanged();
        return changed;
    }

    public boolean disconnectInput(int targetId, PortKind kind, int targetSlot) {
        Node target = node(targetId);
        boolean changed = target != null && target.inputs.removeIf(connection -> connection.kind == kind && connection.targetSlot == targetSlot);
        if (changed) markProgramChanged();
        return changed;
    }

    public List<OutgoingConnection> outgoingConnections(int sourceNodeId) {
        List<OutgoingConnection> result = new ArrayList<>();
        for (Node target : nodes) {
            for (Connection connection : target.inputs) {
                if (connection.sourceNodeId == sourceNodeId) result.add(new OutgoingConnection(target.id, connection.kind, connection.targetSlot));
            }
        }
        return List.copyOf(result);
    }

    /** Runtime evaluation only visits branches that can affect a machine action. */
    public ProgramResult evaluate(MachineControlContext context) {
        return evaluateInternal(context, null, false);
    }

    public ProgramResult evaluate(MachineControlContext context, MachineControlVariableStore variables) {
        return evaluateInternal(context, variables, false);
    }

    /** Full evaluation used by the open editor to populate live node values. */
    public ProgramResult evaluateLive(MachineControlContext context) {
        return evaluateInternal(context, null, true);
    }

    public ProgramResult evaluateLive(MachineControlContext context, MachineControlVariableStore variables) {
        return evaluateInternal(context, variables, true);
    }

    private ProgramResult evaluateInternal(MachineControlContext suppliedContext, MachineControlVariableStore sharedVariables,
                                           boolean captureLiveValues) {
        MachineControlContext context = suppliedContext == null ? () -> 0 : suppliedContext;
        CompiledProgram program = compiledProgram();
        boolean dependsOnTick = captureLiveValues ? program.liveDependsOnTick : program.runtimeDependsOnTick;
        boolean usesInputs = captureLiveValues ? program.liveUsesInputs : program.runtimeUsesInputs;
        boolean usesRedstone = captureLiveValues ? program.liveUsesRedstone : program.runtimeUsesRedstone;
        boolean usesWorld = captureLiveValues ? program.liveUsesWorld : program.runtimeUsesWorld;
        long key = evaluationKey(context, sharedVariables, dependsOnTick, usesInputs, usesRedstone, usesWorld);
        if (key != Long.MIN_VALUE) {
            ProgramResult cached = captureLiveValues ? cachedLiveResult : cachedRuntimeResult;
            long cachedKey = captureLiveValues ? cachedLiveKey : cachedRuntimeKey;
            if (cached != null && cachedKey == key) return cached;
        }

        EvaluationScratch scratch = captureLiveValues ? liveScratch : runtimeScratch;
        scratch.reset();
        Evaluation evaluation = new Evaluation(context, sharedVariables, program, captureLiveValues, scratch);
        ProgramResult result = evaluation.run();
        if (key != Long.MIN_VALUE) {
            if (captureLiveValues) {
                cachedLiveKey = key;
                cachedLiveResult = result;
            } else {
                cachedRuntimeKey = key;
                cachedRuntimeResult = result;
            }
        }
        return result;
    }

    public boolean evaluate(int redstoneInput) {
        return evaluate(() -> Math.max(0, Math.min(15, redstoneInput))).machineEnabled();
    }

    public boolean consumeRuntimeDirty() {
        boolean dirty = runtimeDirty;
        runtimeDirty = false;
        return dirty;
    }

    public MachineControlSchedule copy() { return load(save()); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 12);
        tag.putInt("NextNodeId", nextNodeId);
        tag.putInt("NextVariableId", nextVariableId);
        ListTag variablesTag = new ListTag();
        for (Variable variable : variables) variablesTag.add(variable.save());
        tag.put("Variables", variablesTag);
        ListTag nodesTag = new ListTag();
        for (Node node : nodes) nodesTag.add(node.save());
        tag.put("Nodes", nodesTag);
        return tag;
    }

    public static MachineControlSchedule load(CompoundTag tag) {
        if (!tag.contains("Nodes", Tag.TAG_LIST)) return loadLegacy(tag);
        MachineControlSchedule schedule = new MachineControlSchedule(true);
        int version = tag.getInt("Version");
        boolean migrateWholeNumbers = version < 12;
        ListTag variablesTag = tag.getList("Variables", Tag.TAG_COMPOUND);
        for (int i = 0; i < variablesTag.size() && schedule.variables.size() < MAX_VARIABLES; i++) {
            Variable variable = Variable.load(variablesTag.getCompound(i), migrateWholeNumbers);
            if (variable != null && variable.id >= 0 && variable.id < MAX_VARIABLES && schedule.variable(variable.id) == null) {
                schedule.variables.add(variable);
                schedule.variablesById[variable.id] = variable;
            }
        }
        ListTag nodesTag = tag.getList("Nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodesTag.size() && schedule.nodes.size() < MAX_NODES; i++) {
            Node node = Node.load(nodesTag.getCompound(i), migrateWholeNumbers);
            if (node != null && node.id >= 0 && node.id < MAX_NODES && schedule.node(node.id) == null) {
                schedule.nodes.add(node);
                schedule.nodesById[node.id] = node;
            }
        }
        for (Node node : schedule.nodes) node.normalizeInputSlots();
        schedule.nextNodeId = Math.floorMod(tag.getInt("NextNodeId"), MAX_NODES);
        schedule.nextVariableId = Math.floorMod(tag.getInt("NextVariableId"), MAX_VARIABLES);
        if (schedule.nodes.isEmpty()) return new MachineControlSchedule();
        schedule.markProgramChanged();
        return schedule;
    }

    private static MachineControlSchedule loadLegacy(CompoundTag tag) {
        MachineControlSchedule schedule = new MachineControlSchedule();
        Node input = schedule.nodes.stream().filter(node -> node.type == NodeType.REDSTONE_INPUT).findFirst().orElse(null);
        if (input != null) {
            input.value = toScaled(Math.max(0, Math.min(15, tag.getInt("InputSelector"))));
            input.mode = input.value == 0 ? NodeMode.ANY : NodeMode.EXACT;
        }
        if (tag.contains("Inverted") && !tag.getBoolean("Inverted")) {
            Node not = schedule.nodes.stream().filter(node -> node.type == NodeType.NOT).findFirst().orElse(null);
            Node output = schedule.nodes.stream().filter(node -> node.type == NodeType.MACHINE_ENABLED).findFirst().orElse(null);
            if (not != null && output != null && input != null) {
                schedule.removeNode(not.id);
                output.inputs.clear();
                output.inputs.add(new Connection(input.id, -1, PortKind.BOOLEAN, 0));
                schedule.markProgramChanged();
            }
        }
        return schedule;
    }

    private long evaluationKey(MachineControlContext context, MachineControlVariableStore sharedVariables,
                               boolean dependsOnTick, boolean usesInputs, boolean usesRedstone, boolean usesWorld) {
        long tick = dependsOnTick ? context.evaluationTick() : 0L;
        long inputRevision = usesInputs ? context.inputRevision() : 0L;
        long worldRevision = usesWorld ? context.worldRevision() : 0L;
        if ((dependsOnTick && tick == Long.MIN_VALUE)
                || (usesInputs && inputRevision == Long.MIN_VALUE)
                || (usesWorld && worldRevision == Long.MIN_VALUE)) {
            return Long.MIN_VALUE;
        }
        long key = 0x9E3779B97F4A7C15L ^ programRevision;
        if (dependsOnTick) key = mix(key, tick);
        if (usesInputs) key = mix(key, inputRevision);
        if (usesRedstone) key = mix(key, context.redstoneInput());
        if (usesWorld) key = mix(key, worldRevision);
        if (sharedVariables != null) key = mix(key, sharedVariables.revision());
        return key;
    }

    private static long mix(long seed, long value) {
        long mixed = value + 0x9E3779B97F4A7C15L + (seed << 6) + (seed >>> 2);
        return seed ^ mixed;
    }

    private void markProgramChanged() {
        programRevision++;
        compiledProgram = null;
        invalidateRuntimeCaches();
    }

    private void invalidateRuntimeCaches() {
        cachedRuntimeKey = Long.MIN_VALUE;
        cachedLiveKey = Long.MIN_VALUE;
        cachedRuntimeResult = null;
        cachedLiveResult = null;
    }

    private CompiledProgram compiledProgram() {
        if (compiledProgram == null) compiledProgram = new CompiledProgram();
        return compiledProgram;
    }

    private boolean hasCycle(int id, Set<Integer> visiting, Set<Integer> visited) {
        if (visited.contains(id)) return false;
        if (!visiting.add(id)) return true;
        Node node = node(id);
        if (node != null) {
            for (Connection connection : node.inputs) {
                if (connection.sourceNodeId >= 0 && hasCycle(connection.sourceNodeId, visiting, visited)) return true;
            }
        }
        visiting.remove(id);
        visited.add(id);
        return false;
    }

    private int nextFreeNodeId() {
        for (int offset = 0; offset < MAX_NODES; offset++) {
            int id = (nextNodeId + offset) % MAX_NODES;
            if (node(id) == null) return id;
        }
        return -1;
    }

    private int nextFreeVariableId() {
        for (int offset = 0; offset < MAX_VARIABLES; offset++) {
            int id = (nextVariableId + offset) % MAX_VARIABLES;
            if (variable(id) == null) return id;
        }
        return -1;
    }

    private String uniqueVariableName(String base, int excludedId) {
        String candidate = base;
        int suffix = 2;
        while (variableNameExists(candidate, excludedId)) candidate = base + " " + suffix++;
        return candidate;
    }

    private boolean variableNameExists(String candidate, int excludedId) {
        for (Variable variable : variables) {
            if (variable.id != excludedId && variable.name.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }

    private static String cleanName(String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim().replaceAll("[^A-Za-z0-9 _-]", "");
        if (name.isEmpty()) name = "variable";
        return name.length() > 24 ? name.substring(0, 24) : name;
    }

    private static String cleanFilterText(String text) {
        if (text == null) return "";
        String cleaned = text.replace('\r', '\n').replaceAll("[^A-Za-z0-9_:#,;./\\-\\n ]", "").trim();
        return cleaned.length() > 256 ? cleaned.substring(0, 256) : cleaned;
    }

    private static boolean itemFilterMatches(MachineControlContext context, String text) {
        return context.itemInputCount(text) > 0;
    }

    private static boolean fluidFilterMatches(MachineControlContext context, String text) {
        return context.fluidInputAmount(text) > 0;
    }

    public static int toScaled(int wholeValue) { return clampNumber((long) wholeValue * NUMBER_SCALE); }
    public static int parseScaledNumber(String text) {
        if (text == null) throw new NumberFormatException("null");
        String value = text.trim();
        if (value.isEmpty()) throw new NumberFormatException("invalid number");
        java.math.BigDecimal decimal = new java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal scaled = decimal.movePointRight(2);
        if (scaled.compareTo(java.math.BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) return Integer.MAX_VALUE;
        if (scaled.compareTo(java.math.BigDecimal.valueOf(Integer.MIN_VALUE)) < 0) return Integer.MIN_VALUE;
        return scaled.intValueExact();
    }
    public static String formatNumber(int scaledValue) {
        long value = scaledValue;
        boolean negative = value < 0L;
        long absolute = Math.abs(value);
        long whole = absolute / NUMBER_SCALE;
        long fraction = absolute % NUMBER_SCALE;
        String prefix = negative ? "-" : "";
        if (fraction == 0) return prefix + whole;
        if (fraction % 10 == 0) return prefix + whole + "." + fraction / 10;
        return prefix + whole + "." + String.format(java.util.Locale.ROOT, "%02d", fraction);
    }
    private static int clampNumber(int value) { return Math.max(0, value); }
    private static int clampNumber(long value) { return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value)); }
    private static int clampSignedNumber(int value) { return value; }
    private static int clampSignedNumber(long value) { return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)); }
    private static boolean isSignedNumberNode(NodeType type) {
        return type == NodeType.NUMBER
                || type == NodeType.RECIPE_MIN_CB
                || type == NodeType.RECIPE_MAX_CB
                || type == NodeType.MACHINE_CB;
    }
    private static int clampWorkspace(int value) { return Math.max(WORKSPACE_MIN, Math.min(WORKSPACE_MAX, value)); }

    public enum PortKind { BOOLEAN, NUMBER }

    public enum Category { CONTROL, LOGIC, MATH, MACHINE, INPUTS, WORLD, REDSTONE, VARIABLES }

    public enum NodeMode { ANY, EXACT, AMOUNT, PERCENT }

    public enum NodeType {
        REDSTONE_INPUT(Category.INPUTS, true, true, 0, 1, 0, 1, true),
        INPUT_ENERGY(Category.INPUTS, true, true, 0, 0, 0, 0, true),
        INPUT_STEAM(Category.INPUTS, true, true, 0, 0, 0, 0, true),
        INPUT_ITEMS(Category.INPUTS, true, true, 0, 0, 0, 0, true),
        INPUT_FLUIDS(Category.INPUTS, true, true, 0, 0, 0, 0, true),
        IS_RAINING(Category.WORLD, true, false, 0, 0, 0, 0, true),
        IS_THUNDERING(Category.WORLD, true, false, 0, 0, 0, 0, true),
        HAS_DIRECT_SKY(Category.WORLD, true, false, 0, 0, 0, 0, true),
        IS_DAY(Category.WORLD, true, false, 0, 0, 0, 0, true),
        NUMBER(Category.MATH, false, true, 0, 0, 0, 0, true),
        VARIABLE_REPORTER(Category.VARIABLES, true, true, 0, 1, 0, 1, false),
        SET_VARIABLE(Category.VARIABLES, false, true, 0, 0, 1, 1, true),
        COMPARE(Category.LOGIC, true, false, 0, 0, 2, 2, true),
        NOT(Category.LOGIC, true, false, 1, 1, 0, 0, true),
        AND(Category.LOGIC, true, false, 2, 8, 0, 0, true),
        OR(Category.LOGIC, true, false, 2, 8, 0, 0, true),
        XOR(Category.LOGIC, true, false, 2, 8, 0, 0, true),
        MATH(Category.MATH, false, true, 0, 0, 2, 8, true),
        CLAMP(Category.MATH, false, true, 0, 0, 3, 3, true),
        MACHINE_ENABLED(Category.CONTROL, true, false, 1, 8, 0, 0, true),
        REDSTONE_OUTPUT(Category.REDSTONE, false, true, 1, 1, 1, 1, true),
        MACHINE_RUNNING(Category.MACHINE, true, false, 0, 0, 0, 0, true),
        HAS_ACTIVE_RECIPE(Category.MACHINE, true, false, 0, 0, 0, 0, true),
        RECIPE_PROGRESS(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        RECIPE_DURATION(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        RECIPE_REMAINING(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        RECIPE_MIN_CB(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        RECIPE_MAX_CB(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        MACHINE_CB(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        RECIPE_MIN_RPM(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        RECIPE_MAX_RPM(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        MACHINE_RPM(Category.MACHINE, false, true, 0, 0, 0, 0, true),
        MISSING_ENERGY(Category.MACHINE, true, false, 0, 0, 0, 0, true),
        OUTPUT_BLOCKED(Category.MACHINE, true, false, 0, 0, 0, 0, true),
        MISSING_INPUT(Category.MACHINE, true, false, 0, 0, 0, 0, true),
        MULTIBLOCK_FORMED(Category.MACHINE, true, false, 0, 0, 0, 0, true),
        TEMPERATURE(Category.MACHINE, false, true, 0, 0, 0, 0, true);

        private final Category category;
        private final boolean booleanOutput;
        private final boolean numberOutput;
        private final int minBooleanInputs;
        private final int maxBooleanInputs;
        private final int minNumberInputs;
        private final int maxNumberInputs;
        private final boolean paletteVisible;

        NodeType(Category category, boolean booleanOutput, boolean numberOutput,
                 int minBooleanInputs, int maxBooleanInputs, int minNumberInputs, int maxNumberInputs,
                 boolean paletteVisible) {
            this.category = category;
            this.booleanOutput = booleanOutput;
            this.numberOutput = numberOutput;
            this.minBooleanInputs = minBooleanInputs;
            this.maxBooleanInputs = maxBooleanInputs;
            this.minNumberInputs = minNumberInputs;
            this.maxNumberInputs = maxNumberInputs;
            this.paletteVisible = paletteVisible;
        }

        public Category category() { return category; }
        public boolean paletteVisible() { return paletteVisible; }
        public boolean provides(PortKind kind) { return kind == PortKind.BOOLEAN ? booleanOutput : numberOutput; }
        public boolean accepts(PortKind kind) { return maxInputs(kind) > 0; }
        public int minInputs(PortKind kind) { return kind == PortKind.BOOLEAN ? minBooleanInputs : minNumberInputs; }
        public int maxInputs(PortKind kind) { return kind == PortKind.BOOLEAN ? maxBooleanInputs : maxNumberInputs; }
        public boolean hasTextValue() { return this == INPUT_ITEMS || this == INPUT_FLUIDS; }
        public boolean supportsOperation(Operation operation) {
            return this == COMPARE ? operation.isComparison() : this == MATH && operation.isMath();
        }
        public boolean supportsMode(NodeMode mode) {
            return switch (this) {
                case REDSTONE_INPUT -> mode == NodeMode.ANY || mode == NodeMode.EXACT;
                case INPUT_ENERGY, INPUT_STEAM -> mode == NodeMode.AMOUNT || mode == NodeMode.PERCENT;
                default -> false;
            };
        }
    }

    public enum Operation {
        EQUALS, NOT_EQUALS, LESS, LESS_OR_EQUAL, GREATER, GREATER_OR_EQUAL,
        ADD, SUBTRACT, MULTIPLY, DIVIDE, MIN, MAX;

        public boolean isComparison() { return ordinal() <= GREATER_OR_EQUAL.ordinal(); }
        public boolean isMath() { return ordinal() >= ADD.ordinal(); }
    }

    public record ProgramResult(boolean machineEnabled, int redstoneOutput, Map<Integer, Integer> variables,
                                Map<Integer, Boolean> booleanValues, Map<Integer, Integer> numberValues) { }

    public record OutgoingConnection(int targetNodeId, PortKind kind, int targetSlot) { }

    public static final class Variable {
        private final int id;
        private String name;
        private int value;

        private Variable(int id, String name, int value) { this.id = id; this.name = name; this.value = clampNumber(value); }
        public int id() { return id; }
        public String name() { return name; }
        public int value() { return value; }
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", id);
            tag.putString("Name", name);
            tag.putInt("Value", value);
            return tag;
        }
        private static Variable load(CompoundTag tag, boolean migrateWholeNumbers) {
            if (!tag.contains("Name", Tag.TAG_STRING)) return null;
            int value = tag.getInt("Value");
            return new Variable(tag.getInt("Id"), cleanName(tag.getString("Name")), migrateWholeNumbers ? toScaled(value) : value);
        }
    }

    public static final class Node {
        private final int id;
        private final NodeType type;
        private final List<Connection> inputs = new ArrayList<>();
        private int x;
        private int y;
        private int value;
        private int variableId = -1;
        private Operation operation = Operation.EQUALS;
        private NodeMode mode;
        private String textValue = "";

        private Node(int id, NodeType type, int x, int y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.operation = type == NodeType.MATH ? Operation.ADD : Operation.EQUALS;
            this.mode = switch (type) {
                case REDSTONE_INPUT -> NodeMode.ANY;
                case INPUT_ENERGY, INPUT_STEAM -> NodeMode.AMOUNT;
                default -> NodeMode.EXACT;
            };
        }

        public int id() { return id; }
        public NodeType type() { return type; }
        public int x() { return x; }
        public int y() { return y; }
        public int value() { return value; }
        public int variableId() { return variableId; }
        public Operation operation() { return operation; }
        public NodeMode mode() { return mode; }
        public String textValue() { return textValue; }
        public List<Connection> inputs() { return inputs.stream().sorted(Comparator.comparing(Connection::kind).thenComparingInt(Connection::targetSlot)).toList(); }
        public boolean accepts(PortKind kind) { return type.accepts(kind); }
        public boolean provides(PortKind kind) { return type.provides(kind); }
        public int maxInputs(PortKind kind) { return type.maxInputs(kind); }
        public int visibleInputSlots(PortKind kind) {
            if (!accepts(kind)) return 0;
            int highest = inputs.stream().filter(connection -> connection.kind == kind).mapToInt(Connection::targetSlot).max().orElse(-1);
            int desired = Math.max(type.minInputs(kind), highest + 2);
            return Math.max(type.minInputs(kind), Math.min(type.maxInputs(kind), desired));
        }
        public boolean validSlot(PortKind kind, int slot) { return accepts(kind) && slot >= 0 && slot < type.maxInputs(kind); }
        public Connection connectionAt(PortKind kind, int slot) {
            return inputs.stream().filter(connection -> connection.kind == kind && connection.targetSlot == slot).findFirst().orElse(null);
        }
        public int firstAvailableSlot(PortKind kind) {
            for (int slot = 0; slot < type.maxInputs(kind); slot++) if (connectionAt(kind, slot) == null) return slot;
            return -1;
        }

        private void normalizeInputSlots() {
            for (PortKind kind : PortKind.values()) {
                List<Connection> kindConnections = new ArrayList<>(inputs.stream().filter(connection -> connection.kind == kind).toList());
                inputs.removeIf(connection -> connection.kind == kind);
                Set<Integer> used = new HashSet<>();
                for (Connection connection : kindConnections) {
                    int slot = connection.targetSlot;
                    if (!validSlot(kind, slot) || !used.add(slot)) {
                        slot = 0;
                        while (slot < type.maxInputs(kind) && used.contains(slot)) slot++;
                        if (slot >= type.maxInputs(kind)) continue;
                        used.add(slot);
                    }
                    inputs.add(new Connection(connection.sourceNodeId, connection.sourceVariableId, kind, slot));
                }
            }
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", id);
            tag.putString("Type", type.name());
            tag.putInt("X", x);
            tag.putInt("Y", y);
            tag.putInt("Value", value);
            tag.putInt("VariableId", variableId);
            tag.putString("Operation", operation.name());
            tag.putString("Mode", mode.name());
            if (!textValue.isEmpty()) tag.putString("TextValue", textValue);
            ListTag inputsTag = new ListTag();
            for (Connection connection : inputs) inputsTag.add(connection.save());
            tag.put("Inputs", inputsTag);
            return tag;
        }

        private static Node load(CompoundTag tag, boolean migrateWholeNumbers) {
            try {
                LegacyType legacy = legacyType(tag.getString("Type"));
                Node node = new Node(tag.getInt("Id"), legacy.type, clampWorkspace(tag.getInt("X")), clampWorkspace(tag.getInt("Y")));
                int storedValue = tag.getInt("Value");
                int loadedValue = migrateWholeNumbers ? toScaled(storedValue) : storedValue;
                node.value = node.type == NodeType.NUMBER ? clampSignedNumber(loadedValue) : clampNumber(loadedValue);
                node.variableId = tag.getInt("VariableId");
                if (tag.contains("Operation", Tag.TAG_STRING)) node.operation = Operation.valueOf(tag.getString("Operation"));
                if (tag.contains("Mode", Tag.TAG_STRING)) node.mode = NodeMode.valueOf(tag.getString("Mode"));
                else if (legacy.mode != null) node.mode = legacy.mode;
                if (tag.contains("TextValue", Tag.TAG_STRING)) node.textValue = cleanFilterText(tag.getString("TextValue"));
                if (!legacy.textPrefix.isEmpty() && !node.textValue.isEmpty() && !node.textValue.startsWith("#")) node.textValue = legacy.textPrefix + node.textValue;
                ListTag inputsTag = tag.getList("Inputs", Tag.TAG_COMPOUND);
                int booleanSlot = 0;
                int numberSlot = 0;
                for (int i = 0; i < inputsTag.size(); i++) {
                    Connection connection = Connection.load(inputsTag.getCompound(i));
                    if (connection == null) continue;
                    if (connection.targetSlot < 0) {
                        int slot = connection.kind == PortKind.BOOLEAN ? booleanSlot++ : numberSlot++;
                        connection = new Connection(connection.sourceNodeId, connection.sourceVariableId, connection.kind, slot);
                    }
                    node.inputs.add(connection);
                }
                return node;
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private static LegacyType legacyType(String name) {
            return switch (name) {
                case "ENERGY_STORED" -> new LegacyType(NodeType.INPUT_ENERGY, NodeMode.AMOUNT, "");
                case "ENERGY_PERCENT" -> new LegacyType(NodeType.INPUT_ENERGY, NodeMode.PERCENT, "");
                case "ITEM_INPUT_COUNT" -> new LegacyType(NodeType.INPUT_ITEMS, NodeMode.AMOUNT, "");
                case "ITEM_INPUT_MATCH" -> new LegacyType(NodeType.INPUT_ITEMS, NodeMode.AMOUNT, "");
                case "ITEM_INPUT_TAG" -> new LegacyType(NodeType.INPUT_ITEMS, NodeMode.AMOUNT, "#");
                case "FLUID_INPUT_AMOUNT" -> new LegacyType(NodeType.INPUT_FLUIDS, NodeMode.AMOUNT, "");
                case "FLUID_INPUT_MATCH" -> new LegacyType(NodeType.INPUT_FLUIDS, NodeMode.AMOUNT, "");
                case "FLUID_INPUT_TAG" -> new LegacyType(NodeType.INPUT_FLUIDS, NodeMode.AMOUNT, "#");
                case "RECIPE_MIN_PH" -> new LegacyType(NodeType.RECIPE_MIN_CB, null, "");
                case "RECIPE_MAX_PH" -> new LegacyType(NodeType.RECIPE_MAX_CB, null, "");
                case "MACHINE_PH" -> new LegacyType(NodeType.MACHINE_CB, null, "");
                default -> new LegacyType(NodeType.valueOf(name), null, "");
            };
        }
    }

    public record Connection(int sourceNodeId, int sourceVariableId, PortKind kind, int targetSlot) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Node", sourceNodeId);
            tag.putInt("Variable", sourceVariableId);
            tag.putString("Kind", kind.name());
            tag.putInt("Slot", targetSlot);
            return tag;
        }

        private static Connection load(CompoundTag tag) {
            try {
                int slot = tag.contains("Slot", Tag.TAG_INT) ? tag.getInt("Slot") : -1;
                return new Connection(tag.getInt("Node"), tag.getInt("Variable"), PortKind.valueOf(tag.getString("Kind")), slot);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private record LegacyType(NodeType type, NodeMode mode, String textPrefix) { }

    private record CompiledInput(int sourceNodeId, int sourceVariableId, int targetSlot) { }

    private final class CompiledProgram {
        private final Node[] nodeTable = MachineControlSchedule.this.nodesById.clone();
        private final CompiledInput[][] booleanInputs = new CompiledInput[MAX_NODES][];
        private final CompiledInput[][] numberInputs = new CompiledInput[MAX_NODES][];
        private final int[] variableSetterIds;
        private final int[] machineEnabledIds;
        private final int[] redstoneOutputIds;
        private final int[] liveBooleanIds;
        private final int[] liveNumberIds;
        private final boolean runtimeDependsOnTick;
        private final boolean runtimeUsesInputs;
        private final boolean runtimeUsesRedstone;
        private final boolean runtimeUsesWorld;
        private final boolean liveDependsOnTick;
        private final boolean liveUsesInputs;
        private final boolean liveUsesRedstone;
        private final boolean liveUsesWorld;

        private CompiledProgram() {
            List<Integer> setters = new ArrayList<>();
            List<Integer> enabled = new ArrayList<>();
            List<Integer> outputs = new ArrayList<>();
            List<Integer> liveBooleans = new ArrayList<>();
            List<Integer> liveNumbers = new ArrayList<>();
            for (int id = 0; id < MAX_NODES; id++) {
                Node node = nodeTable[id];
                if (node == null) {
                    booleanInputs[id] = new CompiledInput[0];
                    numberInputs[id] = new CompiledInput[0];
                    continue;
                }
                booleanInputs[id] = compileInputs(node, PortKind.BOOLEAN);
                numberInputs[id] = compileInputs(node, PortKind.NUMBER);
                if (node.type == NodeType.SET_VARIABLE
                        || (node.type == NodeType.VARIABLE_REPORTER
                        && (booleanInputs[id].length > 0 || numberInputs[id].length > 0))) setters.add(id);
                if (node.type == NodeType.MACHINE_ENABLED) enabled.add(id);
                if (node.type == NodeType.REDSTONE_OUTPUT) outputs.add(id);
                if (node.provides(PortKind.BOOLEAN)) liveBooleans.add(id);
                if (node.provides(PortKind.NUMBER)) liveNumbers.add(id);
            }
            variableSetterIds = toIntArray(setters);
            machineEnabledIds = toIntArray(enabled);
            redstoneOutputIds = toIntArray(outputs);
            liveBooleanIds = toIntArray(liveBooleans);
            liveNumberIds = toIntArray(liveNumbers);

            boolean[] runtimeReachable = new boolean[MAX_NODES];
            for (int id : variableSetterIds) markReachable(id, runtimeReachable);
            for (int id : machineEnabledIds) markReachable(id, runtimeReachable);
            for (int id : redstoneOutputIds) markReachable(id, runtimeReachable);
            boolean runtimeTick = false;
            boolean runtimeInputs = false;
            boolean runtimeRedstone = false;
            boolean runtimeWorld = false;
            boolean liveTick = false;
            boolean liveInputs = false;
            boolean liveRedstone = false;
            boolean liveWorld = false;
            for (int id = 0; id < MAX_NODES; id++) {
                Node node = nodeTable[id];
                if (node == null) continue;
                boolean tickDependent = dependsOnMachineTick(node.type);
                boolean inputDependent = dependsOnMachineInputs(node.type);
                boolean redstoneDependent = node.type == NodeType.REDSTONE_INPUT;
                boolean worldDependent = dependsOnWorld(node.type);
                if (runtimeReachable[id]) {
                    runtimeTick |= tickDependent;
                    runtimeInputs |= inputDependent;
                    runtimeRedstone |= redstoneDependent;
                    runtimeWorld |= worldDependent;
                }
                liveTick |= tickDependent;
                liveInputs |= inputDependent;
                liveRedstone |= redstoneDependent;
                liveWorld |= worldDependent;
            }
            runtimeDependsOnTick = runtimeTick;
            runtimeUsesInputs = runtimeInputs;
            runtimeUsesRedstone = runtimeRedstone;
            runtimeUsesWorld = runtimeWorld;
            liveDependsOnTick = liveTick;
            liveUsesInputs = liveInputs;
            liveUsesRedstone = liveRedstone;
            liveUsesWorld = liveWorld;
        }

        private CompiledInput[] compileInputs(Node node, PortKind kind) {
            return node.inputs.stream()
                    .filter(connection -> connection.kind == kind)
                    .sorted(Comparator.comparingInt(Connection::targetSlot))
                    .map(connection -> new CompiledInput(connection.sourceNodeId, connection.sourceVariableId, connection.targetSlot))
                    .toArray(CompiledInput[]::new);
        }

        private void markReachable(int nodeId, boolean[] reachable) {
            if (nodeId < 0 || nodeId >= MAX_NODES || reachable[nodeId] || nodeTable[nodeId] == null) return;
            reachable[nodeId] = true;
            for (CompiledInput input : booleanInputs[nodeId]) if (input.sourceNodeId >= 0) markReachable(input.sourceNodeId, reachable);
            for (CompiledInput input : numberInputs[nodeId]) if (input.sourceNodeId >= 0) markReachable(input.sourceNodeId, reachable);
        }

        private boolean dependsOnMachineTick(NodeType type) {
            return switch (type) {
                case INPUT_ENERGY, INPUT_STEAM, SET_VARIABLE,
                        MACHINE_RUNNING, HAS_ACTIVE_RECIPE, RECIPE_PROGRESS, RECIPE_DURATION,
                        RECIPE_REMAINING, RECIPE_MIN_CB, RECIPE_MAX_CB, MACHINE_CB,
                        RECIPE_MIN_RPM, RECIPE_MAX_RPM, MACHINE_RPM,
                        MISSING_ENERGY, OUTPUT_BLOCKED, MISSING_INPUT,
                        MULTIBLOCK_FORMED, TEMPERATURE -> true;
                default -> false;
            };
        }

        private boolean dependsOnMachineInputs(NodeType type) {
            return type == NodeType.INPUT_ITEMS || type == NodeType.INPUT_FLUIDS;
        }

        private boolean dependsOnWorld(NodeType type) {
            return type == NodeType.IS_RAINING
                    || type == NodeType.IS_THUNDERING
                    || type == NodeType.HAS_DIRECT_SKY
                    || type == NodeType.IS_DAY;
        }

        private int[] toIntArray(List<Integer> values) {
            return values.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private static final class EvaluationScratch {
        private final byte[] booleanStates = new byte[MAX_NODES];
        private final int[] numberValues = new int[MAX_NODES];
        private final boolean[] numberKnown = new boolean[MAX_NODES];
        private final boolean[] visitingBoolean = new boolean[MAX_NODES];
        private final boolean[] visitingNumber = new boolean[MAX_NODES];
        private final int[] variableValues = new int[MAX_VARIABLES];
        private final boolean[] variablePresent = new boolean[MAX_VARIABLES];

        private void reset() {
            Arrays.fill(booleanStates, (byte) 0);
            Arrays.fill(numberKnown, false);
            Arrays.fill(visitingBoolean, false);
            Arrays.fill(visitingNumber, false);
            Arrays.fill(variablePresent, false);
        }
    }

    private final class Evaluation {
        private static final byte UNKNOWN = 0;
        private static final byte FALSE = 1;
        private static final byte TRUE = 2;

        private final MachineControlContext context;
        private final MachineControlVariableStore sharedVariables;
        private final List<MachineControlVariableStore.Entry> sharedVariableEntries;
        private final CompiledProgram program;
        private final boolean captureLiveValues;
        private final byte[] booleanStates;
        private final int[] numberValues;
        private final boolean[] numberKnown;
        private final boolean[] visitingBoolean;
        private final boolean[] visitingNumber;
        private final int[] variableValues;
        private final boolean[] variablePresent;

        private Evaluation(MachineControlContext context, MachineControlVariableStore sharedVariables,
                           CompiledProgram program, boolean captureLiveValues, EvaluationScratch scratch) {
            this.context = context;
            this.sharedVariables = sharedVariables;
            this.sharedVariableEntries = sharedVariables == null ? List.of() : sharedVariables.entriesView();
            this.program = program;
            this.captureLiveValues = captureLiveValues;
            this.booleanStates = scratch.booleanStates;
            this.numberValues = scratch.numberValues;
            this.numberKnown = scratch.numberKnown;
            this.visitingBoolean = scratch.visitingBoolean;
            this.visitingNumber = scratch.visitingNumber;
            this.variableValues = scratch.variableValues;
            this.variablePresent = scratch.variablePresent;
            if (sharedVariables != null) {
                for (MachineControlVariableStore.Entry variable : sharedVariableEntries) {
                    variableValues[variable.id()] = variable.value();
                    variablePresent[variable.id()] = true;
                }
            } else {
                for (Variable variable : MachineControlSchedule.this.variables) {
                    variableValues[variable.id] = variable.value;
                    variablePresent[variable.id] = true;
                }
            }
        }

        private ProgramResult run() {
            if (!captureLiveValues) {
                for (int nodeId : program.variableSetterIds) executeVariableSetter(nodeId);
            }

            boolean enabled = true;
            for (int nodeId : program.machineEnabledIds) enabled &= booleanValue(nodeId);
            int redstoneOutput = 0;
            for (int nodeId : program.redstoneOutputIds) redstoneOutput = Math.max(redstoneOutput, numberValue(nodeId) / NUMBER_SCALE);

            if (captureLiveValues) {
                for (int nodeId : program.liveBooleanIds) booleanValue(nodeId);
                for (int nodeId : program.liveNumberIds) numberValue(nodeId);
            }

            if (!captureLiveValues) persistVariables();
            return new ProgramResult(
                    enabled,
                    Math.max(0, Math.min(15, redstoneOutput)),
                    captureLiveValues ? variableMap() : Map.of(),
                    captureLiveValues ? booleanMap() : Map.of(),
                    captureLiveValues ? numberMap() : Map.of()
            );
        }

        private boolean booleanValue(int nodeId) {
            if (nodeId < 0 || nodeId >= MAX_NODES) return false;
            if (booleanStates[nodeId] != UNKNOWN) return booleanStates[nodeId] == TRUE;
            Node node = program.nodeTable[nodeId];
            if (node == null || !node.provides(PortKind.BOOLEAN) || visitingBoolean[nodeId]) return false;
            visitingBoolean[nodeId] = true;
            boolean value = switch (node.type) {
                case REDSTONE_INPUT -> {
                    CompiledInput[] numeric = program.numberInputs[nodeId];
                    int expected = numeric.length == 0 ? node.value : numberInput(numeric[0]);
                    int signal = effectiveRedstoneInput(nodeId);
                    yield node.mode == NodeMode.ANY && numeric.length == 0 ? signal > 0 : signal == expected;
                }
                case INPUT_ENERGY -> context.energyStored() > 0;
                case INPUT_STEAM -> context.steamStored() > 0;
                case INPUT_ITEMS -> itemFilterMatches(context, node.textValue);
                case INPUT_FLUIDS -> fluidFilterMatches(context, node.textValue);
                case COMPARE -> compare(program.numberInputs[nodeId], node.operation);
                case NOT -> !allBooleanInputs(nodeId, false);
                case AND -> allBooleanInputs(nodeId, false);
                case OR -> anyBooleanInput(nodeId);
                case XOR -> exactlyOneBooleanInput(nodeId);
                case MACHINE_ENABLED -> allBooleanInputs(nodeId, true);
                case MACHINE_RUNNING -> context.machineRunning();
                case HAS_ACTIVE_RECIPE -> context.hasActiveRecipe();
                case MISSING_ENERGY -> context.missingEnergy();
                case OUTPUT_BLOCKED -> context.outputBlocked();
                case MISSING_INPUT -> context.missingInput();
                case MULTIBLOCK_FORMED -> context.multiblockFormed();
                case IS_RAINING -> context.isRaining();
                case IS_THUNDERING -> context.isThundering();
                case HAS_DIRECT_SKY -> context.hasDirectSky();
                case IS_DAY -> context.isDay();
                case VARIABLE_REPORTER -> resolvedVariableNodeValue(nodeId) != 0;
                default -> false;
            };
            visitingBoolean[nodeId] = false;
            booleanStates[nodeId] = value ? TRUE : FALSE;
            return value;
        }

        private int effectiveRedstoneInput(int nodeId) {
            CompiledInput[] inputs = program.booleanInputs[nodeId];
            if (inputs.length > 0) return booleanInput(inputs[0]) ? toScaled(15) : 0;
            return toScaled(Math.max(0, Math.min(15, context.redstoneInput())));
        }

        private int numberValue(int nodeId) {
            if (nodeId < 0 || nodeId >= MAX_NODES) return 0;
            if (numberKnown[nodeId]) return numberValues[nodeId];
            Node node = program.nodeTable[nodeId];
            if (node == null || !node.provides(PortKind.NUMBER) || visitingNumber[nodeId]) return 0;
            visitingNumber[nodeId] = true;
            CompiledInput[] inputs = program.numberInputs[nodeId];
            int value = switch (node.type) {
                case REDSTONE_INPUT -> effectiveRedstoneInput(nodeId);
                case INPUT_ENERGY -> node.mode == NodeMode.PERCENT
                        ? percentage(context.energyStored(), context.energyCapacity())
                        : clampNumber(context.energyStored() * NUMBER_SCALE);
                case INPUT_STEAM -> node.mode == NodeMode.PERCENT
                        ? percentage(context.steamStored(), context.steamCapacity())
                        : clampNumber((long) context.steamStored() * NUMBER_SCALE);
                case INPUT_ITEMS -> clampNumber((long) context.itemInputCount(node.textValue) * NUMBER_SCALE);
                case INPUT_FLUIDS -> clampNumber((long) context.fluidInputAmount(node.textValue) * NUMBER_SCALE);
                case NUMBER -> node.value;
                case VARIABLE_REPORTER -> resolvedVariableNodeValue(nodeId);
                case SET_VARIABLE -> inputs.length == 0 ? node.value : numberInput(inputs[0]);
                case MATH -> math(inputs, node.operation);
                case CLAMP -> clamp(inputs);
                case REDSTONE_OUTPUT -> {
                    int numberSignal = inputs.length == 0 ? node.value : numberInput(inputs[0]);
                    boolean booleanSignal = anyBooleanInput(nodeId);
                    yield Math.max(0, Math.min(toScaled(15), Math.max(numberSignal, booleanSignal ? toScaled(15) : 0)));
                }
                case RECIPE_PROGRESS -> toScaled(context.recipeProgress());
                case RECIPE_DURATION -> toScaled(context.recipeDuration());
                case RECIPE_REMAINING -> toScaled(Math.max(0, context.recipeDuration() - context.recipeProgress()));
                case RECIPE_MIN_CB -> context.recipeMinimumChemicalBalance();
                case RECIPE_MAX_CB -> context.recipeMaximumChemicalBalance();
                case MACHINE_CB -> context.machineChemicalBalance();
                case RECIPE_MIN_RPM -> toScaled(context.recipeMinimumRpm());
                case RECIPE_MAX_RPM -> toScaled(context.recipeMaximumRpm());
                case MACHINE_RPM -> toScaled(context.machineRpm());
                case TEMPERATURE -> toScaled(context.temperature());
                default -> 0;
            };
            visitingNumber[nodeId] = false;
            value = isSignedNumberNode(node.type) ? clampSignedNumber(value) : clampNumber(value);
            numberValues[nodeId] = value;
            numberKnown[nodeId] = true;
            return value;
        }

        private void executeVariableSetter(int nodeId) {
            Node node = program.nodeTable[nodeId];
            if (node == null || node.variableId < 0 || node.variableId >= MAX_VARIABLES || !variablePresent[node.variableId]) return;
            int value = node.type == NodeType.VARIABLE_REPORTER
                    ? resolvedVariableNodeValue(nodeId)
                    : resolvedSetVariableValue(nodeId, node);
            if (value < 0) return;
            value = clampNumber(value);
            variableValues[node.variableId] = value;
            numberValues[nodeId] = value;
            numberKnown[nodeId] = true;
            if (node.provides(PortKind.BOOLEAN)) booleanStates[nodeId] = value != 0 ? TRUE : FALSE;
        }

        private int resolvedVariableNodeValue(int nodeId) {
            Node node = program.nodeTable[nodeId];
            if (node == null) return 0;
            CompiledInput[] numberInputs = program.numberInputs[nodeId];
            if (numberInputs.length > 0) return numberInput(numberInputs[0]);
            CompiledInput[] booleanInputs = program.booleanInputs[nodeId];
            if (booleanInputs.length > 0) return booleanInput(booleanInputs[0]) ? 1 : 0;
            return variableValue(node.variableId);
        }

        private int resolvedSetVariableValue(int nodeId, Node node) {
            CompiledInput[] numberInputs = program.numberInputs[nodeId];
            return numberInputs.length == 0 ? node.value : numberInput(numberInputs[0]);
        }

        private boolean booleanInput(CompiledInput input) {
            return input.sourceNodeId >= 0 && booleanValue(input.sourceNodeId);
        }

        private int numberInput(CompiledInput input) {
            if (input.sourceVariableId >= 0) return variableValue(input.sourceVariableId);
            return input.sourceNodeId >= 0 ? numberValue(input.sourceNodeId) : 0;
        }

        private int variableValue(int variableId) {
            return variableId >= 0 && variableId < MAX_VARIABLES && variablePresent[variableId] ? variableValues[variableId] : 0;
        }

        private boolean allBooleanInputs(int nodeId, boolean emptyValue) {
            CompiledInput[] inputs = program.booleanInputs[nodeId];
            if (inputs.length == 0) return emptyValue;
            for (CompiledInput input : inputs) if (!booleanInput(input)) return false;
            return true;
        }

        private boolean anyBooleanInput(int nodeId) {
            for (CompiledInput input : program.booleanInputs[nodeId]) if (booleanInput(input)) return true;
            return false;
        }

        private boolean exactlyOneBooleanInput(int nodeId) {
            int trueCount = 0;
            for (CompiledInput input : program.booleanInputs[nodeId]) {
                if (booleanInput(input) && ++trueCount > 1) return false;
            }
            return trueCount == 1;
        }

        private boolean compare(CompiledInput[] inputs, Operation operation) {
            if (inputs.length < 2) return false;
            int first = numberInput(inputs[0]);
            int second = numberInput(inputs[1]);
            return switch (operation) {
                case EQUALS -> first == second;
                case NOT_EQUALS -> first != second;
                case LESS -> first < second;
                case LESS_OR_EQUAL -> first <= second;
                case GREATER -> first > second;
                case GREATER_OR_EQUAL -> first >= second;
                default -> false;
            };
        }

        private int math(CompiledInput[] inputs, Operation operation) {
            if (inputs.length == 0) return 0;
            int result = numberInput(inputs[0]);
            for (int i = 1; i < inputs.length; i++) {
                int value = numberInput(inputs[i]);
                result = switch (operation) {
                    case ADD -> clampNumber((long) result + value);
                    case SUBTRACT -> clampNumber((long) result - value);
                    case MULTIPLY -> clampNumber((long) result * value / NUMBER_SCALE);
                    case DIVIDE -> value == 0 ? 0 : clampNumber((long) result * NUMBER_SCALE / value);
                    case MIN -> Math.min(result, value);
                    case MAX -> Math.max(result, value);
                    default -> result;
                };
            }
            return result;
        }

        private int clamp(CompiledInput[] inputs) {
            if (inputs.length == 0) return 0;
            int value = numberInput(inputs[0]);
            int min = inputs.length > 1 ? numberInput(inputs[1]) : 0;
            int max = inputs.length > 2 ? numberInput(inputs[2]) : Integer.MAX_VALUE;
            return Math.max(Math.min(min, max), Math.min(Math.max(min, max), value));
        }

        private int percentage(long value, long capacity) {
            return capacity <= 0 ? 0 : clampNumber(Math.min(255L * NUMBER_SCALE, Math.max(0L, value) * 255L * NUMBER_SCALE / capacity));
        }

        private void persistVariables() {
            boolean changed = false;
            if (sharedVariables != null) {
                for (MachineControlVariableStore.Entry variable : sharedVariableEntries) {
                    changed |= sharedVariables.setValue(variable.id(), variableValues[variable.id()]);
                }
                if (changed) sharedVariables.synchronize(MachineControlSchedule.this);
            } else {
                for (Variable variable : MachineControlSchedule.this.variables) {
                    int next = variableValues[variable.id];
                    if (next == variable.value) continue;
                    variable.value = next;
                    changed = true;
                }
            }
            if (changed) {
                runtimeDirty = true;
                cachedLiveKey = Long.MIN_VALUE;
                cachedLiveResult = null;
            }
        }

        private Map<Integer, Integer> variableMap() {
            Map<Integer, Integer> result = new HashMap<>();
            if (sharedVariables != null) {
                for (MachineControlVariableStore.Entry variable : sharedVariableEntries) {
                    result.put(variable.id(), variableValues[variable.id()]);
                }
            } else {
                for (Variable variable : MachineControlSchedule.this.variables) result.put(variable.id, variableValues[variable.id]);
            }
            return Map.copyOf(result);
        }

        private Map<Integer, Boolean> booleanMap() {
            Map<Integer, Boolean> result = new HashMap<>();
            for (int nodeId : program.liveBooleanIds) {
                if (booleanStates[nodeId] != UNKNOWN) result.put(nodeId, booleanStates[nodeId] == TRUE);
            }
            return Map.copyOf(result);
        }

        private Map<Integer, Integer> numberMap() {
            Map<Integer, Integer> result = new HashMap<>();
            for (int nodeId : program.liveNumberIds) if (numberKnown[nodeId]) result.put(nodeId, numberValues[nodeId]);
            return Map.copyOf(result);
        }
    }

}
