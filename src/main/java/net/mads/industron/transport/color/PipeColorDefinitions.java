package net.mads.industron.transport.color;

import net.mads.industron.Industron;
import net.mads.industron.transport.FluidTransportTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PipeColorDefinitions {
    public static final PipeFamily CREATE_PIPE = new PipeFamily(
            "fluid_pipe",
            "Fluid Pipe",
            ResourceLocation.fromNamespaceAndPath("create", "fluid_pipe"),
            ResourceLocation.fromNamespaceAndPath("create", "glass_fluid_pipe"),
            null
    );

    private PipeColorDefinitions() {
    }

    public static List<PipeFamily> allFamilies() {
        List<PipeFamily> families = new ArrayList<>();
        families.add(CREATE_PIPE);
        for (FluidTransportTier tier : FluidTransportTier.all()) {
            families.add(forTier(tier));
        }
        return List.copyOf(families);
    }

    public static PipeFamily forTier(FluidTransportTier tier) {
        return new PipeFamily(
                tier.pipeId(),
                tier.pipeDisplayName(),
                ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, tier.pipeId()),
                ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, tier.glassPipeId()),
                tier
        );
    }

    public static String colorDisplayName(DyeColor color) {
        String[] words = color.getName().split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            name.append(word.substring(1));
        }
        return name.toString();
    }

    public static String sharedColoredModelId(DyeColor color) {
        return "pipe_colors/" + color.getName();
    }

    public record PipeFamily(
            String id,
            String displayName,
            ResourceLocation basePipeId,
            ResourceLocation baseGlassPipeId,
            @Nullable FluidTransportTier tier
    ) {
        public boolean isCreatePipe() {
            return tier == null;
        }

        public String coloredPipeId(DyeColor color) {
            return color.getName() + "_" + id;
        }

        public String coloredGlassPipeId(DyeColor color) {
            if (isCreatePipe()) {
                return color.getName() + "_glass_fluid_pipe";
            }
            return color.getName() + "_" + tier.glassPipeId();
        }

        public String coloredEncasedPipeId(DyeColor color) {
            if (isCreatePipe()) {
                return color.getName() + "_encased_fluid_pipe";
            }
            return color.getName() + "_encased_" + tier.pipeId();
        }

        public ResourceLocation coloredPipeLocation(DyeColor color) {
            return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, coloredPipeId(color));
        }

        public ResourceLocation coloredGlassPipeLocation(DyeColor color) {
            return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, coloredGlassPipeId(color));
        }

        public String coloredDisplayName(DyeColor color) {
            return PipeColorDefinitions.colorDisplayName(color) + " " + displayName;
        }

        public String coloredGlassDisplayName(DyeColor color) {
            String baseName = isCreatePipe() ? "Glass Fluid Pipe" : tier.glassPipeDisplayName();
            return PipeColorDefinitions.colorDisplayName(color) + " " + baseName;
        }

        public TagKey<Item> allVariantsTag() {
            return itemTag("pipe_variants/" + id);
        }

        public TagKey<Item> coloredVariantsTag() {
            return itemTag("colored_pipe_variants/" + id);
        }

        private static TagKey<Item> itemTag(String path) {
            return TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, path)
            );
        }
    }
}
