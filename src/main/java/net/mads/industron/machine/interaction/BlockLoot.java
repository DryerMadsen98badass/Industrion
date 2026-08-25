package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.Industron;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Drop rule used when a machine interaction removes a block. */
public record BlockLoot(Mode mode, List<Entry> entries) {
    public static final Codec<BlockLoot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Mode::valueOf, Mode::name).fieldOf("mode").forGetter(BlockLoot::mode),
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(BlockLoot::entries)
    ).apply(instance, BlockLoot::new));

    public BlockLoot {
        entries = List.copyOf(entries);
    }

    /** Drops the removed block itself. */
    public static BlockLoot dropSelf() {
        return new BlockLoot(Mode.SELF, List.of());
    }

    /** Drops nothing. */
    public static BlockLoot none() {
        return new BlockLoot(Mode.NONE, List.of());
    }

    /** Rolls one item independently. */
    public static BlockLoot item(String itemId, float chance) {
        return allOf(entry(itemId, chance));
    }

    /** Rolls every entry independently. */
    public static BlockLoot allOf(Entry... entries) {
        return new BlockLoot(Mode.ALL_OF, List.of(entries));
    }

    /** Selects at most one entry using the supplied weights/chances. */
    public static BlockLoot anyOf(Entry... entries) {
        return new BlockLoot(Mode.ANY_OF, List.of(entries));
    }

    /** Defines one weighted/chanced item entry. */
    public static Entry entry(String itemId, float chance) {
        return new Entry(id(itemId), clampChance(chance));
    }

    public List<ItemStack> roll(BlockState removedState, RandomSource random) {
        return switch (mode) {
            case NONE -> List.of();
            case SELF -> List.of(new ItemStack(removedState.getBlock().asItem()));
            case ALL_OF -> rollAll(random);
            case ANY_OF -> rollAny(random);
        };
    }

    private List<ItemStack> rollAll(RandomSource random) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Entry entry : entries) {
            if (random.nextFloat() < entry.chance()) {
                stacks.add(entry.stack());
            }
        }
        return stacks;
    }

    private List<ItemStack> rollAny(RandomSource random) {
        float total = 0.0F;
        for (Entry entry : entries) {
            total += Math.max(0.0F, entry.chance());
        }
        if (total <= 0.0F) {
            return List.of();
        }
        float selected = random.nextFloat() * total;
        for (Entry entry : entries) {
            selected -= Math.max(0.0F, entry.chance());
            if (selected <= 0.0F) {
                return List.of(entry.stack());
            }
        }
        return List.of();
    }

    private static ResourceLocation id(String id) {
        return id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }

    private static float clampChance(float chance) {
        return Math.max(0.0F, Math.min(1.0F, chance));
    }

    public enum Mode {
        NONE,
        SELF,
        ALL_OF,
        ANY_OF
    }

    /** Single possible item drop. */
    public record Entry(ResourceLocation itemId, float chance) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::itemId),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Entry::chance)
        ).apply(instance, Entry::new));

        public ItemStack stack() {
            return new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        }
    }
}
