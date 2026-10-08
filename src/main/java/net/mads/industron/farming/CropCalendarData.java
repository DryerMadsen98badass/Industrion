package net.mads.industron.farming;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Per-dimension records indexed by chunk. No chunks are force-loaded. */
public final class CropCalendarData extends SavedData {
    private final Map<Long, Map<Long, PlantRecord>> chunks = new HashMap<>();
    private java.util.Iterator<Long> climateChunks=java.util.Collections.emptyIterator();
    private final java.util.ArrayDeque<Long> climatePlants=new java.util.ArrayDeque<>();
    /** Fixed work budget also covers fully grown plants whose vanilla random ticks stop. */
    public void tickLoaded(ServerLevel level,int budget) {
        int examined=0;
        while(budget>0 && examined++<128) {
            if(!climatePlants.isEmpty()) {
                BlockPos pos=BlockPos.of(climatePlants.removeFirst());
                if(level.hasChunkAt(pos)){CalendarPlants.update(level,pos);budget--;}
                continue;
            }
            if(!climateChunks.hasNext()) {
                if(chunks.isEmpty())return;
                climateChunks=List.copyOf(chunks.keySet()).iterator();
                return;
            }
            long key=climateChunks.next();var chunk=new net.minecraft.world.level.ChunkPos(key);
            if(level.hasChunk(chunk.x,chunk.z))climatePlants.addAll(positions(key));
        }
    }
    public static CropCalendarData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(CropCalendarData::new,CropCalendarData::load,null),"industron_crop_calendar");
    }
    private static long chunk(BlockPos p) { return net.minecraft.world.level.ChunkPos.asLong(p.getX()>>4,p.getZ()>>4); }
    public PlantRecord get(BlockPos p) { var m=chunks.get(chunk(p));return m==null?null:m.get(p.asLong()); }
    public void put(BlockPos p, PlantRecord tag) { chunks.computeIfAbsent(chunk(p),k->new HashMap<>()).put(p.asLong(),tag);setDirty(); }
    public void remove(BlockPos p) {
        var map=chunks.get(chunk(p)); if(map!=null && map.remove(p.asLong())!=null) { if(map.isEmpty())chunks.remove(chunk(p));setDirty(); }
    }
    public List<Long> positions(long chunk) { var m=chunks.get(chunk);return m==null?List.of():List.copyOf(m.keySet()); }
    private static CropCalendarData load(CompoundTag tag, HolderLookup.Provider registries) {
        CropCalendarData data=new CropCalendarData();
        for(Tag raw:tag.getList("Plants",Tag.TAG_COMPOUND)) {
            CompoundTag entry=(CompoundTag)raw;data.put(BlockPos.of(entry.getLong("Pos")),PlantRecord.from(entry));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list=new ListTag();for(var map:chunks.values())for(var entry:map.entrySet()) {
            CompoundTag value=entry.getValue().copy();value.putLong("Pos",entry.getKey());list.add(value);
        }
        tag.put("Plants",list);return tag;
    }
}
