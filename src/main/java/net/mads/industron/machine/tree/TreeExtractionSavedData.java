package net.mads.industron.machine.tree;

import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TreeExtractionSavedData extends SavedData {

    private static final String NAME = Industron.MOD_ID + "_tree_extraction";

    private final Map<UUID, TreeEntry> trees = new HashMap<>();
    private final Map<Long, UUID> treeByRoot = new HashMap<>();

    private TreeExtractionSavedData() {
    }

    public static TreeExtractionSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(
                        TreeExtractionSavedData::new,
                        TreeExtractionSavedData::load
                ),
                NAME
        );
    }

    private static TreeExtractionSavedData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        TreeExtractionSavedData data = new TreeExtractionSavedData();
        ListTag treeTags = tag.getList("Trees", Tag.TAG_COMPOUND);

        for (Tag treeTag : treeTags) {
            if (!(treeTag instanceof CompoundTag treeCompound)) {
                continue;
            }

            UUID treeId;

            try {
                treeId = treeCompound.getUUID("TreeId");
            } catch (IllegalArgumentException exception) {
                continue;
            }

            ResourceLocation logId = ResourceLocation.tryParse(
                    treeCompound.getString("Log")
            );

            if (logId == null) {
                continue;
            }

            int remainingLogs = treeCompound.getInt("RemainingLogs");

            if (remainingLogs <= 0) {
                continue;
            }

            Set<BlockPos> roots = new LinkedHashSet<>();
            ListTag rootTags = treeCompound.getList("Roots", Tag.TAG_COMPOUND);

            for (Tag rootTag : rootTags) {
                if (rootTag instanceof CompoundTag rootCompound) {
                    roots.add(readPos(rootCompound));
                }
            }

            if (roots.isEmpty()) {
                continue;
            }

            TreeEntry entry = new TreeEntry(
                    treeId,
                    logId,
                    remainingLogs,
                    roots
            );

            data.trees.put(treeId, entry);

            for (BlockPos root : roots) {
                data.treeByRoot.put(root.asLong(), treeId);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        ListTag treeTags = new ListTag();

        for (TreeEntry entry : trees.values()) {
            if (entry.remainingLogs() <= 0 || entry.roots().isEmpty()) {
                continue;
            }

            CompoundTag treeCompound = new CompoundTag();
            treeCompound.putUUID("TreeId", entry.treeId());
            treeCompound.putString("Log", entry.logId().toString());
            treeCompound.putInt("RemainingLogs", entry.remainingLogs());

            ListTag rootTags = new ListTag();

            for (BlockPos root : entry.roots()) {
                rootTags.add(writePos(root));
            }

            treeCompound.put("Roots", rootTags);
            treeTags.add(treeCompound);
        }

        tag.put("Trees", treeTags);
        return tag;
    }

    public void registerTree(
            ResourceLocation logId,
            int logCount,
            Set<BlockPos> roots
    ) {
        if (logCount <= 0 || roots == null || roots.isEmpty()) {
            return;
        }

        Set<BlockPos> immutableRoots = new LinkedHashSet<>();

        for (BlockPos root : roots) {
            immutableRoots.add(root.immutable());
        }

        removeTreesAtRoots(immutableRoots);

        UUID treeId = UUID.randomUUID();
        TreeEntry entry = new TreeEntry(
                treeId,
                logId,
                logCount,
                immutableRoots
        );

        trees.put(treeId, entry);

        for (BlockPos root : immutableRoots) {
            treeByRoot.put(root.asLong(), treeId);
        }

        setDirty();
    }

    public Optional<TreeEntry> getTree(BlockPos rootPos) {
        UUID treeId = treeByRoot.get(rootPos.asLong());

        if (treeId == null) {
            return Optional.empty();
        }

        TreeEntry entry = trees.get(treeId);

        if (entry == null || entry.remainingLogs() <= 0) {
            treeByRoot.remove(rootPos.asLong());
            return Optional.empty();
        }

        return Optional.of(entry);
    }

    public boolean hasTree(BlockPos rootPos) {
        return getTree(rootPos).isPresent();
    }

    public int getRemainingLogs(BlockPos rootPos) {
        return getTree(rootPos)
                .map(TreeEntry::remainingLogs)
                .orElse(0);
    }

    public Optional<ResourceLocation> getLogId(BlockPos rootPos) {
        return getTree(rootPos)
                .map(TreeEntry::logId);
    }

    public boolean consumeLog(BlockPos rootPos) {
        return consumeLogs(rootPos, 1);
    }

    public boolean consumeLogs(BlockPos rootPos, int amount) {
        if (amount <= 0) {
            return false;
        }

        UUID treeId = treeByRoot.get(rootPos.asLong());

        if (treeId == null) {
            return false;
        }

        TreeEntry current = trees.get(treeId);

        if (current == null || current.remainingLogs() < amount) {
            return false;
        }

        int remainingLogs = current.remainingLogs() - amount;

        if (remainingLogs <= 0) {
            removeTree(treeId);
            return true;
        }

        trees.put(
                treeId,
                new TreeEntry(
                        current.treeId(),
                        current.logId(),
                        remainingLogs,
                        current.roots()
                )
        );

        setDirty();
        return true;
    }

    public void removeRoot(BlockPos rootPos) {
        UUID treeId = treeByRoot.remove(rootPos.asLong());

        if (treeId == null) {
            return;
        }

        TreeEntry current = trees.get(treeId);

        if (current == null) {
            setDirty();
            return;
        }

        Set<BlockPos> remainingRoots = new LinkedHashSet<>(current.roots());
        remainingRoots.remove(rootPos);

        if (remainingRoots.isEmpty()) {
            trees.remove(treeId);
        } else {
            trees.put(
                    treeId,
                    new TreeEntry(
                            current.treeId(),
                            current.logId(),
                            current.remainingLogs(),
                            remainingRoots
                    )
            );
        }

        setDirty();
    }

    public void removeTreeAt(BlockPos rootPos) {
        UUID treeId = treeByRoot.get(rootPos.asLong());

        if (treeId != null) {
            removeTree(treeId);
        }
    }

    private void removeTreesAtRoots(Set<BlockPos> roots) {
        Set<UUID> treeIds = new LinkedHashSet<>();

        for (BlockPos root : roots) {
            UUID treeId = treeByRoot.get(root.asLong());

            if (treeId != null) {
                treeIds.add(treeId);
            }
        }

        for (UUID treeId : treeIds) {
            removeTree(treeId);
        }
    }

    private void removeTree(UUID treeId) {
        TreeEntry removed = trees.remove(treeId);

        if (removed == null) {
            return;
        }

        for (BlockPos root : removed.roots()) {
            treeByRoot.remove(root.asLong());
        }

        setDirty();
    }

    public List<TreeEntry> trees() {
        return new ArrayList<>(trees.values());
    }

    private static CompoundTag writePos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    private static BlockPos readPos(CompoundTag tag) {
        return new BlockPos(
                tag.getInt("X"),
                tag.getInt("Y"),
                tag.getInt("Z")
        );
    }

    public record TreeEntry(
            UUID treeId,
            ResourceLocation logId,
            int remainingLogs,
            Set<BlockPos> roots
    ) {
        public TreeEntry {
            roots = Set.copyOf(roots);
        }

        public boolean matchesLog(ResourceLocation requestedLogId) {
            return logId.equals(requestedLogId);
        }
    }
}