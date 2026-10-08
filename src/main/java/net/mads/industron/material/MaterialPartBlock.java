package net.mads.industron.material;

/** Shared identity for generated material blocks, including shaped slab/stair/wall blocks. */
public interface MaterialPartBlock {
    IndustrialMaterial material();
    MaterialPart part();
}
