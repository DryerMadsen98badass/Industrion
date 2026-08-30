package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyMetal;

/** Public material identities for assembly recipes. Future alloys can be added here too. */
public final class Metal {
    /**
     * Leaves material choice free. In a Component tree every Material leaf may resolve
     * independently unless an explicit Metal.X fixes that branch.
     */
    public static final AssemblyMetal ANY = metal("any");
    public static final AssemblyMetal VERNIUM = metal("vernium");
    public static final AssemblyMetal TITENITE = metal("titenite");
    public static final AssemblyMetal AVARN = metal("avarn");
    public static final AssemblyMetal BRELIX = metal("brelix");
    public static final AssemblyMetal QUARTZ = metal("quartz");
    public static final AssemblyMetal REDSTONE = metal("redstone");
    public static final AssemblyMetal CEVORA = metal("cevora");
    public static final AssemblyMetal DRAXIL = metal("draxil");
    public static final AssemblyMetal ELNARA = metal("elnara");
    public static final AssemblyMetal FYRIN = metal("fyrin");
    public static final AssemblyMetal GAVREL = metal("gavrel");
    public static final AssemblyMetal HESKOR = metal("heskor");
    public static final AssemblyMetal LAPIS = metal("lapis");
    public static final AssemblyMetal DIAMOND = metal("diamond");
    public static final AssemblyMetal ILYRA = metal("ilyra");
    public static final AssemblyMetal JORVEN = metal("jorven");
    public static final AssemblyMetal KAVRA = metal("kavra");
    public static final AssemblyMetal LORYX = metal("loryx");
    public static final AssemblyMetal MYDREN = metal("mydren");
    public static final AssemblyMetal NORVAK = metal("norvak");
    public static final AssemblyMetal ORLUNE = metal("orlune");
    public static final AssemblyMetal PRAXEL = metal("praxel");
    public static final AssemblyMetal QEVRIN = metal("qevrin");
    public static final AssemblyMetal RASKEL = metal("raskel");
    public static final AssemblyMetal MADSIUM = metal("madsium");
    public static final AssemblyMetal SORYN = metal("soryn");
    public static final AssemblyMetal TAVRIX = metal("tavrix");
    public static final AssemblyMetal UVREN = metal("uvren");
    public static final AssemblyMetal VELYX = metal("velyx");
    public static final AssemblyMetal WEXARA = metal("wexara");
    public static final AssemblyMetal XORIM = metal("xorim");
    public static final AssemblyMetal NETHERITE = metal("netherite");
    public static final AssemblyMetal YAVREN = metal("yavren");
    public static final AssemblyMetal ZELYTH = metal("zelyth");
    public static final AssemblyMetal ARVEXA = metal("arvexa");
    public static final AssemblyMetal BASKORA = metal("baskora");
    public static final AssemblyMetal CASKEL = metal("caskel");
    public static final AssemblyMetal DENVRA = metal("denvra");
    public static final AssemblyMetal ERYXON = metal("eryxon");
    public static final AssemblyMetal FALUNE = metal("falune");
    public static final AssemblyMetal GRESKA = metal("greska");
    public static final AssemblyMetal HAVOR = metal("havor");
    public static final AssemblyMetal IXRANE = metal("ixrane");
    public static final AssemblyMetal JELYX = metal("jelyx");
    public static final AssemblyMetal KORVEN = metal("korven");
    public static final AssemblyMetal LASKYR = metal("laskyr");
    public static final AssemblyMetal MERVANE = metal("mervane");
    public static final AssemblyMetal NYXORA = metal("nyxora");
    public static final AssemblyMetal ORVEX = metal("orvex");
    public static final AssemblyMetal PYRALIS = metal("pyralis");
    public static final AssemblyMetal QORVEN = metal("qorven");
    public static final AssemblyMetal RELYX = metal("relyx");
    public static final AssemblyMetal SENVRA = metal("senvra");
    public static final AssemblyMetal TALYX = metal("talyx");
    public static final AssemblyMetal USKARA = metal("uskara");
    public static final AssemblyMetal VORYN = metal("voryn");
    public static final AssemblyMetal WELYRA = metal("welyra");
    public static final AssemblyMetal XAVREN = metal("xavren");
    public static final AssemblyMetal YSKEL = metal("yskel");
    public static final AssemblyMetal ZORIXA = metal("zorixa");
    public static final AssemblyMetal AULVEN = metal("aulven");
    public static final AssemblyMetal BREYRA = metal("breyra");
    public static final AssemblyMetal CROVIX = metal("crovix");
    public static final AssemblyMetal DASKEN = metal("dasken");
    public static final AssemblyMetal EVORIN = metal("evorin");
    public static final AssemblyMetal FALYX = metal("falyx");
    public static final AssemblyMetal GRAVEN = metal("graven");
    public static final AssemblyMetal HORYX = metal("horyx");
    public static final AssemblyMetal ISKARA = metal("iskara");
    public static final AssemblyMetal JUVREN = metal("juvren");
    public static final AssemblyMetal KELYRA = metal("kelyra");
    public static final AssemblyMetal LOXEN = metal("loxen");
    public static final AssemblyMetal MAVRIX = metal("mavrix");
    public static final AssemblyMetal NERYN = metal("neryn");
    public static final AssemblyMetal HAAKONIUM = metal("haakonium");
    public static final AssemblyMetal OSKARA = metal("oskara");
    public static final AssemblyMetal PERVIX = metal("pervix");
    public static final AssemblyMetal QALYX = metal("qalyx");
    public static final AssemblyMetal ROVENA = metal("rovena");
    public static final AssemblyMetal SELYR = metal("selyr");
    public static final AssemblyMetal TAVORA = metal("tavora");
    public static final AssemblyMetal EMERALD = metal("emerald");
    public static final AssemblyMetal URYXEN = metal("uryxen");
    public static final AssemblyMetal VASKYR = metal("vaskyr");
    public static final AssemblyMetal WORYN = metal("woryn");
    public static final AssemblyMetal XELYRA = metal("xelyra");
    public static final AssemblyMetal YAVRIX = metal("yavrix");
    public static final AssemblyMetal ZESKAL = metal("zeskal");
    public static final AssemblyMetal ALVORY = metal("alvory");
    public static final AssemblyMetal BRINOX = metal("brinox");
    public static final AssemblyMetal CYVERA = metal("cyvera");
    public static final AssemblyMetal DORYN = metal("doryn");
    public static final AssemblyMetal ERYVA = metal("eryva");
    public static final AssemblyMetal FASKEL = metal("faskel");
    public static final AssemblyMetal GORVIX = metal("gorvix");
    public static final AssemblyMetal HELYRA = metal("helyra");
    public static final AssemblyMetal IVARA = metal("ivara");
    public static final AssemblyMetal JEXON = metal("jexon");
    public static final AssemblyMetal KORYX = metal("koryx");
    public static final AssemblyMetal LUVREN = metal("luvren");

    private Metal() {
    }

    public static AssemblyMetal metal(String id) {
        return new AssemblyMetal(id);
    }
}
