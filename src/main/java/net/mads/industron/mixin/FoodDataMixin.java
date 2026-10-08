package net.mads.industron.mixin;

import net.mads.industron.food.FoodBalance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow private float exhaustionLevel;
    @Shadow private float saturationLevel;

    /** Keep sprinting/jumping/healing meaningful, but make activity far less hungry than vanilla. */
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float industron$scaleActivityExhaustion(float exhaustion) {
        return exhaustion > 0.0F
                ? exhaustion * FoodBalance.ACTIVITY_EXHAUSTION_MULTIPLIER
                : exhaustion;
    }

    /**
     * Slow baseline metabolism. Saturation is deliberately removed so food's
     * visible hunger value is the reserve: four hunger bars are roughly four
     * quiet Minecraft days, with activity shortening that time.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void industron$applySlowMetabolism(Player player, CallbackInfo ci) {
        this.saturationLevel = 0.0F;
        this.exhaustionLevel = Math.min(
                40.0F,
                this.exhaustionLevel + FoodBalance.PASSIVE_EXHAUSTION_PER_TICK
        );
    }
}
