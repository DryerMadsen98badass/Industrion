package net.mads.industron.climate;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.mads.industron.farming.CalendarPlants;

/** One bounded worker; no worker closure reads levels, entities, tags or inventories. */
public final class ClimateWorker {
    private ClimateWorker() {}
    private record Key(ServerLevel level,long pos) {}
    private record Job(double from,double to,double unloadedFrom,long generation,ClimateGrowth.Environment environment,
        PlantClimateProfile profile,Future<Completed> result) {}
    public record Completed(double to,ClimateGrowth.Result result,double unloadedGrowth) {}
    private static final Map<Key,Job> jobs=new LinkedHashMap<>();
    private static ThreadPoolExecutor executor;
    private static ThreadPoolExecutor executor(){
        if(executor==null ||executor.isShutdown())executor=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(256),r->{Thread t=new Thread(r,"Industron climate");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
    public static Completed compute(ServerLevel level,BlockPos pos,long generation,double from,double to,double unloadedFrom,
            ClimateGrowth.Environment environment,PlantClimateProfile profile) {
        Key key=new Key(level,pos.asLong());Job job=jobs.get(key);
        if(job!=null && (job.from!=from ||job.unloadedFrom!=unloadedFrom ||job.generation!=generation ||!job.environment.equals(environment)||!job.profile.equals(profile))) {
            job.result.cancel(false);jobs.remove(key);job=null;
        }
        if(job!=null) {
            if(!job.result.isDone())return null;
            jobs.remove(key);
            try{return job.result.get();}catch(Exception failure){return null;}
        }
        if(to-from<=.25)return calculate(from,to,unloadedFrom,environment,profile);
        if(jobs.size()>=256)return null;
        try{Future<Completed> result=executor().submit(()->calculate(from,to,unloadedFrom,environment,profile));
            jobs.put(key,new Job(from,to,unloadedFrom,generation,environment,profile,result));}catch(RejectedExecutionException ignored){}
        return null;
    }
    private static Completed calculate(double from,double to,double unloadedFrom,ClimateGrowth.Environment environment,PlantClimateProfile profile) {
        var total=ClimateGrowth.integrate(from,to,environment,profile);
        double unloaded=unloadedFrom<0?0:ClimateGrowth.integrate(Math.max(from,unloadedFrom),to,environment,profile).growth();
        return new Completed(to,total,Math.min(total.growth(),unloaded));
    }
    public static void drain() {
        int budget=64;
        for(Key key:List.copyOf(jobs.keySet())) {
            Job job=jobs.get(key);if(job==null||!job.result.isDone())continue;
            if(!key.level.hasChunkAt(BlockPos.of(key.pos))) {jobs.remove(key);continue;}
            CalendarPlants.update(key.level,BlockPos.of(key.pos));if(--budget==0)break;
        }
    }
    public static void forget(ServerLevel level,BlockPos pos){Job job=jobs.remove(new Key(level,pos.asLong()));if(job!=null)job.result.cancel(false);}
    public static void stop(){jobs.clear();if(executor!=null)executor.shutdownNow();executor=null;}
}
