package net.mads.industron.material.defenitions;

import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.GemBlockStyle;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterialBuilder;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialContentProfile;
import net.mads.industron.material.MaterialFormGenerator;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class IndustrialMaterials {
    private static final List<IndustrialMaterial> REGISTERED_MATERIALS = new ArrayList<>();

    // Baseline fictional element set. Atomic numbers 1..100 are intentionally complete.
    // Names are fictional content labels; atomic behavior comes only from Z + tier.
    public static final ElementDefinition VERNIUM =
            element("vernium", "Vernium", "Ve", 1, MachineTier.ULV);

    public static final ElementDefinition TITENITE =
            element("titenite", "Titenite", "Ti", 2, MachineTier.LV);

    public static final ElementDefinition AVARN =
            element("avarn", "Avarn", "Avr", 3, MachineTier.LV);

    public static final ElementDefinition BRELIX =
            element("brelix", "Brelix", "Brx", 4, MachineTier.LV);

    public static final ElementDefinition QUARTZ =
            element("quartz", "Quartz", "Qz", 5, MachineTier.LV)
                    .gemBlockStyle(GemBlockStyle.QUARTZ)
                    .existing(MaterialPart.ROUGH_FLAWLESS_GEM, "minecraft:quartz")
                    .existing(MaterialPart.BLOCK, "minecraft:quartz_block")
                    .existing(MaterialPart.NETHERRACK_ORE, "minecraft:nether_quartz_ore");

    public static final ElementDefinition REDSTONE =
            element("redstone", "Redstone", "Rs", 6, MachineTier.ULV)
                    .gemBlockStyle(GemBlockStyle.REDSTONE)
                    .existing(MaterialPart.DUST, "minecraft:redstone")
                    .existing(MaterialPart.BLOCK, "minecraft:redstone_block")
                    .existing(MaterialPart.ORE, "minecraft:redstone_ore")
                    .existing(MaterialPart.DEEPSLATE_ORE, "minecraft:deepslate_redstone_ore");

    public static final ElementDefinition CEVORA =
            element("cevora", "Cevora", "Cev", 7, MachineTier.LV);

    public static final ElementDefinition DRAXIL =
            element("draxil", "Draxil", "Drx", 8, MachineTier.ULV);

    public static final ElementDefinition ELNARA =
            element("elnara", "Elnara", "Eln", 9, MachineTier.ULV);

    public static final ElementDefinition FYRIN =
            element("fyrin", "Fyrin", "Fyr", 10, MachineTier.ULV);

    public static final ElementDefinition GAVREL =
            element("gavrel", "Gavrel", "Gvr", 11, MachineTier.LV);

    public static final ElementDefinition HESKOR =
            element("heskor", "Heskor", "Hsk", 12, MachineTier.LV);

    public static final ElementDefinition LAPIS =
            element("lapis", "Lapis", "Lp", 13, MachineTier.LV)
                    .gemBlockStyle(GemBlockStyle.LAPIS)
                    .existing(MaterialPart.ROUGH_GEM, "minecraft:lapis_lazuli")
                    .existing(MaterialPart.BLOCK, "minecraft:lapis_block")
                    .existing(MaterialPart.ORE, "minecraft:lapis_ore")
                    .existing(MaterialPart.DEEPSLATE_ORE, "minecraft:deepslate_lapis_ore");

    public static final ElementDefinition DIAMOND =
            element("diamond", "Diamond", "Dia", 14, MachineTier.EV)
                    .gemBlockStyle(GemBlockStyle.DIAMOND)
                    .color(0x62EDE4)
                    .existing(MaterialPart.GEM, "minecraft:diamond")
                    .existing(MaterialPart.BLOCK, "minecraft:diamond_block")
                    .existing(MaterialPart.ORE, "minecraft:diamond_ore")
                    .existing(MaterialPart.DEEPSLATE_ORE, "minecraft:deepslate_diamond_ore");

    public static final ElementDefinition ILYRA =
            element("ilyra", "Ilyra", "Ily", 15, MachineTier.LV);

    public static final ElementDefinition JORVEN =
            element("jorven", "Jorven", "Jrv", 16, MachineTier.ULV);

    public static final ElementDefinition KAVRA =
            element("kavra", "Kavra", "Kvr", 17, MachineTier.ULV);

    public static final ElementDefinition LORYX =
            element("loryx", "Loryx", "Lrx", 18, MachineTier.ULV);

    public static final ElementDefinition MYDREN =
            element("mydren", "Mydren", "Myd", 19, MachineTier.LV);

    public static final ElementDefinition NORVAK =
            element("norvak", "Norvak", "Nrv", 20, MachineTier.LV);

    public static final ElementDefinition ORLUNE =
            element("orlune", "Orlune", "Orl", 21, MachineTier.HV);

    public static final ElementDefinition PRAXEL =
            element("praxel", "Praxel", "Prx", 22, MachineTier.HV);

    public static final ElementDefinition QEVRIN =
            element("qevrin", "Qevrin", "Qvr", 23, MachineTier.HV);

    public static final ElementDefinition RASKEL =
            element("raskel", "Raskel", "Rsk", 24, MachineTier.EV);

    public static final ElementDefinition MADSIUM =
            element("madsium", "Madsium", "Mad", 25, MachineTier.EV);

    public static final ElementDefinition SORYN =
            element("soryn", "Soryn", "Sry", 26, MachineTier.EV);

    public static final ElementDefinition TAVRIX =
            element("tavrix", "Tavrix", "Tvr", 27, MachineTier.EV);

    public static final ElementDefinition UVREN =
            element("uvren", "Uvren", "Uvr", 28, MachineTier.HV);

    public static final ElementDefinition VELYX =
            element("velyx", "Velyx", "Vlx", 29, MachineTier.HV);

    public static final ElementDefinition WEXARA =
            element("wexara", "Wexara", "Wxa", 30, MachineTier.LV);

    public static final ElementDefinition XORIM =
            element("xorim", "Xorim", "Xrm", 31, MachineTier.EV);

    public static final ElementDefinition NETHERITE =
            element("netherite", "Netherite", "Nt", 32, MachineTier.MV)
                    .gemBlockStyle(GemBlockStyle.NETHERITE)
                    .color(0x433D40)
                    .existing(MaterialPart.ROUGH_FLAWLESS_GEM, "minecraft:netherite_scrap")
                    .existing(MaterialPart.BLOCK, "minecraft:netherite_block");

    public static final ElementDefinition YAVREN =
            element("yavren", "Yavren", "Yvr", 33, MachineTier.LV);

    public static final ElementDefinition ZELYTH =
            element("zelyth", "Zelyth", "Zly", 34, MachineTier.ULV);

    public static final ElementDefinition ARVEXA =
            element("arvexa", "Arvexa", "Avx", 35, MachineTier.ULV);

    public static final ElementDefinition BASKORA =
            element("baskora", "Baskora", "Bsa", 36, MachineTier.ULV);

    public static final ElementDefinition CASKEL =
            element("caskel", "Caskel", "Csk", 37, MachineTier.ULV);

    public static final ElementDefinition DENVRA =
            element("denvra", "Denvra", "Dnv", 38, MachineTier.LV);

    public static final ElementDefinition ERYXON =
            element("eryxon", "Eryxon", "Erx", 39, MachineTier.HV);

    public static final ElementDefinition FALUNE =
            element("falune", "Falune", "Fln", 40, MachineTier.HV);

    public static final ElementDefinition GRESKA =
            element("greska", "Greska", "Grs", 41, MachineTier.HV);

    public static final ElementDefinition HAVOR =
            element("havor", "Havor", "Hvr", 42, MachineTier.EV);

    public static final ElementDefinition IXRANE =
            element("ixrane", "Ixrane", "Ixr", 43, MachineTier.EV);

    public static final ElementDefinition JELYX =
            element("jelyx", "Jelyx", "Jlx", 44, MachineTier.EV);

    public static final ElementDefinition KORVEN =
            element("korven", "Korven", "Krv", 45, MachineTier.EV);

    public static final ElementDefinition LASKYR =
            element("laskyr", "Laskyr", "Lsk", 46, MachineTier.HV);

    public static final ElementDefinition MERVANE =
            element("mervane", "Mervane", "Mrv", 47, MachineTier.HV);

    public static final ElementDefinition NYXORA =
            element("nyxora", "Nyxora", "Nyx", 48, MachineTier.LV);

    public static final ElementDefinition ORVEX =
            element("orvex", "Orvex", "Ovx", 49, MachineTier.HV);

    public static final ElementDefinition PYRALIS =
            element("pyralis", "Pyralis", "Pyr", 50, MachineTier.EV);

    public static final ElementDefinition QORVEN =
            element("qorven", "Qorven", "Qrv", 51, MachineTier.LV);

    public static final ElementDefinition RELYX =
            element("relyx", "Relyx", "Rlx", 52, MachineTier.ULV);

    public static final ElementDefinition SENVRA =
            element("senvra", "Senvra", "Snv", 53, MachineTier.ULV);

    public static final ElementDefinition TALYX =
            element("talyx", "Talyx", "Tlx", 54, MachineTier.ULV);

    public static final ElementDefinition USKARA =
            element("uskara", "Uskara", "Usk", 55, MachineTier.ULV);

    public static final ElementDefinition VORYN =
            element("voryn", "Voryn", "Vry", 56, MachineTier.LV);

    public static final ElementDefinition WELYRA =
            element("welyra", "Welyra", "Wlr", 57, MachineTier.MV);

    public static final ElementDefinition XAVREN =
            element("xavren", "Xavren", "Xvr", 58, MachineTier.MV);

    public static final ElementDefinition YSKEL =
            element("yskel", "Yskel", "Ysk", 59, MachineTier.MV);

    public static final ElementDefinition ZORIXA =
            element("zorixa", "Zorixa", "Zrx", 60, MachineTier.MV);

    public static final ElementDefinition AULVEN =
            element("aulven", "Aulven", "Aul", 61, MachineTier.MV);

    public static final ElementDefinition BREYRA =
            element("breyra", "Breyra", "Bya", 62, MachineTier.HV);

    public static final ElementDefinition CROVIX =
            element("crovix", "Crovix", "Crv", 63, MachineTier.HV);

    public static final ElementDefinition DASKEN =
            element("dasken", "Dasken", "Dsk", 64, MachineTier.MV);

    public static final ElementDefinition EVORIN =
            element("evorin", "Evorin", "Evr", 65, MachineTier.MV);

    public static final ElementDefinition FALYX =
            element("falyx", "Falyx", "Flx", 66, MachineTier.MV);

    public static final ElementDefinition GRAVEN =
            element("graven", "Graven", "Gvn", 67, MachineTier.MV);

    public static final ElementDefinition HORYX =
            element("horyx", "Horyx", "Hrx", 68, MachineTier.MV);

    public static final ElementDefinition ISKARA =
            element("iskara", "Iskara", "Isk", 69, MachineTier.ULV);

    public static final ElementDefinition JUVREN =
            element("juvren", "Juvren", "Jvr", 70, MachineTier.LV);

    public static final ElementDefinition KELYRA =
            element("kelyra", "Kelyra", "Klr", 71, MachineTier.HV);

    public static final ElementDefinition LOXEN =
            element("loxen", "Loxen", "Lxn", 72, MachineTier.HV);

    public static final ElementDefinition MAVRIX =
            element("mavrix", "Mavrix", "Mvx", 73, MachineTier.HV);

    public static final ElementDefinition NERYN =
            element("neryn", "Neryn", "Nry", 74, MachineTier.EV);

    public static final ElementDefinition HAAKONIUM =
            element("haakonium", "Haakonium", "Haa", 75, MachineTier.EV);

    public static final ElementDefinition OSKARA =
            element("oskara", "Oskara", "Osk", 76, MachineTier.EV);

    public static final ElementDefinition PERVIX =
            element("pervix", "Pervix", "Pvx", 77, MachineTier.EV);

    public static final ElementDefinition QALYX =
            element("qalyx", "Qalyx", "Qlx", 78, MachineTier.HV);

    public static final ElementDefinition ROVENA =
            element("rovena", "Rovena", "Rvn", 79, MachineTier.HV);

    public static final ElementDefinition SELYR =
            element("selyr", "Selyr", "Slr", 80, MachineTier.LV);

    public static final ElementDefinition TAVORA =
            element("tavora", "Tavora", "Tva", 81, MachineTier.EV);

    public static final ElementDefinition EMERALD =
            element("emerald", "Emerald", "Eme", 82, MachineTier.EV)
                    .gemBlockStyle(GemBlockStyle.EMERALD)
                    .color(0x2ACB58)
                    .existing(MaterialPart.GEM, "minecraft:emerald")
                    .existing(MaterialPart.BLOCK, "minecraft:emerald_block")
                    .existing(MaterialPart.ORE, "minecraft:emerald_ore")
                    .existing(MaterialPart.DEEPSLATE_ORE, "minecraft:deepslate_emerald_ore");

    public static final ElementDefinition URYXEN =
            element("uryxen", "Uryxen", "Urx", 83, MachineTier.LV);

    public static final ElementDefinition VASKYR =
            element("vaskyr", "Vaskyr", "Vsk", 84, MachineTier.ULV);

    public static final ElementDefinition WORYN =
            element("woryn", "Woryn", "Wry", 85, MachineTier.ULV);

    public static final ElementDefinition XELYRA =
            element("xelyra", "Xelyra", "Xlr", 86, MachineTier.ULV);

    public static final ElementDefinition YAVRIX =
            element("yavrix", "Yavrix", "Yvx", 87, MachineTier.ULV);

    public static final ElementDefinition ZESKAL =
            element("zeskal", "Zeskal", "Zsk", 88, MachineTier.LV);

    public static final ElementDefinition ALVORY =
            element("alvory", "Alvory", "Alv", 89, MachineTier.LV);

    public static final ElementDefinition BRINOX =
            element("brinox", "Brinox", "Bnx", 90, MachineTier.MV);

    public static final ElementDefinition CYVERA =
            element("cyvera", "Cyvera", "Cyv", 91, MachineTier.MV);

    public static final ElementDefinition DORYN =
            element("doryn", "Doryn", "Dry", 92, MachineTier.MV);

    public static final ElementDefinition ERYVA =
            element("eryva", "Eryva", "Ery", 93, MachineTier.MV);

    public static final ElementDefinition FASKEL =
            element("faskel", "Faskel", "Fsk", 94, MachineTier.MV);

    public static final ElementDefinition GORVIX =
            element("gorvix", "Gorvix", "Gvx", 95, MachineTier.HV);

    public static final ElementDefinition HELYRA =
            element("helyra", "Helyra", "Hly", 96, MachineTier.MV);

    public static final ElementDefinition IVARA =
            element("ivara", "Ivara", "Iva", 97, MachineTier.MV);

    public static final ElementDefinition JEXON =
            element("jexon", "Jexon", "Jxn", 98, MachineTier.MV);

    public static final ElementDefinition KORYX =
            element("koryx", "Koryx", "Krx", 99, MachineTier.MV);

    public static final ElementDefinition LUVREN =
            element("luvren", "Luvren", "Lvr", 100, MachineTier.MV);


    // Late-game fictional elements. IV/LuV remain Nether-band; ZPM+ are End-band by geology policy.
    public static final ElementDefinition AEVRON =
            element("aevron", "Aevron", "Aev", 101, MachineTier.IV);

    public static final ElementDefinition BRALYX =
            element("bralyx", "Bralyx", "Blx", 102, MachineTier.IV);

    public static final ElementDefinition CIRYNE =
            element("ciryne", "Ciryne", "Cir", 103, MachineTier.IV);

    public static final ElementDefinition DOVREX =
            element("dovrex", "Dovrex", "Dvx", 104, MachineTier.IV);

    public static final ElementDefinition ELARIX =
            element("elarix", "Elarix", "Elx", 105, MachineTier.IV);

    public static final ElementDefinition FARYON =
            element("faryon", "Faryon", "Fyn", 106, MachineTier.IV);

    public static final ElementDefinition GRYVEN =
            element("gryven", "Gryven", "Gry", 107, MachineTier.IV);

    public static final ElementDefinition HELYX =
            element("helyx", "Helyx", "Hlx", 108, MachineTier.IV);

    public static final ElementDefinition IOVRAX =
            element("iovrax", "Iovrax", "Iov", 109, MachineTier.IV);

    public static final ElementDefinition JARYNE =
            element("jaryne", "Jaryne", "Jyn", 110, MachineTier.IV);

    public static final ElementDefinition KEXARA =
            element("kexara", "Kexara", "Kxa", 111, MachineTier.IV);

    public static final ElementDefinition LYRVEN =
            element("lyrven", "Lyrven", "Lvn", 112, MachineTier.IV);

    public static final ElementDefinition MORYX =
            element("moryx", "Moryx", "Mox", 113, MachineTier.LUV);

    public static final ElementDefinition NAXIRA =
            element("naxira", "Naxira", "Nax", 114, MachineTier.LUV);

    public static final ElementDefinition OVELYN =
            element("ovelyn", "Ovelyn", "Ovl", 115, MachineTier.LUV);

    public static final ElementDefinition PRYVEN =
            element("pryven", "Pryven", "Pyv", 116, MachineTier.LUV);

    public static final ElementDefinition QYXARA =
            element("qyxara", "Qyxara", "Qyx", 117, MachineTier.LUV);

    public static final ElementDefinition RHELIX =
            element("rhelix", "Rhelix", "Rhx", 118, MachineTier.LUV);

    public static final ElementDefinition SYVREN =
            element("syvren", "Syvren", "Syv", 119, MachineTier.LUV);

    public static final ElementDefinition TYRAX =
            element("tyrax", "Tyrax", "Tya", 120, MachineTier.LUV);

    public static final ElementDefinition ULEXAR =
            element("ulexar", "Ulexar", "Ulx", 121, MachineTier.LUV);

    public static final ElementDefinition VORYXA =
            element("voryxa", "Voryxa", "Vyx", 122, MachineTier.LUV);

    public static final ElementDefinition WYREON =
            element("wyreon", "Wyreon", "Wyo", 123, MachineTier.LUV);

    public static final ElementDefinition XANDREL =
            element("xandrel", "Xandrel", "Xdl", 124, MachineTier.LUV);

    public static final ElementDefinition YRYXEN =
            element("yryxen", "Yryxen", "Yrx", 125, MachineTier.ZPM);

    public static final ElementDefinition ZORVANE =
            element("zorvane", "Zorvane", "Zvn", 126, MachineTier.ZPM);

    public static final ElementDefinition AXYRON =
            element("axyron", "Axyron", "Axr", 127, MachineTier.ZPM);

    public static final ElementDefinition BELVIX =
            element("belvix", "Belvix", "Bvx", 128, MachineTier.ZPM);

    public static final ElementDefinition CORTHEN =
            element("corthen", "Corthen", "Cth", 129, MachineTier.ZPM);

    public static final ElementDefinition DRELYX =
            element("drelyx", "Drelyx", "Dlx", 130, MachineTier.ZPM);

    public static final ElementDefinition ERYNDRA =
            element("eryndra", "Eryndra", "Edr", 131, MachineTier.ZPM);

    public static final ElementDefinition FYRAXON =
            element("fyraxon", "Fyraxon", "Fxn", 132, MachineTier.ZPM);

    public static final ElementDefinition GALYTH =
            element("galyth", "Galyth", "Glt", 133, MachineTier.UV);

    public static final ElementDefinition HORYVEN =
            element("horyven", "Horyven", "Hvn", 134, MachineTier.UV);

    public static final ElementDefinition ILYXAR =
            element("ilyxar", "Ilyxar", "Ilx", 135, MachineTier.UV);

    public static final ElementDefinition JORATH =
            element("jorath", "Jorath", "Jot", 136, MachineTier.UV);

    public static final ElementDefinition KYVREN =
            element("kyvren", "Kyvren", "Kyv", 137, MachineTier.UV);

    public static final ElementDefinition LOXARA =
            element("loxara", "Loxara", "Lxa", 138, MachineTier.UV);

    public static final ElementDefinition MYRITH =
            element("myrith", "Myrith", "Myr", 139, MachineTier.UHV);

    public static final ElementDefinition NUVEXA =
            element("nuvexa", "Nuvexa", "Nvx", 140, MachineTier.UHV);

    public static final ElementDefinition ORYND =
            element("orynd", "Orynd", "Ory", 141, MachineTier.UHV);

    public static final ElementDefinition PALYX =
            element("palyx", "Palyx", "Plx", 142, MachineTier.UHV);

    public static final ElementDefinition QEVORA =
            element("qevora", "Qevora", "Qvo", 143, MachineTier.UEV);

    public static final ElementDefinition RUXEN =
            element("ruxen", "Ruxen", "Rux", 144, MachineTier.UEV);

    public static final ElementDefinition SYLVAR =
            element("sylvar", "Sylvar", "Syl", 145, MachineTier.UEV);

    public static final ElementDefinition TERYX =
            element("teryx", "Teryx", "Trx", 146, MachineTier.UIV);

    public static final ElementDefinition UVARYA =
            element("uvarya", "Uvarya", "Uva", 147, MachineTier.UIV);

    public static final ElementDefinition VEXALON =
            element("vexalon", "Vexalon", "Vxn", 148, MachineTier.UXV);

    public static final ElementDefinition WYRALIS =
            element("wyralis", "Wyralis", "Wys", 149, MachineTier.OPV);

    public static final ElementDefinition XYTHER =
            element("xyther", "Xyther", "Xyt", 150, MachineTier.MAX);

    public static final List<ElementDefinition> ELEMENT_DEFINITIONS = discoverElementDefinitions();

    public static final List<IndustrialMaterial> ELEMENTS = ELEMENT_DEFINITIONS.stream()
            .map(IndustrialMaterials::materialFromElement)
            .toList();
    public static final List<IndustrialFluid> FLUIDS = List.of();
    public static final List<IndustrialMaterial> MATERIALS = Collections.unmodifiableList(REGISTERED_MATERIALS);
    public static final List<IndustrialMaterial> ALL = Collections.unmodifiableList(new AbstractList<>() {
        @Override
        public IndustrialMaterial get(int index) {
            return index < ELEMENTS.size()
                    ? ELEMENTS.get(index)
                    : REGISTERED_MATERIALS.get(index - ELEMENTS.size());
        }

        @Override
        public int size() {
            return ELEMENTS.size() + REGISTERED_MATERIALS.size();
        }
    });

    static {
        CompoundMaterials.init();
        MineralDustMaterials.init();
        OreMaterials.init();
    }

    private static List<ElementDefinition> discoverElementDefinitions() {
        List<ElementDefinition> elements = new ArrayList<>();
        for (java.lang.reflect.Field field : IndustrialMaterials.class.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    || field.getType() != ElementDefinition.class) {
                continue;
            }
            try {
                ElementDefinition element = (ElementDefinition) field.get(null);
                if (element != null) elements.add(element);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not discover element definition " + field.getName(), e);
            }
        }
        elements.sort(java.util.Comparator.comparingInt(ElementDefinition::atomicNumber));
        java.util.HashSet<Integer> atomicNumbers = new java.util.HashSet<>();
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (ElementDefinition element : elements) {
            if (!atomicNumbers.add(element.atomicNumber())) {
                throw new IllegalStateException("Duplicate atomic number " + element.atomicNumber());
            }
            if (!ids.add(element.id())) {
                throw new IllegalStateException("Duplicate element id " + element.id());
            }
        }
        return List.copyOf(elements);
    }

    private IndustrialMaterials() {
    }

    public static ElementDefinition element(
            String id,
            String displayName,
            String symbol,
            int atomicNumber,
            MachineTier tier
    ) {
        return new ElementDefinition(id, displayName, symbol, atomicNumber, tier);
    }

    /** Starts a fictional compound/alloy/polymer/fluid material definition. */
    public static IndustrialMaterialBuilder material(String id, String displayName, int color) {
        return new IndustrialMaterialBuilder(id, displayName, color, MaterialContentProfile.AUTO);
    }

    /** Starts a reviewed trace mineral that owns exactly one generated form: DUST. */
    public static IndustrialMaterialBuilder mineralDust(String id, String displayName) {
        return new IndustrialMaterialBuilder(id, displayName, -1, MaterialContentProfile.MINERAL_DUST);
    }

    /** Starts a reviewed natural ore. Color, chemistry, tier and geology are derived automatically. */
    public static IndustrialMaterialBuilder ore(String id, String displayName) {
        return new IndustrialMaterialBuilder(id, displayName, -1, MaterialContentProfile.ORE);
    }

    /** One top-level composition unit used by {@code .contains(...)}. */
    public static MaterialComponent component(IndustrialSubstance substance, int amount) {
        return new MaterialComponent(substance, amount);
    }

    public static void registerCompound(IndustrialMaterial material) {
        if (REGISTERED_MATERIALS.stream().anyMatch(existing -> existing.id().equals(material.id()))) {
            throw new IllegalStateException("Duplicate compound material id: " + material.id());
        }
        REGISTERED_MATERIALS.add(material);
    }

    private static IndustrialMaterial materialFromElement(ElementDefinition element) {
        MaterialProperties properties = MaterialPropertyCalculator.calculate(element);

        EnumSet<MaterialPart> parts = EnumSet.noneOf(MaterialPart.class);
        parts.addAll(MaterialFormGenerator.partsFor(properties));
        parts.addAll(element.existingParts().keySet());

        String blockMaterialSet = properties.gemCandidate()
                ? element.gemBlockStyle().orElseGet(() -> GemBlockStyle.forAtomicNumber(element.atomicNumber())).id()
                : "dull";

        return new IndustrialMaterial(
                element.id(),
                element.displayName(),
                element.atomicNumber(),
                element.tier(),
                MaterialContentProfile.AUTO,
                properties,
                "dull",
                blockMaterialSet,
                Set.copyOf(parts),
                element.existingParts(),
                Set.of(),
                Map.of(),
                properties.radioactivity(),
                Optional.of(element.symbol()),
                List.of(),
                0,
                false,
                Map.of(),
                List.of(),
                Optional.empty(),
                0,
                Optional.empty(),
                0,
                Optional.empty(),
                true
        );
    }
}
