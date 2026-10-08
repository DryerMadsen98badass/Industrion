package net.mads.industron.menu;

import net.mads.industron.recipe.stone_shaping.StoneShapingPattern;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipe;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipes;
import net.mads.industron.registry.MenuRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import java.util.BitSet;
import java.util.List;

public final class StoneShapingMenu extends AbstractContainerMenu {
    public static final int PIXEL_BUTTON_BASE = 0;
    public static final int COMPLETE_BUTTON = StoneShapingPattern.PIXELS;
    private static final int SYNC_SEGMENT_BITS = 16;
    private static final int SYNC_SEGMENTS = StoneShapingPattern.PIXELS / SYNC_SEGMENT_BITS;
    private static final float NEIGHBOR_BREAK_CHANCE = 0.05F;

    private final Player player;
    private final ResourceLocation texture;
    private final Item expectedMain;
    private final Item expectedOff;
    private final BitSet remaining = StoneShapingPattern.fullMask();

    public StoneShapingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(
                id,
                inventory,
                buffer.readResourceLocation(),
                item(buffer.readResourceLocation()),
                item(buffer.readResourceLocation())
        );
    }

    private StoneShapingMenu(int id, Inventory inventory, ResourceLocation texture, Item expectedMain, Item expectedOff) {
        super(MenuRegistry.STONE_SHAPING.get(), id);
        this.player = inventory.player;
        this.texture = texture;
        this.expectedMain = expectedMain;
        this.expectedOff = expectedOff;
        addMaskSyncSlots();
    }

    /**
     * Starts one irreversible shaping attempt. Recipe inputs are consumed before the menu opens,
     * so closing with Esc never refunds the Pebbles and Done never consumes a second pair.
     */
    public static boolean open(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        List<net.minecraft.world.item.crafting.RecipeHolder<StoneShapingRecipe>> recipes =
                StoneShapingRecipes.forHands(player.level(), main, off);
        if (recipes.isEmpty()) return false;

        ResourceLocation texture = recipes.getFirst().value().texture();
        Item mainItem = main.getItem();
        Item offItem = off.getItem();

        // Opening the shaping session is the commit point for the raw Pebbles.
        if (!player.getAbilities().instabuild) {
            main.shrink(1);
            off.shrink(1);
        }

        ((IPlayerExtension) serverPlayer).openMenu(
                new SimpleMenuProvider(
                        (id, inventory, menuPlayer) -> new StoneShapingMenu(id, inventory, texture, mainItem, offItem),
                        Component.translatable("gui.industron.stone_shaping")
                ),
                buffer -> {
                    buffer.writeResourceLocation(texture);
                    buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(mainItem));
                    buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(offItem));
                }
        );
        return true;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public boolean hasPixel(int x, int y) {
        if (x < 0 || x >= StoneShapingPattern.SIZE || y < 0 || y >= StoneShapingPattern.SIZE) return false;
        return remaining.get(StoneShapingPattern.index(x, y));
    }

    /** Client prediction for the directly struck pixel. Random fracture neighbours are server-only. */
    public void removePixelLocal(int x, int y) {
        if (x < 0 || x >= StoneShapingPattern.SIZE || y < 0 || y >= StoneShapingPattern.SIZE) return;
        remaining.clear(StoneShapingPattern.index(x, y));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player != this.player || !stillValid(player)) return false;
        if (id >= PIXEL_BUTTON_BASE && id < PIXEL_BUTTON_BASE + StoneShapingPattern.PIXELS) {
            int pixel = id - PIXEL_BUTTON_BASE;
            if (!remaining.get(pixel)) return true;
            remaining.clear(pixel);
            if (!player.level().isClientSide()) {
                fractureNeighbours(player, pixel % StoneShapingPattern.SIZE, pixel / StoneShapingPattern.SIZE);
                broadcastChanges();
            }
            return true;
        }
        if (id != COMPLETE_BUTTON || player.level().isClientSide()) return false;
        complete(player);
        return true;
    }

    private void fractureNeighbours(Player player, int x, int y) {
        tryFracture(player, x - 1, y);
        tryFracture(player, x + 1, y);
        tryFracture(player, x, y - 1);
        tryFracture(player, x, y + 1);
    }

    private void tryFracture(Player player, int x, int y) {
        if (x < 0 || x >= StoneShapingPattern.SIZE || y < 0 || y >= StoneShapingPattern.SIZE) return;
        int index = StoneShapingPattern.index(x, y);
        if (!remaining.get(index)) return;
        if (player.getRandom().nextFloat() < NEIGHBOR_BREAK_CHANCE) {
            remaining.clear(index);
        }
    }

    private void complete(Player player) {
        List<net.minecraft.world.item.crafting.RecipeHolder<StoneShapingRecipe>> matchingInputs =
                StoneShapingRecipes.forItems(player.level(), expectedMain, expectedOff).stream()
                        .filter(holder -> holder.value().texture().equals(texture))
                        .toList();
        StoneShapingRecipe matched = matchingInputs.stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .filter(recipe -> recipe.matchesRemaining(remaining))
                .findFirst()
                .orElse(null);

        if (matched != null) {
            ItemStack output = matched.result();
            if (!player.getInventory().add(output)) {
                player.drop(output, false);
            }
        }
        player.closeContainer();
    }

    private void addMaskSyncSlots() {
        for (int segment = 0; segment < SYNC_SEGMENTS; segment++) {
            final int segmentIndex = segment;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return maskSegment(segmentIndex);
                }

                @Override
                public void set(int value) {
                    applyMaskSegment(segmentIndex, value);
                }
            });
        }
    }

    private int maskSegment(int segment) {
        int start = segment * SYNC_SEGMENT_BITS;
        int value = 0;
        for (int bit = 0; bit < SYNC_SEGMENT_BITS; bit++) {
            if (remaining.get(start + bit)) value |= (1 << bit);
        }
        return value;
    }

    private void applyMaskSegment(int segment, int value) {
        int start = segment * SYNC_SEGMENT_BITS;
        for (int bit = 0; bit < SYNC_SEGMENT_BITS; bit++) {
            remaining.set(start + bit, (value & (1 << bit)) != 0);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    private static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
    }
}
