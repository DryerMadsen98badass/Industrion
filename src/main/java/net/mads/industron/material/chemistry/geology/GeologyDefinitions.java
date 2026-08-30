package net.mads.industron.material.chemistry.geology;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GeologyDefinitions {
    private static final Map<String,OreMineral> MINERALS=new LinkedHashMap<>();
    private static final Map<String,DepositDefinition> DEPOSITS=new LinkedHashMap<>();
    private GeologyDefinitions(){}
    public static synchronized OreMineral mineral(OreMineral value){if(MINERALS.putIfAbsent(value.id(),value)!=null)throw new IllegalStateException("Duplicate ore mineral "+value.id());return value;}
    public static synchronized DepositDefinition deposit(DepositDefinition value){if(DEPOSITS.putIfAbsent(value.id(),value)!=null)throw new IllegalStateException("Duplicate deposit "+value.id());return value;}
    public static synchronized Collection<OreMineral> minerals(){return java.util.List.copyOf(MINERALS.values());}
    public static synchronized Collection<DepositDefinition> deposits(){return java.util.List.copyOf(DEPOSITS.values());}
    public static synchronized Optional<OreMineral> mineral(String id){return Optional.ofNullable(MINERALS.get(id));}
    public static synchronized Optional<DepositDefinition> deposit(String id){return Optional.ofNullable(DEPOSITS.get(id));}
}
