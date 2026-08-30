package net.mads.industron.material.defenitions;

import net.mads.industron.material.IndustrialMaterial;

import java.util.List;

import static net.mads.industron.material.defenitions.IndustrialMaterials.*;

/**
 * Reviewed fictional mineral phases used as trace components in StoneMaterial.
 *
 * <p>Every definition owns only DUST. Formula, color, topology, phase, properties and tier are
 * calculated from {@code .contains(...)}. These are not ores by themselves; only OreMaterials
 * definitions own ore blocks or natural deposits.</p>
 */
public final class MineralDustMaterials {
    // Early Overworld mineral family: ULV/LV components only.
    public static final IndustrialMaterial VERNALITE = mineralDust("vernalite", "Vernalite")
            .contains(component(VERNIUM, 2), component(ILYRA, 1)).build();
    public static final IndustrialMaterial DRAXITE = mineralDust("draxite", "Draxite")
            .contains(component(DRAXIL, 2), component(CEVORA, 1)).build();
    public static final IndustrialMaterial ELNARITE = mineralDust("elnarite", "Elnarite")
            .contains(component(ELNARA, 3), component(QORVEN, 1)).build();
    public static final IndustrialMaterial FYRALITE = mineralDust("fyralite", "Fyralite")
            .contains(component(FYRIN, 2), component(TITENITE, 1)).build();
    public static final IndustrialMaterial JORVITE = mineralDust("jorvite", "Jorvite")
            .contains(component(JORVEN, 2), component(YAVREN, 1)).build();
    public static final IndustrialMaterial KAVRITE = mineralDust("kavrite", "Kavrite")
            .contains(component(KAVRA, 1), component(ALVORY, 1)).build();
    public static final IndustrialMaterial LORYXITE = mineralDust("loryxite", "Loryxite")
            .contains(component(LORYX, 2), component(SELYR, 1)).build();
    public static final IndustrialMaterial RELYXITE = mineralDust("relyxite", "Relyxite")
            .contains(component(RELYX, 3), component(URYXEN, 1)).build();
    public static final IndustrialMaterial SENVRAITE = mineralDust("senvraite", "Senvraite")
            .contains(component(SENVRA, 2), component(ILYRA, 1)).build();
    public static final IndustrialMaterial TALYXITE = mineralDust("talyxite", "Talyxite")
            .contains(component(TALYX, 2), component(CEVORA, 1)).build();
    public static final IndustrialMaterial USKARITE = mineralDust("uskarite", "Uskarite")
            .contains(component(USKARA, 1), component(QORVEN, 1)).build();
    public static final IndustrialMaterial VASKYRITE = mineralDust("vaskyrite", "Vaskyrite")
            .contains(component(VASKYR, 2), component(TITENITE, 1)).build();

    // Mid Overworld mineral family: MV/HV-bearing phases.
    public static final IndustrialMaterial WELYRITE = mineralDust("welyrite", "Welyrite")
            .contains(component(WELYRA, 2), component(ILYRA, 1)).build();
    public static final IndustrialMaterial XAVRITE = mineralDust("xavrite", "Xavrite")
            .contains(component(XAVREN, 2), component(QORVEN, 1)).build();
    public static final IndustrialMaterial YSKELITE = mineralDust("yskelite", "Yskelite")
            .contains(component(YSKEL, 2), component(CEVORA, 1)).build();
    public static final IndustrialMaterial ZORIXITE = mineralDust("zorixite", "Zorixite")
            .contains(component(ZORIXA, 3), component(DRAXIL, 1)).build();
    public static final IndustrialMaterial AULVENITE = mineralDust("aulvenite", "Aulvenite")
            .contains(component(AULVEN, 2), component(TITENITE, 1)).build();
    public static final IndustrialMaterial DASKENITE = mineralDust("daskenite", "Daskenite")
            .contains(component(DASKEN, 2), component(NORVAK, 1)).build();
    public static final IndustrialMaterial EVORINITE = mineralDust("evorinite", "Evorinite")
            .contains(component(EVORIN, 2), component(ILYRA, 1)).build();
    public static final IndustrialMaterial FALYXITE = mineralDust("falyxite", "Falyxite")
            .contains(component(FALYX, 2), component(QORVEN, 1)).build();
    public static final IndustrialMaterial GRAVENITE = mineralDust("gravenite", "Gravenite")
            .contains(component(GRAVEN, 2), component(CEVORA, 1)).build();
    public static final IndustrialMaterial HORYXITE = mineralDust("horyxite", "Horyxite")
            .contains(component(HORYX, 2), component(ALVORY, 1)).build();
    public static final IndustrialMaterial CYVERITE = mineralDust("cyverite", "Cyverite")
            .contains(component(CYVERA, 2), component(GAVREL, 1)).build();
    public static final IndustrialMaterial GORVIXITE = mineralDust("gorvixite", "Gorvixite")
            .contains(component(GORVIX, 2), component(WELYRA, 1)).build();
    public static final IndustrialMaterial KELYRITE = mineralDust("kelyrite", "Kelyrite")
            .contains(component(KELYRA, 2), component(XAVREN, 1)).build();
    public static final IndustrialMaterial CROVIXITE = mineralDust("crovixite", "Crovixite")
            .contains(component(CROVIX, 2), component(YAVREN, 1)).build();

    // EV mineral family. These are valid Tuff traces and form the lower Nether mineral band.
    public static final IndustrialMaterial SORYNITE = mineralDust("sorynite", "Sorynite")
            .contains(component(SORYN, 3), component(QORVEN, 2)).build();
    public static final IndustrialMaterial SORYXITE = mineralDust("soryxite", "Soryxite")
            .contains(component(SORYN, 1), component(DRAXIL, 1)).build();
    public static final IndustrialMaterial RASKORITE = mineralDust("raskorite", "Raskorite")
            .contains(component(RASKEL, 5), component(QORVEN, 1), component(DRAXIL, 1)).build();
    public static final IndustrialMaterial NERYNITE = mineralDust("nerynite", "Nerynite")
            .contains(component(NERYN, 5), component(URYXEN, 1), component(DRAXIL, 1)).build();
    public static final IndustrialMaterial MADSIITE = mineralDust("madsiite", "Madsiite")
            .contains(component(MADSIUM, 2), component(QORVEN, 1)).build();
    public static final IndustrialMaterial HAVORITE = mineralDust("havorite", "Havorite")
            .contains(component(HAVOR, 2), component(URYXEN, 1)).build();
    public static final IndustrialMaterial IXRANITE = mineralDust("ixranite", "Ixranite")
            .contains(component(IXRANE, 2), component(DRAXIL, 1)).build();
    public static final IndustrialMaterial JELYXITE = mineralDust("jelyxite", "Jelyxite")
            .contains(component(JELYX, 2), component(TITENITE, 1)).build();
    public static final IndustrialMaterial KORVENITE = mineralDust("korvenite", "Korvenite")
            .contains(component(KORVEN, 2), component(CEVORA, 1)).build();
    public static final IndustrialMaterial TAVORITE = mineralDust("tavorite", "Tavorite")
            .contains(component(TAVORA, 2), component(ILYRA, 1)).build();

    // IV/LuV Nether mineral family, built from the late-game fictional elements.
    public static final IndustrialMaterial AEVRITE = mineralDust("aevrite", "Aevrite")
            .contains(component(AEVRON, 2), component(HAVOR, 1)).build();
    public static final IndustrialMaterial BRALYXITE = mineralDust("bralyxite", "Bralyxite")
            .contains(component(BRALYX, 2), component(SORYN, 1)).build();
    public static final IndustrialMaterial CIRYNITE = mineralDust("cirynite", "Cirynite")
            .contains(component(CIRYNE, 2), component(RASKEL, 1)).build();
    public static final IndustrialMaterial DOVREXITE = mineralDust("dovrexite", "Dovrexite")
            .contains(component(DOVREX, 2), component(NERYN, 1)).build();
    public static final IndustrialMaterial MORYXITE = mineralDust("moryxite", "Moryxite")
            .contains(component(MORYX, 2), component(AEVRON, 1)).build();
    public static final IndustrialMaterial NAXIRITE = mineralDust("naxirite", "Naxirite")
            .contains(component(NAXIRA, 2), component(BRALYX, 1)).build();
    public static final IndustrialMaterial OVELYNITE = mineralDust("ovelynite", "Ovelynite")
            .contains(component(OVELYN, 2), component(CIRYNE, 1)).build();
    public static final IndustrialMaterial PRYVENITE = mineralDust("pryvenite", "Pryvenite")
            .contains(component(PRYVEN, 2), component(DOVREX, 1)).build();
    public static final IndustrialMaterial QYXARITE = mineralDust("qyxarite", "Qyxarite")
            .contains(component(QYXARA, 2), component(ELARIX, 1)).build();
    public static final IndustrialMaterial RHELIXITE = mineralDust("rhelixite", "Rhelixite")
            .contains(component(RHELIX, 2), component(GRYVEN, 1)).build();
    public static final IndustrialMaterial SYVRENITE = mineralDust("syvrenite", "Syvrenite")
            .contains(component(SYVREN, 2), component(HELYX, 1)).build();
    public static final IndustrialMaterial TYRAXITE = mineralDust("tyraxite", "Tyraxite")
            .contains(component(TYRAX, 2), component(IOVRAX, 1)).build();

    // End mineral family. Every phase contains at least one ZPM+ element.
    public static final IndustrialMaterial YRYXITE = mineralDust("yryxite", "Yryxite")
            .contains(component(YRYXEN, 2), component(ZORVANE, 1)).build();
    public static final IndustrialMaterial ZORVANITE = mineralDust("zorvanite", "Zorvanite")
            .contains(component(ZORVANE, 2), component(AXYRON, 1)).build();
    public static final IndustrialMaterial AXYRITE = mineralDust("axyrite", "Axyrite")
            .contains(component(AXYRON, 2), component(BELVIX, 1)).build();
    public static final IndustrialMaterial BELVIXITE = mineralDust("belvixite", "Belvixite")
            .contains(component(BELVIX, 2), component(CORTHEN, 1)).build();
    public static final IndustrialMaterial GALYTHITE = mineralDust("galythite", "Galythite")
            .contains(component(GALYTH, 2), component(HORYVEN, 1)).build();
    public static final IndustrialMaterial HORYVENITE = mineralDust("horyvenite", "Horyvenite")
            .contains(component(HORYVEN, 2), component(ILYXAR, 1)).build();
    public static final IndustrialMaterial MYRITHITE = mineralDust("myrithite", "Myrithite")
            .contains(component(MYRITH, 2), component(NUVEXA, 1)).build();
    public static final IndustrialMaterial NUVEXITE = mineralDust("nuvexite", "Nuvexite")
            .contains(component(NUVEXA, 2), component(ORYND, 1)).build();
    public static final IndustrialMaterial QEVORITE = mineralDust("qevorite", "Qevorite")
            .contains(component(QEVORA, 2), component(RUXEN, 1)).build();
    public static final IndustrialMaterial TERYXITE = mineralDust("teryxite", "Teryxite")
            .contains(component(TERYX, 2), component(UVARYA, 1)).build();
    public static final IndustrialMaterial VEXALITE = mineralDust("vexalite", "Vexalite")
            .contains(component(VEXALON, 2), component(WYRALIS, 1)).build();
    public static final IndustrialMaterial XYTHERITE = mineralDust("xytherite", "Xytherite")
            .contains(component(XYTHER, 2), component(VEXALON, 1)).build();

    public static final List<IndustrialMaterial> ALL = List.of(
            VERNALITE, DRAXITE, ELNARITE, FYRALITE, JORVITE, KAVRITE, LORYXITE, RELYXITE, SENVRAITE, TALYXITE, USKARITE, VASKYRITE,
            WELYRITE, XAVRITE, YSKELITE, ZORIXITE, AULVENITE, DASKENITE, EVORINITE, FALYXITE, GRAVENITE, HORYXITE, CYVERITE, GORVIXITE, KELYRITE, CROVIXITE,
            SORYNITE, SORYXITE, RASKORITE, NERYNITE, MADSIITE, HAVORITE, IXRANITE, JELYXITE, KORVENITE, TAVORITE,
            AEVRITE, BRALYXITE, CIRYNITE, DOVREXITE, MORYXITE, NAXIRITE, OVELYNITE, PRYVENITE, QYXARITE, RHELIXITE, SYVRENITE, TYRAXITE,
            YRYXITE, ZORVANITE, AXYRITE, BELVIXITE, GALYTHITE, HORYVENITE, MYRITHITE, NUVEXITE, QEVORITE, TERYXITE, VEXALITE, XYTHERITE
    );

    private MineralDustMaterials() {
    }

    public static void init() {
        // Intentionally empty: touching this class initializes its static material definitions.
    }
}
