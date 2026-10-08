package net.mads.industron.machine.foundry;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.machine.foundry.casting.CastingDefinitions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Ceramic casting mold. A mold may exist before a casting volume/recipe is assigned. */
public final class CastingMoldItem extends Item {
    private final IndustrialMaterial clay;
    private final MaterialPart coldPart;
    private final MaterialPart moldPart;
    private final CastingDefinitions.Form form;

    public CastingMoldItem(IndustrialMaterial clay, CastingDefinitions.Form form) {
        this(clay, form.cold(), form.mold(), form);
    }

    public CastingMoldItem(IndustrialMaterial clay, CastingDefinitions.PendingMold pending) {
        this(clay, pending.cold(), pending.mold(), null);
    }

    private CastingMoldItem(
            IndustrialMaterial clay,
            MaterialPart coldPart,
            MaterialPart moldPart,
            CastingDefinitions.Form form
    ) {
        super(new Properties().stacksTo(16));
        this.clay = clay;
        this.coldPart = coldPart;
        this.moldPart = moldPart;
        this.form = form;
    }

    public IndustrialMaterial clay() { return clay; }
    public MaterialPart coldPart() { return coldPart; }
    public MaterialPart moldPart() { return moldPart; }

    /** Null until this mold receives a real casting volume/recipe. */
    public CastingDefinitions.Form form() { return form; }

    public boolean hasCastingRecipe() { return form != null; }

    @Override public Component getName(ItemStack stack) {
        return Component.literal(clay.displayName() + " " + moldPart.displayName());
    }
}
