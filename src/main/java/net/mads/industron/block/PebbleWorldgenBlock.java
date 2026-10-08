package net.mads.industron.block;

import com.mojang.serialization.MapCodec;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small non-item worldgen node that represents one loose stone Pebble on the ground. */
public final class PebbleWorldgenBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 3.0D, 12.0D);
    public static final MapCodec<PebbleWorldgenBlock> CODEC = simpleCodec(properties ->
            new PebbleWorldgenBlock(net.mads.industron.material.defenitions.StoneMaterials.STONE, properties));

    private final StoneMaterial material;

    public PebbleWorldgenBlock(StoneMaterial material) {
        this(material, BlockBehaviour.Properties.of()
                .instabreak()
                .noCollission()
                .noOcclusion()
                .sound(SoundType.STONE));
    }

    private PebbleWorldgenBlock(StoneMaterial material, BlockBehaviour.Properties properties) {
        super(properties);
        this.material = material;
    }

    public StoneMaterial material() {
        return material;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()) {
            var pebble = ItemRegistry.getStructureMaterialFormItem(material, MaterialPart.PEBBLE);
            if (pebble == null) return InteractionResult.PASS;
            ItemStack stack = new ItemStack(pebble.get());
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
