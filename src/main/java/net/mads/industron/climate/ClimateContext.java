package net.mads.industron.climate;
import net.minecraft.world.level.Level;
import java.util.ArrayDeque;
import java.util.function.Supplier;
/** Scoped server world; client supplier returns a level only on its owning render/game thread. */
public final class ClimateContext {
    private ClimateContext() {}
    private static final ThreadLocal<ArrayDeque<Level>> worlds=ThreadLocal.withInitial(ArrayDeque::new);
    public static Supplier<Level> client=()->null;
    public static void push(Level level){worlds.get().push(level);}
    public static void pop(){var stack=worlds.get();if(!stack.isEmpty())stack.pop();if(stack.isEmpty())worlds.remove();}
    public static Level level(){var stack=worlds.get();return stack.isEmpty()?client.get():stack.peek();}
}
