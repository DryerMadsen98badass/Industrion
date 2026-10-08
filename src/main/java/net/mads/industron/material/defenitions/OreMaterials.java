package net.mads.industron.material.defenitions;
import net.mads.industron.material.IndustrialMaterial;
import static net.mads.industron.material.defenitions.IndustrialMaterials.*;
/**
 * Reviewed named ore minerals generated from the latest ore-source report.
 *
 * <p>Only the name/ID and reviewed {@code .contains(...)} are defined here.
 * Tier, dimension, Y range, biome affinity, compatible StoneMaterial hosts,
 * deposit geometry, grade, rarity, ore blocks, processing forms and loot are
 * derived by the existing material/geology systems. No vanilla breaking tier
 * is assigned here.</p>
 *
 * <p>The current report contains 123 solid ore candidates. Gas/fluid natural
 * sources are deliberately not defined in this class.</p>
 */
public final class OreMaterials {
    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Vernium + Ilyra; preview LV / HYDROTHERMAL. */
    public static final IndustrialMaterial VERNITE = ore("vernite", "Vernite")
            .contains(component(VERNIUM, 3), component(ILYRA, 1)).build();

    /** Source: Avarn + Arvexa; preview LV / MAGMATIC. */
    public static final IndustrialMaterial AVARNITE = ore("avarnite", "Avarnite")
            .contains(component(AVARN, 1), component(ARVEXA, 1)).build();

    /** Source: Brelix + Jorven; preview LV / HYDROTHERMAL. */
    public static final IndustrialMaterial BRELIXITE = ore("brelixite", "Brelixite")
            .contains(component(BRELIX, 1), component(JORVEN, 1)).build();

    /** Source: Quartz + Ilyra; preview LV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial QUARTZALITE = ore("quartzalite", "Quartzalite")
            .contains(component(QUARTZ, 1), component(ILYRA, 1)).build();

    /** Source: Redstone + Jorven; preview ULV / MAGMATIC. */
    public static final IndustrialMaterial REDSTONITE = ore("redstonite", "Redstonite")
            .contains(component(REDSTONE, 1), component(JORVEN, 2)).build();

    /** Source: Gavrel + Cyvera; preview MV / MAGMATIC. */
    public static final IndustrialMaterial GAVRELITE = ore("gavrelite", "Gavrelite")
            .contains(component(GAVREL, 6), component(CYVERA, 7)).build();

    /** Source: Heskor + Zelyth; preview LV / MAGMATIC. */
    public static final IndustrialMaterial HESKORITE = ore("heskorite", "Heskorite")
            .contains(component(HESKOR, 1), component(ZELYTH, 1)).build();

    /** Source: Lapis + Yavren; preview LV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial LAZURITE = ore("lazurite", "Lazurite")
            .contains(component(LAPIS, 1), component(YAVREN, 1)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Diamond + Ilyra; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial DIAMORITE = ore("diamorite", "Diamorite")
            .contains(component(DIAMOND, 3), component(ILYRA, 4)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Mydren + Senvra; preview LV / MAGMATIC. */
    public static final IndustrialMaterial MYDRENITE = ore("mydrenite", "Mydrenite")
            .contains(component(MYDREN, 1), component(SENVRA, 1)).build();

    /** Source: Norvak + Dasken; preview MV / MAGMATIC. */
    public static final IndustrialMaterial NORVAKITE = ore("norvakite", "Norvakite")
            .contains(component(NORVAK, 1), component(DASKEN, 1)).build();

    /** Source: Orlune + Yavren; preview HV / SKARN_LIKE. */
    public static final IndustrialMaterial ORLUNITE = ore("orlunite", "Orlunite")
            .contains(component(ORLUNE, 1), component(YAVREN, 1)).build();

    /** Source: Praxel + Qorven; preview HV / SKARN_LIKE. */
    public static final IndustrialMaterial PRAXELITE = ore("praxelite", "Praxelite")
            .contains(component(PRAXEL, 3), component(QORVEN, 4)).build();

    /** Source: Qevrin + Qorven; preview HV / SKARN_LIKE. */
    public static final IndustrialMaterial QEVRINITE = ore("qevrinite", "Qevrinite")
            .contains(component(QEVRIN, 3), component(QORVEN, 4)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Raskel + Qorven; preview EV / BANDED. */
    public static final IndustrialMaterial RASKELITE = ore("raskelite", "Raskelite")
            .contains(component(RASKEL, 3), component(QORVEN, 1)).build();

    /** Source: Madsium + Qorven; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial MADSITE = ore("madsite", "Madsite")
            .contains(component(MADSIUM, 3), component(QORVEN, 2)).build();

    /** Source: Soryn + Qorven; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial SORYNALITE = ore("sorynalite", "Sorynalite")
            .contains(component(SORYN, 3), component(QORVEN, 2)).build();

    /** Source: Tavrix + Qorven; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial TAVRIXITE = ore("tavrixite", "Tavrixite")
            .contains(component(TAVRIX, 3), component(QORVEN, 2)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Uvren + Qorven; preview HV / VEIN. */
    public static final IndustrialMaterial UVRENITE = ore("uvrenite", "Uvrenite")
            .contains(component(UVREN, 3), component(QORVEN, 2)).build();

    /** Source: Velyx + Vaskyr; preview HV / MAGMATIC. */
    public static final IndustrialMaterial VELYXITE = ore("velyxite", "Velyxite")
            .contains(component(VELYX, 2), component(VASKYR, 1)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Xorim + Qorven; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial XORIMITE = ore("xorimite", "Xorimite")
            .contains(component(XORIM, 1), component(QORVEN, 1)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Netherite + Yavren; preview MV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial NETHERICITE = ore("nethericite", "Nethericite")
            .contains(component(NETHERITE, 3), component(YAVREN, 4)).build();

    /** Source: Caskel + Woryn; preview ULV / MAGMATIC. */
    public static final IndustrialMaterial CASKELITE = ore("caskelite", "Caskelite")
            .contains(component(CASKEL, 1), component(WORYN, 1)).build();

    /** Source: Denvra + Relyx; preview LV / MAGMATIC. */
    public static final IndustrialMaterial DENVRITE = ore("denvrite", "Denvrite")
            .contains(component(DENVRA, 1), component(RELYX, 1)).build();

    /** Source: Eryxon + Qorven; preview HV / SKARN_LIKE. */
    public static final IndustrialMaterial ERYXONITE = ore("eryxonite", "Eryxonite")
            .contains(component(ERYXON, 1), component(QORVEN, 1)).build();

    /** Source: Falune + Qorven; preview HV / MAGMATIC. */
    public static final IndustrialMaterial FALUNITE = ore("falunite", "Falunite")
            .contains(component(FALUNE, 3), component(QORVEN, 4)).build();

    /** Source: Greska + Qorven; preview HV / MAGMATIC. */
    public static final IndustrialMaterial GRESKITE = ore("greskite", "Greskite")
            .contains(component(GRESKA, 3), component(QORVEN, 4)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Havor + Uryxen; preview EV / BANDED. */
    public static final IndustrialMaterial HAVORENE = ore("havorene", "Havorene")
            .contains(component(HAVOR, 3), component(URYXEN, 1)).build();

    /** Source: Ixrane + Draxil; preview EV / HYDROTHERMAL. */
    public static final IndustrialMaterial IXRALITE = ore("ixralite", "Ixralite")
            .contains(component(IXRANE, 1), component(DRAXIL, 1)).build();

    /** Source: Jelyx + Uryxen; preview EV / MAGMATIC. */
    public static final IndustrialMaterial JELYXENE = ore("jelyxene", "Jelyxene")
            .contains(component(JELYX, 3), component(URYXEN, 2)).build();

    /** Source: Korven + Cevora; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial KORVALITE = ore("korvalite", "Korvalite")
            .contains(component(KORVEN, 3), component(CEVORA, 2)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Laskyr + Uryxen; preview HV / MAGMATIC. */
    public static final IndustrialMaterial LASKYRITE = ore("laskyrite", "Laskyrite")
            .contains(component(LASKYR, 3), component(URYXEN, 2)).build();

    /** Source: Mervane + Vaskyr; preview HV / MAGMATIC. */
    public static final IndustrialMaterial MERVANITE = ore("mervanite", "Mervanite")
            .contains(component(MERVANE, 2), component(VASKYR, 1)).build();

    /** Source: Orvex + Qorven; preview HV / MAGMATIC. */
    public static final IndustrialMaterial ORVEXITE = ore("orvexite", "Orvexite")
            .contains(component(ORVEX, 1), component(QORVEN, 1)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Pyralis + Qorven; preview EV / MAGMATIC. */
    public static final IndustrialMaterial PYRALISITE = ore("pyralisite", "Pyralisite")
            .contains(component(PYRALIS, 3), component(QORVEN, 4)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Uskara + Qorven; preview LV / HYDROTHERMAL. */
    public static final IndustrialMaterial USKALINE = ore("uskaline", "Uskaline")
            .contains(component(USKARA, 3), component(QORVEN, 1)).build();

    /** Source: Voryn + Vaskyr; preview LV / MAGMATIC. */
    public static final IndustrialMaterial VORYNITE = ore("vorynite", "Vorynite")
            .contains(component(VORYN, 1), component(VASKYR, 1)).build();

    /** Source: Welyra + Ilyra; preview MV / SKARN_LIKE. */
    public static final IndustrialMaterial WELYRINE = ore("welyrine", "Welyrine")
            .contains(component(WELYRA, 3), component(ILYRA, 2)).build();

    /** Source: Xavren + Qorven; preview MV / MAGMATIC. */
    public static final IndustrialMaterial XAVRENITE = ore("xavrenite", "Xavrenite")
            .contains(component(XAVREN, 3), component(QORVEN, 2)).build();

    /** Source: Yskel + Cevora; preview MV / SKARN_LIKE. */
    public static final IndustrialMaterial YSKARINE = ore("yskarine", "Yskarine")
            .contains(component(YSKEL, 3), component(CEVORA, 2)).build();

    /** Source: Zorixa + Draxil; preview MV / MAGMATIC. */
    public static final IndustrialMaterial ZORIXENE = ore("zorixene", "Zorixene")
            .contains(component(ZORIXA, 1), component(DRAXIL, 1)).build();

    /** Source: Aulven + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial AULVARITE = ore("aulvarite", "Aulvarite")
            .contains(component(AULVEN, 1), component(VASKYR, 1)).build();

    /** Source: Breyra + Uryxen; preview HV / BANDED. */
    public static final IndustrialMaterial BREYRITE = ore("breyrite", "Breyrite")
            .contains(component(BREYRA, 3), component(URYXEN, 1)).build();

    /** Source: Crovix + Uryxen; preview HV / BANDED. */
    public static final IndustrialMaterial CROVIALITE = ore("crovialite", "Crovialite")
            .contains(component(CROVIX, 3), component(URYXEN, 2)).build();

    /** Source: Dasken + Norvak; preview MV / MAGMATIC. */
    public static final IndustrialMaterial DASKORITE = ore("daskorite", "Daskorite")
            .contains(component(DASKEN, 1), component(NORVAK, 1)).build();

    /** Source: Evorin + Ilyra; preview MV / BANDED. */
    public static final IndustrialMaterial EVORALITE = ore("evoralite", "Evoralite")
            .contains(component(EVORIN, 3), component(ILYRA, 2)).build();

    /** Source: Falyx + Qorven; preview MV / BANDED. */
    public static final IndustrialMaterial FALYXENE = ore("falyxene", "Falyxene")
            .contains(component(FALYX, 3), component(QORVEN, 2)).build();

    /** Source: Graven + Cevora; preview MV / SKARN_LIKE. */
    public static final IndustrialMaterial GRAVELLITE = ore("gravellite", "Gravellite")
            .contains(component(GRAVEN, 3), component(CEVORA, 2)).build();

    /** Source: Horyx + Alvory; preview MV / MAGMATIC. */
    public static final IndustrialMaterial HORYLINE = ore("horyline", "Horyline")
            .contains(component(HORYX, 1), component(ALVORY, 1)).build();

    /** Source: Iskara + Woryn; preview ULV / MAGMATIC. */
    public static final IndustrialMaterial ISKARITE = ore("iskarite", "Iskarite")
            .contains(component(ISKARA, 1), component(WORYN, 1)).build();

    /** Source: Juvren + Vaskyr; preview LV / MAGMATIC. */
    public static final IndustrialMaterial JUVRENITE = ore("juvrenite", "Juvrenite")
            .contains(component(JUVREN, 1), component(VASKYR, 1)).build();

    /** Source: Kelyra + Xavren; preview HV / MAGMATIC. */
    public static final IndustrialMaterial KELYRORITE = ore("kelyrorite", "Kelyrorite")
            .contains(component(KELYRA, 1), component(XAVREN, 1)).build();

    /** Source: Loxen + Uryxen; preview HV / MAGMATIC. */
    public static final IndustrialMaterial LOXENITE = ore("loxenite", "Loxenite")
            .contains(component(LOXEN, 3), component(URYXEN, 4)).build();

    /** Source: Mavrix + Uryxen; preview HV / MAGMATIC. */
    public static final IndustrialMaterial MAVRIXITE = ore("mavrixite", "Mavrixite")
            .contains(component(MAVRIX, 3), component(URYXEN, 4)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Neryn + Uryxen; preview EV / BANDED. */
    public static final IndustrialMaterial NERYLITE = ore("nerylite", "Nerylite")
            .contains(component(NERYN, 3), component(URYXEN, 1)).build();

    /** Source: Haakonium + Uryxen; preview EV / MAGMATIC. */
    public static final IndustrialMaterial HAAKONITE = ore("haakonite", "Haakonite")
            .contains(component(HAAKONIUM, 3), component(URYXEN, 2)).build();

    /** Source: Oskara + Uryxen; preview EV / MAGMATIC. */
    public static final IndustrialMaterial OSKARITE = ore("oskarite", "Oskarite")
            .contains(component(OSKARA, 3), component(URYXEN, 2)).build();

    /** Source: Pervix + Uryxen; preview EV / MAGMATIC. */
    public static final IndustrialMaterial PERVIXITE = ore("pervixite", "Pervixite")
            .contains(component(PERVIX, 3), component(URYXEN, 2)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Qalyx + Uryxen; preview HV / MAGMATIC. */
    public static final IndustrialMaterial QALYXITE = ore("qalyxite", "Qalyxite")
            .contains(component(QALYX, 3), component(URYXEN, 2)).build();

    /** Source: Rovena + Vaskyr; preview HV / MAGMATIC. */
    public static final IndustrialMaterial ROVENITE = ore("rovenite", "Rovenite")
            .contains(component(ROVENA, 2), component(VASKYR, 1)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Tavora + Ilyra; preview EV / PEGMATITE_LIKE. */
    public static final IndustrialMaterial TAVORINE = ore("tavorine", "Tavorine")
            .contains(component(TAVORA, 1), component(ILYRA, 1)).build();

    /** Source: Emerald + Uryxen; preview EV / MAGMATIC. */
    public static final IndustrialMaterial EMERALDITE = ore("emeraldite", "Emeraldite")
            .contains(component(EMERALD, 3), component(URYXEN, 4)).build();

    // Overworld ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Yavrix + Woryn; preview ULV / MAGMATIC. */
    public static final IndustrialMaterial YAVRIXITE = ore("yavrixite", "Yavrixite")
            .contains(component(YAVRIX, 1), component(WORYN, 1)).build();

    /** Source: Zeskal + Vaskyr; preview LV / MAGMATIC. */
    public static final IndustrialMaterial ZESKALITE = ore("zeskalite", "Zeskalite")
            .contains(component(ZESKAL, 1), component(VASKYR, 1)).build();

    /** Source: Alvory + Kavra; preview LV / MAGMATIC. */
    public static final IndustrialMaterial ALVORITE = ore("alvorite", "Alvorite")
            .contains(component(ALVORY, 1), component(KAVRA, 2)).build();

    /** Source: Brinox + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial BRINOXITE = ore("brinoxite", "Brinoxite")
            .contains(component(BRINOX, 1), component(VASKYR, 1)).build();

    /** Source: Cyvera + Gavrel; preview MV / MAGMATIC. */
    public static final IndustrialMaterial CYVERALITE = ore("cyveralite", "Cyveralite")
            .contains(component(CYVERA, 7), component(GAVREL, 6)).build();

    /** Source: Doryn + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial DORYNITE = ore("dorynite", "Dorynite")
            .contains(component(DORYN, 1), component(VASKYR, 1)).build();

    /** Source: Eryva + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial ERYVITE = ore("eryvite", "Eryvite")
            .contains(component(ERYVA, 1), component(VASKYR, 1)).build();

    /** Source: Faskel + Uryxen; preview MV / BANDED. */
    public static final IndustrialMaterial FASKELITE = ore("faskelite", "Faskelite")
            .contains(component(FASKEL, 3), component(URYXEN, 1)).build();

    /** Source: Gorvix + Welyra; preview HV / MAGMATIC. */
    public static final IndustrialMaterial GORVIALITE = ore("gorvialite", "Gorvialite")
            .contains(component(GORVIX, 1), component(WELYRA, 1)).build();

    /** Source: Helyra + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial HELYRITE = ore("helyrite", "Helyrite")
            .contains(component(HELYRA, 1), component(VASKYR, 1)).build();

    /** Source: Ivara + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial IVARITE = ore("ivarite", "Ivarite")
            .contains(component(IVARA, 1), component(VASKYR, 1)).build();

    /** Source: Jexon + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial JEXONITE = ore("jexonite", "Jexonite")
            .contains(component(JEXON, 1), component(VASKYR, 1)).build();

    /** Source: Koryx + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial KORYXITE = ore("koryxite", "Koryxite")
            .contains(component(KORYX, 1), component(VASKYR, 1)).build();

    /** Source: Luvren + Vaskyr; preview MV / MAGMATIC. */
    public static final IndustrialMaterial LUVRENITE = ore("luvrenite", "Luvrenite")
            .contains(component(LUVREN, 1), component(VASKYR, 1)).build();

    // Nether ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Aevron + Havor; preview IV / VEIN. */
    public static final IndustrialMaterial AEVRONITE = ore("aevronite", "Aevronite")
            .contains(component(AEVRON, 1), component(HAVOR, 1)).build();

    /** Source: Bralyx + Soryn; preview IV / VEIN. */
    public static final IndustrialMaterial BRALYXENE = ore("bralyxene", "Bralyxene")
            .contains(component(BRALYX, 1), component(SORYN, 1)).build();

    /** Source: Ciryne + Ovelyn; preview L / VEIN. */
    public static final IndustrialMaterial CIRYALITE = ore("ciryalite", "Ciryalite")
            .contains(component(CIRYNE, 1), component(OVELYN, 1)).build();

    /** Source: Dovrex + Pryven; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial DOVRALITE = ore("dovralite", "Dovralite")
            .contains(component(DOVREX, 1), component(PRYVEN, 2)).build();

    /** Source: Elarix + Qyxara; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial ELARIXITE = ore("elarixite", "Elarixite")
            .contains(component(ELARIX, 1), component(QYXARA, 4)).build();

    /** Source: Faryon + Pryven; preview L / PEGMATITE_LIKE. */
    public static final IndustrialMaterial FARYONITE = ore("faryonite", "Faryonite")
            .contains(component(FARYON, 2), component(PRYVEN, 1)).build();

    /** Source: Gryven + Pryven; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial GRYVENITE = ore("gryvenite", "Gryvenite")
            .contains(component(GRYVEN, 1), component(PRYVEN, 1)).build();

    /** Source: Helyx + Syvren; preview L / MAGMATIC. */
    public static final IndustrialMaterial HELYXITE = ore("helyxite", "Helyxite")
            .contains(component(HELYX, 7), component(SYVREN, 6)).build();

    /** Source: Iovrax + Tyrax; preview L / MAGMATIC. */
    public static final IndustrialMaterial IOVRAXITE = ore("iovraxite", "Iovraxite")
            .contains(component(IOVRAX, 1), component(TYRAX, 1)).build();

    /** Source: Jaryne + Pryven; preview L / VEIN. */
    public static final IndustrialMaterial JARYNITE = ore("jarynite", "Jarynite")
            .contains(component(JARYNE, 1), component(PRYVEN, 1)).build();

    /** Source: Kexara + Pryven; preview L / VEIN. */
    public static final IndustrialMaterial KEXARITE = ore("kexarite", "Kexarite")
            .contains(component(KEXARA, 2), component(PRYVEN, 1)).build();

    /** Source: Lyrven + Pryven; preview L / VEIN. */
    public static final IndustrialMaterial LYRVENITE = ore("lyrvenite", "Lyrvenite")
            .contains(component(LYRVEN, 1), component(PRYVEN, 1)).build();

    /** Source: Moryx + Aevron; preview L / VEIN. */
    public static final IndustrialMaterial MORYLINE = ore("moryline", "Moryline")
            .contains(component(MORYX, 7), component(AEVRON, 5)).build();

    /** Source: Naxira + Bralyx; preview L / PEGMATITE_LIKE. */
    public static final IndustrialMaterial NAXORITE = ore("naxorite", "Naxorite")
            .contains(component(NAXIRA, 3), component(BRALYX, 2)).build();

    /** Source: Ovelyn + Ciryne; preview L / VEIN. */
    public static final IndustrialMaterial OVELARITE = ore("ovelarite", "Ovelarite")
            .contains(component(OVELYN, 1), component(CIRYNE, 1)).build();

    /** Source: Syvren + Helyx; preview L / MAGMATIC. */
    public static final IndustrialMaterial SYVRALITE = ore("syvralite", "Syvralite")
            .contains(component(SYVREN, 6), component(HELYX, 7)).build();

    /** Source: Tyrax + Iovrax; preview L / MAGMATIC. */
    public static final IndustrialMaterial TYRALINE = ore("tyraline", "Tyraline")
            .contains(component(TYRAX, 1), component(IOVRAX, 1)).build();

    /** Source: Ulexar + Pryven; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial ULEXARITE = ore("ulexarite", "Ulexarite")
            .contains(component(ULEXAR, 1), component(PRYVEN, 1)).build();

    /** Source: Voryxa + Pryven; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial VORYXITE = ore("voryxite", "Voryxite")
            .contains(component(VORYXA, 1), component(PRYVEN, 1)).build();

    /** Source: Wyreon + Pryven; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial WYREONITE = ore("wyreonite", "Wyreonite")
            .contains(component(WYREON, 1), component(PRYVEN, 1)).build();

    /** Source: Xandrel + Pryven; preview L / HYDROTHERMAL. */
    public static final IndustrialMaterial XANDRELITE = ore("xandrelite", "Xandrelite")
            .contains(component(XANDREL, 1), component(PRYVEN, 1)).build();

    // End ore minerals. Projected dimension is derived from the strongest component tier.
    /** Source: Yryxen + Zorvane; preview ZPM / BANDED. */
    public static final IndustrialMaterial YRYXENITE = ore("yryxenite", "Yryxenite")
            .contains(component(YRYXEN, 1), component(ZORVANE, 1)).build();

    /** Source: Zorvane + Axyron; preview ZPM / BANDED. */
    public static final IndustrialMaterial ZORVANORITE = ore("zorvanorite", "Zorvanorite")
            .contains(component(ZORVANE, 1), component(AXYRON, 1)).build();

    /** Source: Axyron + Zorvane; preview ZPM / BANDED. */
    public static final IndustrialMaterial AXYRONITE = ore("axyronite", "Axyronite")
            .contains(component(AXYRON, 1), component(ZORVANE, 1)).build();

    /** Source: Belvix + Corthen; preview ZPM / BANDED. */
    public static final IndustrialMaterial BELVARITE = ore("belvarite", "Belvarite")
            .contains(component(BELVIX, 6), component(CORTHEN, 7)).build();

    /** Source: Corthen + Belvix; preview ZPM / BANDED. */
    public static final IndustrialMaterial CORTHENITE = ore("corthenite", "Corthenite")
            .contains(component(CORTHEN, 7), component(BELVIX, 6)).build();

    /** Source: Drelyx + Pryven; preview ZPM / HYDROTHERMAL. */
    public static final IndustrialMaterial DRELYXITE = ore("drelyxite", "Drelyxite")
            .contains(component(DRELYX, 1), component(PRYVEN, 1)).build();

    /** Source: Eryndra + Pryven; preview ZPM / HYDROTHERMAL. */
    public static final IndustrialMaterial ERYNDRITE = ore("eryndrite", "Eryndrite")
            .contains(component(ERYNDRA, 1), component(PRYVEN, 1)).build();

    /** Source: Fyraxon + Pryven; preview ZPM / HYDROTHERMAL. */
    public static final IndustrialMaterial FYRAXONITE = ore("fyraxonite", "Fyraxonite")
            .contains(component(FYRAXON, 1), component(PRYVEN, 1)).build();

    /** Source: Galyth + Horyven; preview UV / BANDED. */
    public static final IndustrialMaterial GALYRINE = ore("galyrine", "Galyrine")
            .contains(component(GALYTH, 1), component(HORYVEN, 1)).build();

    /** Source: Horyven + Galyth; preview UV / BANDED. */
    public static final IndustrialMaterial HORYVALITE = ore("horyvalite", "Horyvalite")
            .contains(component(HORYVEN, 1), component(GALYTH, 1)).build();

    /** Source: Ilyxar + Pryven; preview UV / HYDROTHERMAL. */
    public static final IndustrialMaterial ILYXARITE = ore("ilyxarite", "Ilyxarite")
            .contains(component(ILYXAR, 1), component(PRYVEN, 1)).build();

    /** Source: Jorath + Pryven; preview UV / HYDROTHERMAL. */
    public static final IndustrialMaterial JORATHITE = ore("jorathite", "Jorathite")
            .contains(component(JORATH, 1), component(PRYVEN, 1)).build();

    /** Source: Kyvren + Qyxara; preview UV / HYDROTHERMAL. */
    public static final IndustrialMaterial KYVRENITE = ore("kyvrenite", "Kyvrenite")
            .contains(component(KYVREN, 1), component(QYXARA, 1)).build();

    /** Source: Loxara + Pryven; preview UV / HYDROTHERMAL. */
    public static final IndustrialMaterial LOXARITE = ore("loxarite", "Loxarite")
            .contains(component(LOXARA, 1), component(PRYVEN, 1)).build();

    /** Source: Myrith + Nuvexa; preview UHV / VEIN. */
    public static final IndustrialMaterial MYRALITE = ore("myralite", "Myralite")
            .contains(component(MYRITH, 1), component(NUVEXA, 1)).build();

    /** Source: Nuvexa + Myrith; preview UHV / VEIN. */
    public static final IndustrialMaterial NUVEXALINE = ore("nuvexaline", "Nuvexaline")
            .contains(component(NUVEXA, 1), component(MYRITH, 1)).build();

    /** Source: Orynd + Nuvexa; preview UHV / BANDED. */
    public static final IndustrialMaterial ORYNDITE = ore("oryndite", "Oryndite")
            .contains(component(ORYND, 1), component(NUVEXA, 1)).build();

    /** Source: Palyx + Pryven; preview UHV / HYDROTHERMAL. */
    public static final IndustrialMaterial PALYXITE = ore("palyxite", "Palyxite")
            .contains(component(PALYX, 1), component(PRYVEN, 1)).build();

    /** Source: Qevora + Ruxen; preview UEV / BANDED. */
    public static final IndustrialMaterial QEVARITE = ore("qevarite", "Qevarite")
            .contains(component(QEVORA, 7), component(RUXEN, 6)).build();

    /** Source: Ruxen + Qevora; preview UEV / BANDED. */
    public static final IndustrialMaterial RUXENITE = ore("ruxenite", "Ruxenite")
            .contains(component(RUXEN, 6), component(QEVORA, 7)).build();

    /** Source: Sylvar + Pryven; preview UEV / HYDROTHERMAL. */
    public static final IndustrialMaterial SYLVARITE = ore("sylvarite", "Sylvarite")
            .contains(component(SYLVAR, 1), component(PRYVEN, 1)).build();

    /** Source: Teryx + Pryven; preview UIV / HYDROTHERMAL. */
    public static final IndustrialMaterial TERYLINE = ore("teryline", "Teryline")
            .contains(component(TERYX, 1), component(PRYVEN, 1)).build();

    /** Source: Uvarya + Pryven; preview UIV / HYDROTHERMAL. */
    public static final IndustrialMaterial UVARYITE = ore("uvaryite", "Uvaryite")
            .contains(component(UVARYA, 1), component(PRYVEN, 1)).build();

    /** Source: Vexalon + Xyther; preview MAX / BANDED. */
    public static final IndustrialMaterial VEXALONITE = ore("vexalonite", "Vexalonite")
            .contains(component(VEXALON, 1), component(XYTHER, 1)).build();

    /** Source: Wyralis + Pryven; preview O / HYDROTHERMAL. */
    public static final IndustrialMaterial WYRALISITE = ore("wyralisite", "Wyralisite")
            .contains(component(WYRALIS, 1), component(PRYVEN, 1)).build();

    /** Source: Xyther + Vexalon; preview MAX / BANDED. */
    public static final IndustrialMaterial XYTHRALITE = ore("xythralite", "Xythralite")
            .contains(component(XYTHER, 1), component(VEXALON, 1)).build();

    private OreMaterials() {
    }

    public static void init() {
        // Touching this class initializes all static ore definitions.
    }
}
