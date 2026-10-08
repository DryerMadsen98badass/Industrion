package net.mads.industron.block.coils;

import net.mads.industron.machine.MachineTier;

import java.util.List;

/** Heating coils are declared explicitly; machine tiers never generate coil blocks. */
public final class CoilDefinitions {
    private static final String OFF_TEXTURE = "block/coils/coil_off";
    private static final String ON_TEXTURE = "block/coils/coil_on";
    private static final String FRAME_TEXTURE = "block/coils/coil_frame";

    public static final CoilDefinition ISKARIUM = CoilDefinition.coil("electric_iskarium")
            .displayName("Electric Iskarium Coil")
            .temperature(500)
            .tier(MachineTier.ULV)
            .offTexture(OFF_TEXTURE)
            .color(0xB8734A)
            .onTexture(ON_TEXTURE)
            .color(0xB8734A)
            .frameTexture(FRAME_TEXTURE)
            .build();

    public static final CoilDefinition JUBREX = CoilDefinition.coil("electric_jubrex")
            .displayName("Electric Jubrex Coil")
            .temperature(1_000)
            .tier(MachineTier.LV)
            .offTexture(OFF_TEXTURE)
            .color(0x9A836F)
            .onTexture(ON_TEXTURE)
            .color(0x9A836F)
            .frameTexture(FRAME_TEXTURE)
            .build();

    public static final CoilDefinition HORDELYRA = CoilDefinition.coil("electric_hordelyra")
            .displayName("Electric Hordelyra Coil")
            .temperature(1_500)
            .tier(MachineTier.MV)
            .offTexture(OFF_TEXTURE)
            .color(0x6F7783)
            .onTexture(ON_TEXTURE)
            .color(0x6F7783)
            .frameTexture(FRAME_TEXTURE)
            .build();

    public static final CoilDefinition MAVLOX = CoilDefinition.coil("electric_mavlox")
            .displayName("Electric Mavlox Coil")
            .temperature(2_000)
            .tier(MachineTier.HV)
            .offTexture(OFF_TEXTURE)
            .color(0x555B66)
            .onTexture(ON_TEXTURE)
            .color(0x555B66)
            .frameTexture(FRAME_TEXTURE)
            .build();

    public static final CoilDefinition NERYKON = CoilDefinition.coil("electric_nerykon")
            .displayName("Electric Nerykon Coil")
            .temperature(2_500)
            .tier(MachineTier.EV)
            .offTexture(OFF_TEXTURE)
            .color(0x343942)
            .onTexture(ON_TEXTURE)
            .color(0x343942)
            .frameTexture(FRAME_TEXTURE)
            .build();

    public static final List<CoilDefinition> ALL = List.of(
            ISKARIUM,
            JUBREX,
            HORDELYRA,
            MAVLOX,
            NERYKON
    );

    public static final CoilDefinition PLACEHOLDER = ISKARIUM;

    private CoilDefinitions() {
    }
}