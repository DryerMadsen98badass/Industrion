package net.mads.industron.tool;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class EquipmentLootModifier extends LootModifier {
    public static final MapCodec<EquipmentLootModifier> CODEC=RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance,EquipmentLootModifier::new));
    public EquipmentLootModifier(LootItemCondition[] conditions){super(conditions);}
    @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        boolean nether = net.mads.industron.progression.ProgressionMaterials.isNether(context.getLevel());
        ObjectArrayList<ItemStack> replaced = new ObjectArrayList<>();
        for (ItemStack stack : loot) {
            ItemStack result = net.mads.industron.progression.ProgressionMaterials.replace(stack, nether);
            result = EquipmentReplacement.replace(result, nether);
            int remaining = result.getCount();
            while (remaining > 0) {
                int count = Math.min(remaining, result.getMaxStackSize());
                replaced.add(result.copyWithCount(count));
                remaining -= count;
            }
        }
        return replaced;
    }
}
