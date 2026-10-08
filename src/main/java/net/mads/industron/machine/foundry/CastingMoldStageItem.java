package net.mads.industron.machine.foundry;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Wet/dried ceramic mold intermediate. Finished molds remain CastingMoldItem. */
public final class CastingMoldStageItem extends Item {
    public enum Stage {
        UNFIRED("Unfired "),
        DRIED_UNFIRED("Dried Unfired ");

        private final String prefix;

        Stage(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }
    }

    private final IndustrialMaterial clay;
    private final MaterialPart moldPart;
    private final Stage stage;

    public CastingMoldStageItem(IndustrialMaterial clay, MaterialPart moldPart, Stage stage) {
        super(new Properties().stacksTo(16));
        this.clay = clay;
        this.moldPart = moldPart;
        this.stage = stage;
    }

    public IndustrialMaterial clay() { return clay; }
    public MaterialPart moldPart() { return moldPart; }
    public Stage stage() { return stage; }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(stage.prefix() + clay.displayName() + " " + moldPart.displayName());
    }
}
