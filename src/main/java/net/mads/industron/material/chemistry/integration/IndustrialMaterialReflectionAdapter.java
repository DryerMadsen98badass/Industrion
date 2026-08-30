package net.mads.industron.material.chemistry.integration;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.MaterialSource;
import net.mads.industron.material.chemistry.MaterialSourceType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class IndustrialMaterialReflectionAdapter {
    public Map<String,MaterialSnapshot> loadAll(){
        Map<String,MaterialSnapshot> result=new LinkedHashMap<>();
        try{
            Class<?> registry=Class.forName("net.mads.industron.material.defenitions.IndustrialMaterials");
            Object all=readStatic(registry,"ALL","MATERIALS","ELEMENTS");
            for(Object material:iterable(all)){
                MaterialSnapshot snapshot=adapt(material);result.put(snapshot.id(),snapshot);
            }
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot load IndustrialMaterials",e);}
        return result;
    }

    public MaterialSnapshot adapt(Object material){
        String id=string(call(material,"id","getId")).toLowerCase(Locale.ROOT);
        String name=string(callOr(material,id,"displayName","name","getDisplayName"));
        int color=integer(callOr(material,0x808080,"color","baseColor","rgb"));
        int tier=0;String tierName="ULV";
        Object tierObj=callOr(material,null,"tier","machineTier");
        if(tierObj instanceof MachineTier machineTier){
            int index=MachineTier.ALL.indexOf(machineTier);
            if(index>=0)tier=index;
            tierName=machineTier.id().toUpperCase(Locale.ROOT);
        }else if(tierObj instanceof Enum<?> e){
            tier=e.ordinal();
            tierName=e.name();
        }
        List<CompositionEntry> components=new ArrayList<>();
        Object rawComponents=callOr(material,List.of(),"components","composition");
        for(Object component:iterable(rawComponents)){
            Object substance=callOr(component,null,"substance","material","component");
            if(substance==null)continue;
            String child=string(call(substance,"id","getId")).toLowerCase(Locale.ROOT);
            int amount=integer(callOr(component,1,"amount","count"));
            ChemistryPhase phase=inferPhase(substance);
            components.add(new CompositionEntry(child,Math.max(1,amount),phase));
        }
        Map<String,Double> props=extractProperties(callOr(material,null,"properties","materialProperties"));
        Set<MaterialClassification> classes=EnumSet.noneOf(MaterialClassification.class);
        if(bool(callOr(material,false,"isMetal","metal")))classes.add(MaterialClassification.CONDUCTOR);
        ChemistryPhase phase=inferPhase(material);
        Set<MaterialSource> sources=inferSources(material,id,phase);
        return net.mads.industron.material.chemistry.ChemistryDefinitions.apply(new MaterialSnapshot(id,name,color,phase,components,Optional.empty(),props,classes,tier,tierName,sources,material));
    }

    private Set<MaterialSource> inferSources(Object material,String id,ChemistryPhase phase){
        Set<MaterialSource> result=new LinkedHashSet<>();
        if(bool(callOr(material,false,"hasWorldgen","generatesOre","oreGenerated")))result.add(new MaterialSource(MaterialSourceType.WORLD_DEPOSIT,id+"_worldgen"));
        Object existingParts = callOr(material, Map.of(), "existingParts");
        if ((existingParts instanceof Map<?, ?> mappedParts && !mappedParts.isEmpty())
                || bool(callOr(material,false,"hasExisting","isExisting","external"))) {
            result.add(new MaterialSource(MaterialSourceType.EXTERNAL_MAPPING,id+"_existing"));
        }
        if(phase==ChemistryPhase.LIQUID&&bool(callOr(material,false,"naturalFluid","hasFluidDeposit")))result.add(new MaterialSource(MaterialSourceType.NATURAL_FLUID_DEPOSIT,id+"_deposit"));
        if(phase==ChemistryPhase.GAS&&bool(callOr(material,false,"naturalGas","hasGasDeposit")))result.add(new MaterialSource(MaterialSourceType.NATURAL_GAS_DEPOSIT,id+"_deposit"));
        return result;
    }

    private ChemistryPhase inferPhase(Object material){
        if(material instanceof IndustrialMaterial industrial){
            return phaseFromProperties(industrial.properties());
        }
        if(material instanceof ElementDefinition element){
            return phaseFromProperties(MaterialPropertyCalculator.calculate(element));
        }

        Object value=callOr(material,null,"phase","state","materialState");
        ChemistryPhase direct=parsePhase(value);
        if(direct!=ChemistryPhase.UNKNOWN)return direct;

        Object properties=callOr(material,null,"properties","materialProperties");
        if(properties!=null){
            ChemistryPhase fromState=parsePhase(callOr(properties,null,"state","physicalState"));
            if(fromState!=ChemistryPhase.UNKNOWN)return fromState;

            Object melting=callOr(properties,null,"meltingPoint","meltingPointC");
            Object boiling=callOr(properties,null,"boilingPoint","boilingPointC");
            if(melting instanceof Number mp && boiling instanceof Number bp){
                if(bp.doubleValue()<=20.0)return ChemistryPhase.GAS;
                if(mp.doubleValue()<=20.0)return ChemistryPhase.LIQUID;
                return ChemistryPhase.SOLID;
            }
        }

        if(bool(callOr(material,false,"isGas","gas")))return ChemistryPhase.GAS;
        if(bool(callOr(material,false,"isLiquid","liquid","isFluid")))return ChemistryPhase.LIQUID;
        return ChemistryPhase.SOLID;
    }

    private static ChemistryPhase phaseFromProperties(MaterialProperties properties){
        return switch(properties.state()){
            case GAS -> ChemistryPhase.GAS;
            case LIQUID -> ChemistryPhase.LIQUID;
            case SOLID -> ChemistryPhase.SOLID;
        };
    }

    private static ChemistryPhase parsePhase(Object value){
        if(value==null)return ChemistryPhase.UNKNOWN;
        String n=String.valueOf(value).toUpperCase(Locale.ROOT);
        if(n.contains("GAS"))return ChemistryPhase.GAS;
        if(n.contains("LIQUID")||n.contains("FLUID"))return ChemistryPhase.LIQUID;
        if(n.contains("MOLTEN"))return ChemistryPhase.MOLTEN;
        if(n.contains("PLASMA"))return ChemistryPhase.PLASMA;
        if(n.contains("SOLID"))return ChemistryPhase.SOLID;
        return ChemistryPhase.UNKNOWN;
    }

    private Map<String,Double> extractProperties(Object properties){
        if(properties==null)return Map.of();Map<String,Double> result=new LinkedHashMap<>();
        for(Method method:properties.getClass().getMethods()){
            if(method.getParameterCount()!=0||method.getDeclaringClass()==Object.class||Modifier.isStatic(method.getModifiers()))continue;
            if(method.getName().equals("hashCode"))continue;
            Class<?> type=method.getReturnType();if(!(type.isPrimitive()||Number.class.isAssignableFrom(type)))continue;
            try{Object value=method.invoke(properties);if(value instanceof Number n)result.put(normalize(method.getName()),n.doubleValue());}catch(ReflectiveOperationException ignored){}
        }
        return result;
    }

    private static String normalize(String s){
        String value=s;
        if(value.startsWith("get")&&value.length()>3)value=value.substring(3);
        return value.replace("_","").toLowerCase(Locale.ROOT);
    }
    private static Object readStatic(Class<?> type,String...names)throws ReflectiveOperationException{for(String name:names)try{Field f=type.getField(name);return f.get(null);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(String.join(",",names));}
    private static Object call(Object target,String...names){Object v=callOr(target,null,names);if(v==null)throw new IllegalStateException("Missing method "+String.join("/",names)+" on "+target.getClass());return v;}
    private static Object callOr(Object target,Object fallback,String...names){if(target==null)return fallback;for(String name:names)try{Method m=target.getClass().getMethod(name);return m.invoke(target);}catch(ReflectiveOperationException ignored){}return fallback;}
    private static Iterable<?> iterable(Object value){if(value instanceof Iterable<?> i)return i;if(value instanceof Object[] a)return List.of(a);if(value instanceof Map<?,?> m)return m.values();return List.of();}
    private static String string(Object value){return String.valueOf(value);}
    private static int integer(Object value){return value instanceof Number n?n.intValue():Integer.parseInt(String.valueOf(value));}
    private static boolean bool(Object value){return value instanceof Boolean b&&b;}
}
