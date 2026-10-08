package net.mads.industron.item;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.neoforged.neoforge.network.PacketDistributor;
public final class ClimateInstrumentItem extends Item {
    public ClimateInstrumentItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server)PacketDistributor.sendToPlayer(server,net.mads.industron.network.ClimateStatePayload.snapshot(server,true));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
