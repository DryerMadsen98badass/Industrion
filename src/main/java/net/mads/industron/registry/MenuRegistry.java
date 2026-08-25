package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.menu.MachineControlScheduleMenu;
import net.mads.industron.menu.MachinePortMenu;
import net.mads.industron.menu.MultiblockControllerMenu;
import net.mads.industron.menu.SingleBlockMachineMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Industron.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachinePortMenu>> MACHINE_PORT =
            MENUS.register("machine_port", () -> IMenuTypeExtension.create(MachinePortMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MultiblockControllerMenu>> MULTIBLOCK_CONTROLLER =
            MENUS.register("multiblock_controller", () -> IMenuTypeExtension.create(MultiblockControllerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SingleBlockMachineMenu>> SINGLE_BLOCK_MACHINE =
            MENUS.register("single_block_machine", () -> IMenuTypeExtension.create(SingleBlockMachineMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineControlScheduleMenu>> MACHINE_CONTROL_SCHEDULE =
            MENUS.register("machine_control_schedule", () -> IMenuTypeExtension.create(MachineControlScheduleMenu::new));

    public static void register(IEventBus modEventBus) { MENUS.register(modEventBus); }
}