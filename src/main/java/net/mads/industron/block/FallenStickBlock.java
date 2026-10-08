package net.mads.industron.block;

import com.mojang.serialization.MapCodec;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A tiny world-only branch fragment that can be picked up as the matching WoodMaterial Stick. */
public final class FallenStickBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 16 local ground positions (4x4 grid) across the full 16x16 block surface. */
    public static final IntegerProperty POSITION = IntegerProperty.create("position", 0, 15);
    public static final MapCodec<FallenStickBlock> CODEC = simpleCodec(properties ->
            new FallenStickBlock(WoodMaterials.OAK, properties));

    private final WoodMaterial material;

    public FallenStickBlock(WoodMaterial material) {
        this(material, BlockBehaviour.Properties.of()
                .instabreak()
                .noCollission()
                .noOcclusion()
                .sound(SoundType.WOOD));
    }

    private FallenStickBlock(WoodMaterial material, BlockBehaviour.Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POSITION, 0));
    }

    public WoodMaterial material() {
        return material;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POSITION);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = state.getValue(POSITION);
        int baseX = (index & 3) * 4;
        int baseZ = (index >> 2) * 4;
        int x;
        int z;
        switch (state.getValue(FACING)) {
            case EAST -> {
                x = 12 - baseZ;
                z = baseX;
            }
            case SOUTH -> {
                x = 12 - baseX;
                z = 12 - baseZ;
            }
            case WEST -> {
                x = baseZ;
                z = 12 - baseX;
            }
            default -> {
                x = baseX;
                z = baseZ;
            }
        }
        return Block.box(x, 0.0D, z, x + 4.0D, 0.25D, z + 4.0D);
    }

    public BlockState randomState(RandomSource random) {
        Direction[] horizontal = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        return defaultBlockState()
                .setValue(FACING, horizontal[random.nextInt(horizontal.length)])
                .setValue(POSITION, random.nextInt(16));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
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
            Item item = stickItem();
            if (item == null) return InteractionResult.PASS;
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private Item stickItem() {
        if (material.hasExistingPart(MaterialPart.STICK)) {
            return BuiltInRegistries.ITEM.getOptional(material.existingPart(MaterialPart.STICK)).orElse(null);
        }
        var holder = ItemRegistry.getStructureMaterialFormItem(material, MaterialPart.STICK);
        return holder == null ? null : holder.get();
    }
}
