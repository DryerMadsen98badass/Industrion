package net.mads.industron.material.structure;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Vanilla 1.21.1 boat-item placement logic pointed at a generated per-wood EntityType. */
public final class StructureWoodBoatItem extends Item {
    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

    private final Supplier<? extends EntityType<? extends Boat>> entityType;

    public StructureWoodBoatItem(
            Supplier<? extends EntityType<? extends Boat>> entityType,
            Item.Properties properties
    ) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) return InteractionResultHolder.pass(stack);

        Vec3 view = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(
                player,
                player.getBoundingBox().expandTowards(view.scale(5.0D)).inflate(1.0D),
                ENTITY_PREDICATE
        );
        if (!entities.isEmpty()) {
            Vec3 eyes = player.getEyePosition();
            for (Entity entity : entities) {
                AABB bounds = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (bounds.contains(eyes)) return InteractionResultHolder.pass(stack);
            }
        }

        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        Boat boat = entityType.get().create(level);
        if (boat == null) return InteractionResultHolder.fail(stack);
        Vec3 location = hit.getLocation();
        boat.setPos(location.x, location.y, location.z);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResultHolder.fail(stack);

        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                EntityType.<Boat>createDefaultStackConfig(serverLevel, stack, player).accept(boat);
            }
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
