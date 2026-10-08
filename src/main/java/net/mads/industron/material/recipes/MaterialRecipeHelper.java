package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public final class MaterialRecipeHelper {
    private MaterialRecipeHelper() {
    }

    public static boolean hasItems(IndustrialMaterial material, MaterialPart... parts) {
        for (MaterialPart part : parts) {
            if (!material.has(part) || part.isFluid()) {
                return false;
            }
        }
        return true;
    }

    public static String itemId(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return material.existingPart(part).toString();
        }
        return Industron.MOD_ID + ":" + part.registryName(material);
    }

    /**
     * Resolves one composition substance to the physical recipe representation it owns
     * at ambient state. Liquids/gases stay fluids; solids use a real dust, pulp or owned biological item.
     */
    public static ComponentIngredient componentIngredient(IndustrialSubstance substance) {
        if (substance instanceof StructureMaterial structureMaterial) {
            try {
                return new ItemIngredient(powderItem(structureMaterial));
            } catch (IllegalStateException noStructurePowder) {
                // Gem/metal structure wrappers may intentionally share an id with their underlying
                // IndustrialMaterial and expose no separate structure powder form. Fall through to
                // the IndustrialMaterial representation instead of rejecting a legal .contains(...).
            }
        }

        IndustrialMaterial material = substance instanceof IndustrialMaterial direct
                ? direct
                : MaterialCatalog.find(substance.id());
        if (material == null) {
            throw new IllegalStateException(
                    "Contained substance " + substance.id() + " has no registered IndustrialMaterial representation"
            );
        }

        // Global .contains(...) representation precedence: a registered DUST always wins, even
        // when the material's default physical state is liquid/gas. Only materials without DUST
        // fall back to their owned biological item or registered ambient fluid/gas form. This keeps every recipe generator
        // aligned with the chemistry planner (e.g. Kavra must stay a dust when DUST exists).
        if (material.has(MaterialPart.DUST)) {
            return new ItemIngredient(materialItem(material, MaterialPart.DUST));
        }
        if (material.has(MaterialPart.BIOLOGICAL_FEED)) {
            return new ItemIngredient(materialItem(material, MaterialPart.BIOLOGICAL_FEED));
        }
        if (material.has(MaterialPart.LIQUID)) {
            return fluidIngredient(material, MaterialPart.LIQUID);
        }
        if (material.has(MaterialPart.GAS)) {
            return fluidIngredient(material, MaterialPart.GAS);
        }

        throw new IllegalStateException(
                "Contained material " + material.id()
                        + " has no registered DUST/WOOD_PULP/BIOLOGICAL_FEED/LIQUID/GAS representation; recipe generation will not invent one"
        );
    }

    /** Resolves the registered dust or pulp item of any solid substance accepted by .contains(...). */
    public static ItemLike powderItem(IndustrialSubstance substance) {
        if (substance instanceof IndustrialMaterial material) {
            if (!material.has(MaterialPart.DUST)) {
                throw new IllegalStateException("Contained material " + material.id() + " has no dust form");
            }
            return materialItem(material, MaterialPart.DUST);
        }
        if (substance instanceof StructureMaterial material) {
            MaterialPart part = material.generatedForms().contains(MaterialPart.DUST)
                    || material.hasExistingPart(MaterialPart.DUST)
                    ? MaterialPart.DUST
                    : material.generatedForms().contains(MaterialPart.WOOD_PULP)
                    || material.hasExistingPart(MaterialPart.WOOD_PULP)
                    ? MaterialPart.WOOD_PULP
                    : null;
            if (part == null) {
                throw new IllegalStateException("Contained structure material " + material.id() + " has no dust or pulp form");
            }
            if (material.hasExistingPart(part)) {
                return registeredItem(material.existingPart(part).toString());
            }
            var holder = ItemRegistry.getStructureMaterialFormItem(material, part);
            if (holder == null) {
                throw new IllegalStateException("Contained structure material " + material.id()
                        + " did not register " + part);
            }
            return holder.get();
        }

        IndustrialMaterial elementalMaterial = MaterialCatalog.find(substance.id());
        if (elementalMaterial == null) {
            throw new IllegalStateException(
                    "Contained substance " + substance.id() + " has no registered material form"
            );
        }
        if (!elementalMaterial.has(MaterialPart.DUST)) {
            throw new IllegalStateException("Contained element " + substance.id() + " has no dust form");
        }
        return materialItem(elementalMaterial, MaterialPart.DUST);
    }

    private static FluidIngredient fluidIngredient(IndustrialMaterial material, MaterialPart part) {
        if (!material.has(part)) {
            throw new IllegalStateException(
                    "Contained " + material.properties().state().name().toLowerCase(java.util.Locale.ROOT)
                            + " material " + material.id() + " has no " + part + " fluid form"
            );
        }
        return new FluidIngredient(
                IndustrialFluidLookup.fluidId(material, part).toString(),
                part == MaterialPart.GAS ? ChemistryPhase.GAS : ChemistryPhase.LIQUID
        );
    }

    private static ItemLike materialItem(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return registeredItem(material.existingPart(part).toString());
        }
        var holder = ItemRegistry.getMaterialItem(material, part);
        if (holder == null) {
            throw new IllegalStateException("Material " + material.id() + " did not register " + part);
        }
        return holder.get();
    }

    private static Item registeredItem(String id) {
        Item item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
        if (item == null || BuiltInRegistries.ITEM.getKey(item).getPath().equals("air")) {
            throw new IllegalStateException("No registered dust or pulp item exists at " + id);
        }
        return item;
    }

    public sealed interface ComponentIngredient permits ItemIngredient, FluidIngredient {
    }

    public record ItemIngredient(ItemLike item) implements ComponentIngredient {
        public ItemIngredient {
            if (item == null) throw new IllegalArgumentException("Component item cannot be null");
        }
    }

    /** Fluid id + phase; callers convert exact material units to phase-aware mB. */
    public record FluidIngredient(String fluidId, ChemistryPhase phase) implements ComponentIngredient {
        public FluidIngredient {
            if (fluidId == null || fluidId.isBlank()) {
                throw new IllegalArgumentException("Component fluid id cannot be blank");
            }
            if (phase != ChemistryPhase.LIQUID && phase != ChemistryPhase.GAS) {
                throw new IllegalArgumentException("Component fluid phase must be LIQUID or GAS");
            }
        }
    }
}
