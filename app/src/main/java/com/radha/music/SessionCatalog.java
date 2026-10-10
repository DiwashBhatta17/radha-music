package com.radha.music;

import java.util.*;
import java.util.concurrent.*;

/** Memory-only catalog: navigation never owns or cancels a shared fetch. */
final class SessionCatalog {
    interface Backend {
        OnlineClient.Results search(String query,boolean watch)throws Exception;
        OnlineClient.Results more(OnlineClient.Results previous)throws Exception;
    }
    static final SessionCatalog INSTANCE=new SessionCatalog();
    private final Backend backend;
    SessionCatalog(){this(new Backend(){public OnlineClient.Results search(String q,boolean w)throws Exception{return OnlineClient.search(q,w);}public OnlineClient.Results more(OnlineClient.Results p)throws Exception{return OnlineClient.more(p);}});}
    SessionCatalog(Backend backend){this.backend=backend;}
    private final ExecutorService workers=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"Radha catalog");t.setDaemon(true);return t;});
    private final Map<Object,FutureTask<OnlineClient.Results>> requests=new LinkedHashMap<>();
    private String preference,musicSeed,watchSeed;
    private List<OnlineClient.Track> musicTaste=Collections.emptyList(),watchTaste=Collections.emptyList();
    private List<String> musicQueries=Collections.emptyList(),watchQueries=Collections.emptyList();
    private final List<FutureTask<OnlineClient.Results>> startup=new ArrayList<>();
    private boolean startupShown;
    synchronized boolean takeStartup(){if(startupShown)return false;startupShown=true;return true;}
    synchronized boolean startupReady(){return !startup.isEmpty()&&startup.stream().allMatch(FutureTask::isDone);}
    synchronized String seed(OnlineStore store,boolean watch){
        if(musicSeed==null||!Objects.equals(preference,store.preference())){
            requests.clear();musicTaste=store.tasteTracks(false);watchTaste=store.tasteTracks(true);preference=store.preference();musicQueries=store.recommendationQueries(false);watchQueries=new ArrayList<>(store.recommendationQueries(true));watchQueries.add(store.preference()+" live music sessions");
            musicSeed=musicQueries.get(0);watchSeed=watchQueries.get(0);
        }
        return watch?watchSeed:musicSeed;
    }
    synchronized void warm(OnlineStore store){if(startup.isEmpty())for(boolean watch:new boolean[]{false,true})startup.add(searchTask(seed(store,watch),watch));}
    private synchronized FutureTask<OnlineClient.Results> searchTask(String q,boolean watch){
        boolean home=q.equals(watch?watchSeed:musicSeed);List<String> queries=new ArrayList<>(watch?watchQueries:musicQueries);
        return task((watch?"video:":"music:")+q,()->home?recommend(queries,watch):backend.search(q,watch));
    }
    OnlineClient.Results recommend(List<String> queries,boolean watch)throws Exception{
        List<List<OnlineClient.Track>> groups=new ArrayList<>();List<OnlineClient.Track> taste=watch?watchTaste:musicTaste;if(!taste.isEmpty())groups.add(taste);OnlineClient.Results first=null;Exception failure=null;boolean fallback=false;
        for(String query:queries){try{OnlineClient.Results found=backend.search(query,watch);if(first==null)first=found;groups.add(found.tracks());fallback|=found.fallback();}catch(Exception e){failure=e;}}
        if(first==null)throw failure==null?new java.io.IOException("No recommendations available"):failure;
        List<OnlineClient.Track> tracks=RecommendationRules.mix(groups);OnlineClient.Results tail=first;
        for(int page=1;!watch&&tracks.size()<25&&OnlineClient.hasMore(tail)&&page<5;page++){
            try{tail=backend.more(tail);groups.add(tail.tracks());tracks=RecommendationRules.mix(groups);}catch(Exception e){break;}
        }
        return new OnlineClient.Results(tracks,tail.query(),tail.next(),fallback);
    }
    OnlineClient.Results search(String q,boolean watch)throws Exception{return await(searchTask(q,watch));}
    synchronized OnlineClient.Results peek(String q,boolean watch){FutureTask<OnlineClient.Results> ready=requests.get((watch?"video:":"music:")+q);if(ready==null||!ready.isDone())return null;try{return ready.get();}catch(Exception e){return null;}}
    OnlineClient.Results more(OnlineClient.Results previous)throws Exception{return await(task(previous,()->backend.more(previous)));}
    synchronized FutureTask<OnlineClient.Results> task(Object key,Callable<OnlineClient.Results> work){
        FutureTask<OnlineClient.Results> existing=requests.get(key);if(existing!=null)return existing;
        FutureTask<OnlineClient.Results> created=new FutureTask<>(work){@Override protected void done(){try{get();}catch(Exception e){synchronized(SessionCatalog.this){requests.remove(key,this);}}}};
        if(requests.size()>=64){Iterator<Map.Entry<Object,FutureTask<OnlineClient.Results>>> it=requests.entrySet().iterator();while(it.hasNext()){Map.Entry<Object,FutureTask<OnlineClient.Results>> e=it.next();if(e.getValue().isDone()&&!e.getKey().equals("music:"+musicSeed)&&!e.getKey().equals("video:"+watchSeed)){it.remove();break;}}}
        requests.put(key,created);workers.execute(created);return created;
    }
    static OnlineClient.Results await(FutureTask<OnlineClient.Results> task)throws Exception{try{return task.get();}catch(ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Error)throw (Error)cause;if(cause instanceof Exception)throw (Exception)cause;throw new RuntimeException(cause);}}
    synchronized void reset(){requests.clear();startup.clear();startupShown=false;musicTaste=Collections.emptyList();watchTaste=Collections.emptyList();preference=null;musicSeed=null;watchSeed=null;musicQueries=Collections.emptyList();watchQueries=Collections.emptyList();}
}
