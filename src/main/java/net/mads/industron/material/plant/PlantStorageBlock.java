package net.mads.industron.material.plant;

import com.mojang.serialization.MapCodec;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.ArrayList;
import java.util.List;

/** Count lives in blockstate: save/load, pistons and explosions keep the stored quantity. */
public final class PlantStorageBlock extends Block {
    public static final IntegerProperty COUNT=IntegerProperty.create("count",1,PlantStorage.CAPACITY);
    private static final VoxelShape[] SHAPES=new VoxelShape[8];
    static {for(int i=0;i<8;i++)SHAPES[i]=Block.box(0,0,0,16,(i+1)*2,16);}
    public static final MapCodec<PlantStorageBlock> CODEC=simpleCodec(p -> new PlantStorageBlock(PlantMaterials.WHEAT,p));
    private final PlantMaterial material;
    public PlantStorageBlock(PlantMaterial material) {
        this(material,BlockBehaviour.Properties.of().strength(.25F).sound(SoundType.GRASS).noOcclusion());
    }
    private PlantStorageBlock(PlantMaterial material,BlockBehaviour.Properties properties) {
        super(properties);this.material=material;
        registerDefaultState(stateDefinition.any().setValue(COUNT,1));
    }
    public PlantMaterial material(){return material;}
    public PlantPart part(){return PlantPart.STORAGE_BLOCK;}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(COUNT);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[(s.getValue(COUNT)-1)/8];}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos other) {
        return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,neighbor,l,p,other);
    }
    public ItemStack plantItem(){return new ItemStack(BuiltInRegistries.ITEM.get(material.storageItem().orElseThrow()));}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder context) {
        ItemStack source=plantItem();int count=state.getValue(COUNT);var drops=new ArrayList<ItemStack>();
        while(count>0){int amount=Math.min(count,source.getMaxStackSize());drops.add(source.copyWithCount(amount));count-=amount;}
        return drops;
    }
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state) {return plantItem();}
}
