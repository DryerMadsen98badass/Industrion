package net.mads.industron.kinetics.shaft;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.foundation.placement.PoleHelper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

/** Shared Create-compatible runtime block for generated Industron shafts. */
public abstract class AbstractMaterialShaftBlock extends AbstractSimpleShaftBlock {
    private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new MaterialShaftPlacementHelper());

    private final ShaftLimits shaftLimits;
    private final boolean supportsSteamEngine;

    protected AbstractMaterialShaftBlock(
            BlockBehaviour.Properties properties,
            ShaftLimits shaftLimits,
            boolean supportsSteamEngine
    ) {
        super(properties.noOcclusion());
        this.shaftLimits = shaftLimits;
        this.supportsSteamEngine = supportsSteamEngine;
    }

    public final ShaftLimits shaftLimits() {
        return shaftLimits;
    }

    public final boolean supportsSteamEngine() {
        return supportsSteamEngine;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.SIX_VOXEL_POLE.get(state.getValue(AXIS));
    }

    @Override
    public float getParticleTargetRadius() {
        return 0.35F;
    }

    @Override
    public float getParticleInitialRadius() {
        return 0.125F;
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.MATERIAL_SHAFT.get();
    }

    /**
     * Mirrors Create's shaft placement assist, but accepts both Create shafts and Industron
     * material shafts as the pole being extended. Because this block also extends
     * AbstractSimpleShaftBlock, Create's own ShaftBlock helper recognises material-shaft items
     * when the player starts the placement from a normal Create shaft.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (player.isShiftKeyDown() || !player.mayBuild()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
        if (helper.matchesItem(stack) && stack.getItem() instanceof BlockItem blockItem) {
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, blockItem, player, hand, hitResult);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @MethodsReturnNonnullByDefault
    private static final class MaterialShaftPlacementHelper extends PoleHelper<Direction.Axis> {
        private MaterialShaftPlacementHelper() {
            super(
                    state -> state.getBlock() instanceof AbstractShaftBlock,
                    state -> state.getValue(AXIS),
                    AXIS
            );
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return stack -> stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof AbstractShaftBlock;
        }
    }
}
