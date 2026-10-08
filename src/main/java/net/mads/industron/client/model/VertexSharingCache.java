package net.mads.industron.client.model;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
/** One bake only: the index is discarded after resource reload. Arrays must remain immutable. */
public final class VertexSharingCache {
    private final Map<Key,int[]> arrays=new HashMap<>();
    private long savedBytes;
    public int[] share(int[] vertices) {
        int[] existing=arrays.putIfAbsent(new Key(vertices),vertices);
        if(existing==null||existing==vertices)return vertices;
        savedBytes+=(long)vertices.length*Integer.BYTES;
        return existing;
    }
    public long savedBytes(){return savedBytes;}
    private record Key(int[] values) {
        @Override public int hashCode(){return Arrays.hashCode(values);}
        @Override public boolean equals(Object other){return other instanceof Key key&&Arrays.equals(values,key.values);}
    }
}
