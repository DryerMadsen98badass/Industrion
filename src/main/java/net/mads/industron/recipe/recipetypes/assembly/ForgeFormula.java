package net.mads.industron.recipe.recipetypes.assembly;

import java.util.OptionalInt;

/**
 * Tiny arithmetic expression used by manual anvil forging.
 *
 * <p>The definition writes only an expression in {@code y}, for example {@code y / 2} or
 * {@code y * 0.75}. Evaluation uses double precision for the entire expression and discards the
 * decimal remainder exactly once, after the full expression has completed.</p>
 */
public final class ForgeFormula {
    private final String source;

    private ForgeFormula(String source) {
        this.source = source;
        // Fail during definition bootstrap rather than during a player's forge interaction.
        evaluateDouble(100.0D);
    }

    public static ForgeFormula compile(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Forge formula cannot be blank");
        }
        return new ForgeFormula(source.trim());
    }

    public String source() {
        return source;
    }

    public OptionalInt apply(int current) {
        if (current < 0) return OptionalInt.empty();
        final double value;
        try {
            value = evaluateDouble(current);
        } catch (IllegalArgumentException exception) {
            return OptionalInt.empty();
        }
        if (!Double.isFinite(value) || value < 0.0D || value > Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        // User rule: decimals are valid inside the calculation, but the final remainder vanishes.
        return OptionalInt.of((int) Math.floor(value));
    }

    private double evaluateDouble(double y) {
        Parser parser = new Parser(source, y);
        double value = parser.expression();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected forge-formula token at " + parser.index + " in: " + source);
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Forge formula produced a non-finite value: " + source);
        }
        return value;
    }

    private static final class Parser {
        private final String text;
        private final double y;
        private int index;

        private Parser(String text, double y) {
            this.text = text;
            this.y = y;
        }

        private double expression() {
            double value = term();
            while (true) {
                skipWhitespace();
                if (take('+')) value += term();
                else if (take('-')) value -= term();
                else return value;
            }
        }

        private double term() {
            double value = factor();
            while (true) {
                skipWhitespace();
                if (take('*')) {
                    value *= factor();
                } else if (take('/')) {
                    double divisor = factor();
                    if (divisor == 0.0D) throw new IllegalArgumentException("Division by zero in forge formula: " + text);
                    value /= divisor;
                } else {
                    return value;
                }
            }
        }

        private double factor() {
            skipWhitespace();
            if (take('+')) return factor();
            if (take('-')) return -factor();
            if (take('(')) {
                double value = expression();
                skipWhitespace();
                if (!take(')')) throw new IllegalArgumentException("Missing ')' in forge formula: " + text);
                return value;
            }
            if (!atEnd() && (text.charAt(index) == 'y' || text.charAt(index) == 'Y')) {
                index++;
                return y;
            }
            return number();
        }

        private double number() {
            skipWhitespace();
            int start = index;
            boolean decimal = false;
            while (!atEnd()) {
                char c = text.charAt(index);
                if (Character.isDigit(c)) {
                    index++;
                } else if (c == '.' && !decimal) {
                    decimal = true;
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) throw new IllegalArgumentException("Expected number or y in forge formula: " + text);
            try {
                return Double.parseDouble(text.substring(start, index));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid number in forge formula: " + text, exception);
            }
        }

        private boolean take(char expected) {
            if (!atEnd() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(text.charAt(index))) index++;
        }

        private boolean atEnd() {
            return index >= text.length();
        }
    }
}
