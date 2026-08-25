package net.mads.industron.item;

import net.minecraft.resources.ResourceLocation;

public record SimpleItemDefinition(
        String id,
        String displayName,
        String texture,
        Integer color,
        double furnaceFuelItems,
        int durability
) {

    public static final int TICKS_PER_SMELTED_ITEM = 200;

    public SimpleItemDefinition {
        if (id == null
                || id.isBlank()
                || !ResourceLocation.isValidPath(id)) {
            throw new IllegalArgumentException(
                    "Invalid simple item id: " + id
            );
        }

        if (displayName == null
                || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Simple item display name cannot be blank: "
                            + id
            );
        }

        if (texture == null
                || texture.isBlank()) {
            throw new IllegalArgumentException(
                    "Simple item texture cannot be blank: "
                            + id
            );
        }

        if (texture.contains(":")) {
            if (ResourceLocation.tryParse(texture) == null) {
                throw new IllegalArgumentException(
                        "Invalid simple item texture: "
                                + texture
                );
            }
        } else if (!ResourceLocation.isValidPath(texture)) {
            throw new IllegalArgumentException(
                    "Invalid simple item texture path: "
                            + texture
            );
        }

        if (!Double.isFinite(furnaceFuelItems)
                || furnaceFuelItems < 0) {
            throw new IllegalArgumentException(
                    "furnaceFuelItems must be a finite, "
                            + "non-negative number: "
                            + id
            );
        }

        if (durability < 0) {
            throw new IllegalArgumentException(
                    "Durability cannot be negative: "
                            + id
            );
        }

        /*
         * Gjør sekssifrede RGB-farger helt synlige automatisk.
         *
         * 0x3B3B3B blir til 0xFF3B3B3B.
         *
         * Farger som allerede inneholder alpha, som
         * 0x803B3B3B, beholdes uendret.
         */
        if (color != null) {
            color = normalizeColor(color);
        }
    }

    /**
     * Vanlig item.
     *
     * Bruker itemets ID for automatisk tekstursøk.
     */
    public SimpleItemDefinition(
            String id,
            String displayName
    ) {
        this(
                id,
                displayName,
                id,
                null,
                0,
                0
        );
    }

    /**
     * Item med valgfri eksisterende tekstur og farge.
     */
    public SimpleItemDefinition(
            String id,
            String displayName,
            String texture,
            int color
    ) {
        this(
                id,
                displayName,
                texture,
                Integer.valueOf(color),
                0,
                0
        );
    }

    public SimpleItemDefinition furnaceFuel(
            double items
    ) {
        return new SimpleItemDefinition(
                id,
                displayName,
                texture,
                color,
                items,
                durability
        );
    }

    /**
     * Gir itemet durability.
     *
     * Items med durability skal registreres med maks stack size 1.
     */
    public SimpleItemDefinition durability(
            int durability
    ) {
        if (durability <= 0) {
            throw new IllegalArgumentException(
                    "Durability must be greater than zero: "
                            + id
            );
        }

        return new SimpleItemDefinition(
                id,
                displayName,
                texture,
                color,
                furnaceFuelItems,
                durability
        );
    }

    public boolean hasColor() {
        return color != null;
    }

    public int itemColor() {
        return color != null
                ? color
                : 0xFFFFFFFF;
    }

    public boolean isFurnaceFuel() {
        return furnaceFuelItems > 0;
    }

    public boolean hasDurability() {
        return durability > 0;
    }

    public int maxStackSize() {
        return hasDurability() ? 1 : 64;
    }

    public int furnaceBurnTimeTicks() {
        if (!isFurnaceFuel()) {
            return 0;
        }

        double ticks =
                furnaceFuelItems * TICKS_PER_SMELTED_ITEM;

        if (ticks > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Fuel burn time is too large for item: "
                            + id
            );
        }

        return Math.max(
                1,
                (int) Math.round(ticks)
        );
    }

    private static int normalizeColor(
            int color
    ) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }

        return color;
    }
}