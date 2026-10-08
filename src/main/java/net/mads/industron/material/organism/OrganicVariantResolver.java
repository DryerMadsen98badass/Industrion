package net.mads.industron.material.organism;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Shared-form intersection + rendezvous hashing; enumeration order never affects the result. */
public final class OrganicVariantResolver {
    private OrganicVariantResolver() {}
    public static String select(String materialId, String family, Collection<? extends Set<String>> requiredForms) {
        if (requiredForms.isEmpty()) throw new IllegalArgumentException("No required texture forms");
        TreeSet<String> common = null;
        for (Set<String> candidates : requiredForms) {
            for (String candidate : candidates) if (!candidate.matches("variant_[1-9][0-9]*"))
                throw new IllegalArgumentException("Invalid variant directory: " + candidate);
            if (common == null) common=new TreeSet<>(candidates); else common.retainAll(candidates);
        }
        if (common == null || common.isEmpty()) throw new IllegalArgumentException("No complete texture pair for " + materialId + "/" + family);
        String selected=null;
        byte[] best=null;
        for (String candidate : common) {
            byte[] score=hash(materialId + "\0" + family + "\0" + candidate);
            if (best==null || Arrays.compareUnsigned(score,best)>0) { best=score; selected=candidate; }
        }
        return selected;
    }
    private static byte[] hash(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable",e); }
    }
}
