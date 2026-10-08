package net.mads.industron.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.mads.industron.Industron;
import net.mads.industron.worldgen.GeologyDepositFeature;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Admin/debug commands for validating deterministic geology without scanning blocks. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class GeologyCommands {
    private static final int DEFAULT_SEARCH_REGION_RADIUS = 64;

    private GeologyCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("industron")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("geology")
                                .then(Commands.literal("locate")
                                        .then(Commands.argument("ore", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        GeologyDepositFeature.oreIds(), builder
                                                ))
                                                .executes(context -> locate(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "ore")
                                                )))))
        );
    }

    private static int locate(net.minecraft.commands.CommandSourceStack source, String oreId) {
        BlockPos origin = BlockPos.containing(source.getPosition());
        GeologyDepositFeature.LocatedDeposit result = GeologyDepositFeature.locateNearest(
                source.getLevel(), origin, oreId, DEFAULT_SEARCH_REGION_RADIUS
        );
        if (result == null) {
            source.sendFailure(Component.literal(
                    "No planned geology deposit containing '" + oreId + "' was found within "
                            + DEFAULT_SEARCH_REGION_RADIUS + " geology regions."
            ));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Nearest planned " + oreId + " deposit: "
                        + result.x() + " " + result.y() + " " + result.z()
                        + " (~" + Math.round(result.distanceBlocks()) + " blocks), geometry="
                        + result.geometry().name().toLowerCase(java.util.Locale.ROOT)
                        + ", primary=" + result.primaryMaterialId()
        ), false);
        return 1;
    }
}
