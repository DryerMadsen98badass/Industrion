package net.mads.industron.mixin;
import net.mads.industron.climate.ClimateMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Winter dawn is later than vanilla's wake-up clock. Avoid waking into another sleepable night. */
@Mixin(ServerLevel.class)
public abstract class SeasonSleepMixin {
    @ModifyArg(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"),index=0)
    private long industron$wakeAtDaylight(long nextDay) {
        if(((ServerLevel)(Object)this).dimension()!=Level.OVERWORLD)return nextDay;
        double dawn=6000-ClimateMath.daylightTicks(nextDay/24000.0)/2;
        return nextDay+(long)Math.max(500,dawn+500);
    }
}
