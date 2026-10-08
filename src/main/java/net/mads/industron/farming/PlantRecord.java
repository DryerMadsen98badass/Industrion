package net.mads.industron.farming;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

/** Live calendar state uses primitives; the existing NBT format remains the save/network format. */
public final class PlantRecord {
    private static final String[] DOUBLES={"Last","Planted","Growth","Temperature","Humidity","ShelterHeat","Stress","LastAdded","LastElapsed","Fruit","UnloadedAdded","UnloadedDays","UnloadedFrom"};
    private static final String[] INTS={"Age","Height","RainMode"};
    private static final String[] BOOLS={"Water","Light","Aquatic","Roof"};
    private static final Map<String,Integer> FIELDS=fields();
    private final double[] doubles=new double[DOUBLES.length];
    private final int[] ints=new int[INTS.length];
    private long present,generation;
    private int booleans;
    private String block="";
    private CompoundTag extras;
    private static Map<String,Integer> fields() {
        Map<String,Integer> map=new HashMap<>();int index=0;
        for(String key:DOUBLES)map.put(key,index++);
        for(String key:INTS)map.put(key,index++);
        for(String key:BOOLS)map.put(key,index++);
        map.put("Generation",index++);map.put("Block",index);
        return Map.copyOf(map);
    }
    private void mark(String key){present|=1L<<FIELDS.get(key);}
    private CompoundTag extras(){if(extras==null)extras=new CompoundTag();return extras;}
    public boolean contains(String key){Integer index=FIELDS.get(key);return index!=null?(present&(1L<<index))!=0:extras!=null&&extras.contains(key);}
    public double getDouble(String key){Integer index=FIELDS.get(key);return index!=null&&index<DOUBLES.length?doubles[index]:extras==null?0:extras.getDouble(key);}
    public void putDouble(String key,double value){Integer index=FIELDS.get(key);if(index!=null&&index<DOUBLES.length){doubles[index]=value;mark(key);}else extras().putDouble(key,value);}
    public int getInt(String key){Integer index=FIELDS.get(key);return index!=null&&index>=DOUBLES.length&&index<DOUBLES.length+INTS.length?ints[index-DOUBLES.length]:extras==null?0:extras.getInt(key);}
    public void putInt(String key,int value){Integer index=FIELDS.get(key);if(index!=null&&index>=DOUBLES.length&&index<DOUBLES.length+INTS.length){ints[index-DOUBLES.length]=value;mark(key);}else extras().putInt(key,value);}
    public boolean getBoolean(String key){Integer index=FIELDS.get(key);int offset=DOUBLES.length+INTS.length;return index!=null&&index>=offset&&index<offset+BOOLS.length?(booleans&(1<<(index-offset)))!=0:extras!=null&&extras.getBoolean(key);}
    public void putBoolean(String key,boolean value){Integer index=FIELDS.get(key);int offset=DOUBLES.length+INTS.length;if(index!=null&&index>=offset&&index<offset+BOOLS.length){int bit=1<<(index-offset);booleans=value?booleans|bit:booleans&~bit;mark(key);}else extras().putBoolean(key,value);}
    public long getLong(String key){return key.equals("Generation")?generation:extras==null?0:extras.getLong(key);}
    public void putLong(String key,long value){if(key.equals("Generation")){generation=value;mark(key);}else extras().putLong(key,value);}
    public String getString(String key){return key.equals("Block")?block:extras==null?"":extras.getString(key);}
    public void putString(String key,String value){if(key.equals("Block")){block=value;mark(key);}else extras().putString(key,value);}
    public void remove(String key) {
        Integer index=FIELDS.get(key);
        if(index==null){if(extras!=null)extras.remove(key);return;}
        present&=~(1L<<index);
        if(index<DOUBLES.length)doubles[index]=0;
        else if(index<DOUBLES.length+INTS.length)ints[index-DOUBLES.length]=0;
        else if(index<DOUBLES.length+INTS.length+BOOLS.length)booleans&=~(1<<(index-DOUBLES.length-INTS.length));
        else if(key.equals("Generation"))generation=0;else block="";
    }
    public CompoundTag copy(){
        CompoundTag tag=extras==null?new CompoundTag():extras.copy();
        for(String key:DOUBLES)if(contains(key))tag.putDouble(key,getDouble(key));
        for(String key:INTS)if(contains(key))tag.putInt(key,getInt(key));
        for(String key:BOOLS)if(contains(key))tag.putBoolean(key,getBoolean(key));
        if(contains("Generation"))tag.putLong("Generation",generation);
        if(contains("Block"))tag.putString("Block",block);
        return tag;
    }
    public static PlantRecord from(CompoundTag tag) {
        PlantRecord record=new PlantRecord();
        for(String key:DOUBLES)if(tag.contains(key))record.putDouble(key,tag.getDouble(key));
        for(String key:INTS)if(tag.contains(key))record.putInt(key,tag.getInt(key));
        for(String key:BOOLS)if(tag.contains(key))record.putBoolean(key,tag.getBoolean(key));
        if(tag.contains("Generation"))record.putLong("Generation",tag.getLong("Generation"));
        if(tag.contains("Block"))record.putString("Block",tag.getString("Block"));
        for(String key:tag.getAllKeys())if(!FIELDS.containsKey(key)&&!key.equals("Pos"))record.extras().put(key,tag.get(key).copy());
        return record;
    }
}
