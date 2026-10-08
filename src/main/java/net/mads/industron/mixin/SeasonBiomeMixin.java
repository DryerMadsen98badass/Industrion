package net.mads.industron.mixin;
import net.mads.industron.climate.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Biome.class)
public abstract class SeasonBiomeMixin {
    @Inject(method="getPrecipitationAt",at=@At("HEAD"),cancellable=true)
    private void industron$precipitation(BlockPos pos,CallbackInfoReturnable<Biome.Precipitation> cir) {
        Level level=ClimateContext.level();if(level==null||level.dimension()!=Level.OVERWORLD)return;
        Biome biome=(Biome)(Object)this;
        cir.setReturnValue(!biome.hasPrecipitation()?Biome.Precipitation.NONE:ClimateWorld.ambient(level,pos)<=0?Biome.Precipitation.SNOW:Biome.Precipitation.RAIN);
    }
    @Inject(method="coldEnoughToSnow",at=@At("HEAD"),cancellable=true)
    private void industron$cold(BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        Level level=ClimateContext.level();if(level!=null&&level.dimension()==Level.OVERWORLD)cir.setReturnValue(ClimateWorld.ambient(level,pos)<=0);
    }
    @Inject(method="warmEnoughToRain",at=@At("HEAD"),cancellable=true)
    private void industron$warm(BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        Level level=ClimateContext.level();if(level!=null&&level.dimension()==Level.OVERWORLD)cir.setReturnValue(ClimateWorld.ambient(level,pos)>0);
    }
    @Inject(method="shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",at=@At("HEAD"),cancellable=true)
    private void industron$freeze(LevelReader reader,BlockPos pos,boolean edge,CallbackInfoReturnable<Boolean> cir) {
        if(!(reader instanceof Level level)||level.dimension()!=Level.OVERWORLD)return;
        boolean valid=ClimateWorld.ambient(level,pos)<=0 && pos.getY()>=level.getMinBuildHeight()&&pos.getY()<level.getMaxBuildHeight()
            &&level.getBrightness(LightLayer.BLOCK,pos)<10 &&level.getBlockState(pos).is(Blocks.WATER)&&level.getFluidState(pos).isSource();
        if(valid&&edge)valid=!(level.isWaterAt(pos.west())&&level.isWaterAt(pos.east())&&level.isWaterAt(pos.north())&&level.isWaterAt(pos.south()));
        cir.setReturnValue(valid);
    }
    @Inject(method="shouldSnow",at=@At("HEAD"),cancellable=true)
    private void industron$snow(LevelReader reader,BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        if(!(reader instanceof Level level)||level.dimension()!=Level.OVERWORLD)return;
        cir.setReturnValue(((Biome)(Object)this).hasPrecipitation()&&ClimateWorld.ambient(level,pos)<=0
            &&pos.getY()>=level.getMinBuildHeight()&&pos.getY()<level.getMaxBuildHeight()&&level.getBrightness(LightLayer.BLOCK,pos)<10
            &&(level.getBlockState(pos).isAir()||level.getBlockState(pos).is(Blocks.SNOW))&&Blocks.SNOW.defaultBlockState().canSurvive(level,pos));
    }
}
