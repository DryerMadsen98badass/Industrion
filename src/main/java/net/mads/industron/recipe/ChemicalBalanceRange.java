package net.mads.industron.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Inclusive Chemical Balance (CB) range stored in hundredths so recipe checks
 * and machine-control comparisons remain deterministic.
 *
 * <p>Positive values are acidic, zero is neutral, and negative values are basic.</p>
 */
public record ChemicalBalanceRange(int minHundredths, int maxHundredths) {
    public static final int SCALE = 100;
    public static final int MIN_HUNDREDTHS = -100 * SCALE;
    public static final int MAX_HUNDREDTHS = 100 * SCALE;
    public static final int NEUTRAL_HUNDREDTHS = 0;

    public static final Codec<ChemicalBalanceRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("min").forGetter(ChemicalBalanceRange::min),
            Codec.DOUBLE.fieldOf("max").forGetter(ChemicalBalanceRange::max)
    ).apply(instance, ChemicalBalanceRange::of));

    public ChemicalBalanceRange {
        if (minHundredths < MIN_HUNDREDTHS || minHundredths > MAX_HUNDREDTHS) {
            throw new IllegalArgumentException("Minimum Chemical Balance must be between -100 and 100");
        }
        if (maxHundredths < MIN_HUNDREDTHS || maxHundredths > MAX_HUNDREDTHS) {
            throw new IllegalArgumentException("Maximum Chemical Balance must be between -100 and 100");
        }
        if (maxHundredths < minHundredths) {
            throw new IllegalArgumentException("Maximum Chemical Balance cannot be lower than minimum Chemical Balance");
        }
    }

    public static ChemicalBalanceRange of(double min, double max) {
        return new ChemicalBalanceRange(toHundredths(min), toHundredths(max));
    }

    public static int toHundredths(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Chemical Balance must be a finite number");
        }
        if (value < -100.0D || value > 100.0D) {
            throw new IllegalArgumentException("Chemical Balance must be between -100 and 100");
        }
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(SCALE))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    public static double fromHundredths(int value) {
        return value / (double) SCALE;
    }

    public double min() {
        return fromHundredths(minHundredths);
    }

    public double max() {
        return fromHundredths(maxHundredths);
    }

    public boolean containsHundredths(int value) {
        return value >= minHundredths && value <= maxHundredths;
    }

    public static String formatHundredths(int value) {
        double cb = fromHundredths(value);
        if (cb > 0.0D) {
            return String.format(java.util.Locale.ROOT, "+%.2f", cb);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", cb);
    }
}
