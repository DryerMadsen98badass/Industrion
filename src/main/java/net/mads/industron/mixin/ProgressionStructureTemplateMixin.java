package net.mads.industron.mixin;

import net.mads.industron.progression.ProgressionMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.mads.industron.progression.ProgressionStructureNbt;

import java.util.ArrayList;
import java.util.List;

/** Runs after structure processors, including ruined-portal weathering and gold placement. */
@Mixin(StructureTemplate.class)
public abstract class ProgressionStructureTemplateMixin {
    @Inject(method="processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)Ljava/util/List;", at=@At("RETURN"), cancellable=true)
    private static void industron$materials(ServerLevelAccessor level, BlockPos origin, BlockPos pivot,
            StructurePlaceSettings settings, List<StructureTemplate.StructureBlockInfo> input, StructureTemplate template,
            CallbackInfoReturnable<List<StructureTemplate.StructureBlockInfo>> callback) {
        boolean nether=ProgressionMaterials.isNether(level.getLevel());
        List<StructureTemplate.StructureBlockInfo> blocks=new ArrayList<>(callback.getReturnValue().size());
        for(var info:callback.getReturnValue()) {
            var state=ProgressionMaterials.replaceStructureState(info.state(),nether);
            var nbt = ProgressionStructureNbt.replace(info.nbt(), level.registryAccess(), nether);
            blocks.add(state==info.state() && nbt==info.nbt()?info:
                    new StructureTemplate.StructureBlockInfo(info.pos(),state,nbt));
        }
        callback.setReturnValue(blocks);
    }
}
