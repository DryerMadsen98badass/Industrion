package net.mads.industron.mixin;

import net.mads.industron.progression.ProgressionMaterials;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Also covers procedural structures that place resource blocks without a template. */
@Mixin(StructurePiece.class)
public abstract class ProgressionStructurePieceMixin {
    @ModifyVariable(method="placeBlock",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private BlockState industron$materials(BlockState state,WorldGenLevel level,BlockState original,
                                          int x,int y,int z,BoundingBox bounds) {
        return ProgressionMaterials.replaceStructureState(state,ProgressionMaterials.isNether(level.getLevel()));
    }
}
