package com.radha.music;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Only metadata and listening preferences are stored; audio/video is never cached to disk. */
public final class OnlineStore {
    private final SharedPreferences prefs;
    private final Context context;
    public OnlineStore(Context context){this.context=context.getApplicationContext();prefs=context.getSharedPreferences("online",0);prune(System.currentTimeMillis());}
    public synchronized void remember(List<MediaEntry> entries){
        LinkedHashMap<String,MediaEntry> all=new LinkedHashMap<>();for(MediaEntry e:entries)all.put(e.id,e);for(MediaEntry e:known())all.putIfAbsent(e.id,e);
        JSONArray json=new JSONArray();int count=0;for(MediaEntry e:all.values()){if(count++>=300)break;json.put(e.json());}prefs.edit().putString("known",json.toString()).apply();
    }
    public List<MediaEntry> known(){LinkedHashMap<String,MediaEntry> out=new LinkedHashMap<>();for(String key:new String[]{"pinned","known"})try{JSONArray a=new JSONArray(prefs.getString(key,"[]"));for(int i=0;i<a.length();i++){MediaEntry entry=MediaEntry.from(a.getJSONObject(i));out.putIfAbsent(entry.id,entry);}}catch(Exception ignored){}return new ArrayList<>(out.values());}
    public void pin(MediaEntry entry){JSONArray out=new JSONArray();out.put(entry.json());try{JSONArray old=new JSONArray(prefs.getString("pinned","[]"));for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);if(!entry.id.equals(item.optString("id")))out.put(item);}}catch(Exception ignored){}prefs.edit().putString("pinned",out.toString()).apply();}
    public synchronized void listened(MediaEntry entry){
        prune(System.currentTimeMillis());remember(Collections.singletonList(entry));List<String> ids=recentIds();ids.remove(entry.id);ids.add(0,entry.id);if(ids.size()>80)ids=ids.subList(0,80);
        pin(entry);
        JSONObject history;try{history=new JSONObject(prefs.getString("trackHistory","{}"));}catch(Exception e){history=new JSONObject();}
        try{JSONObject old=history.optJSONObject(entry.id);JSONObject value=new JSONObject();value.put("plays",Math.min(100000,Math.max(totals().getOrDefault(entry.id,0),old==null?0:old.optInt("plays"))+1));value.put("last",System.currentTimeMillis());history.put(entry.id,value);}catch(Exception ignored){}
        Map<String,Integer> scores=scores();scores.put(entry.artist,Math.min(10000,scores.getOrDefault(entry.artist,0)+1));
        Set<String> bestArtists=new HashSet<>(OnlineRules.topArtists(scores,100));scores.keySet().retainAll(bestArtists);
        prefs.edit().putString("recent",new JSONArray(ids).toString()).putString("scores",new JSONObject(scores).toString()).putString("trackHistory",history.toString()).apply();
    }
    public synchronized void learnRadio(MediaEntry seed,List<MediaEntry> related){
        if(related.isEmpty())return;
        JSONArray groups=new JSONArray();try{JSONObject group=new JSONObject().put("seed",seed.id);JSONArray entries=new JSONArray();for(MediaEntry entry:related)entries.put(entry.json());group.put("entries",entries);groups.put(group);JSONArray old=new JSONArray(prefs.getString("tasteRadios","[]"));for(int i=0;i<old.length()&&groups.length()<3;i++)if(!seed.id.equals(old.getJSONObject(i).optString("seed")))groups.put(old.getJSONObject(i));prefs.edit().putString("tasteRadios",groups.toString()).apply();}catch(Exception ignored){}
    }
    public List<OnlineClient.Track> tasteTracks(boolean watch){
        List<List<OnlineClient.Track>> groups=new ArrayList<>();try{JSONArray saved=new JSONArray(prefs.getString("tasteRadios","[]"));for(int i=0;i<saved.length();i++){List<OnlineClient.Track> group=new ArrayList<>();JSONArray entries=saved.getJSONObject(i).getJSONArray("entries");for(int j=0;j<entries.length();j++){MediaEntry e=MediaEntry.from(entries.getJSONObject(j));if(e.video==watch)group.add(new OnlineClient.Track("https://www.youtube.com/watch?v="+OnlineRules.videoId(e.id),e.name,e.artist,e.image,e.duration/1000));}groups.add(group);}}catch(Exception ignored){}return RecommendationRules.mix(groups);
    }
    public Map<String,DiscoveryRules.Listening> history(){Map<String,DiscoveryRules.Listening> out=new HashMap<>();try{JSONObject o=new JSONObject(prefs.getString("trackHistory","{}"));Iterator<String> keys=o.keys();while(keys.hasNext()){String id=keys.next();JSONObject item=o.getJSONObject(id);out.put(id,new DiscoveryRules.Listening(item.optInt("plays"),item.optLong("last")));}}catch(Exception ignored){}return out;}
    private List<MediaEntry> entriesFor(List<String> ids,boolean video){Map<String,MediaEntry> all=new HashMap<>();for(MediaEntry e:known())all.put(e.id,e);List<MediaEntry> out=new ArrayList<>();for(String id:ids){MediaEntry e=all.get(id);if(e!=null&&e.video==video)out.add(e);}return out;}
    public List<MediaEntry> speedDials(boolean video){Map<String,DiscoveryRules.Listening> h=qualified();h.keySet().removeIf(id->id.endsWith("/video")!=video);return entriesFor(DiscoveryRules.speedDials(h),video);}
    private Map<String,DiscoveryRules.Listening> qualified(){Map<String,DiscoveryRules.Listening> out=new HashMap<>();try{JSONObject json=new JSONObject(prefs.getString("searchCompletions","{}"));Iterator<String> ids=json.keys();while(ids.hasNext()){String id=ids.next();JSONObject value=json.getJSONObject(id);out.put(id,new DiscoveryRules.Listening(value.optInt("plays"),value.optLong("last")));}}catch(Exception ignored){}return out;}
    public synchronized void completedSearch(MediaEntry entry){
        Map<String,DiscoveryRules.Listening> history=qualified();DiscoveryRules.Listening old=history.get(entry.id);history.put(entry.id,new DiscoveryRules.Listening(Math.min(100000,old==null?1:old.plays()+1),System.currentTimeMillis()));
        List<String> best=DiscoveryRules.speedDials(history);JSONObject json=new JSONObject();try{for(String id:best){DiscoveryRules.Listening value=history.get(id);json.put(id,new JSONObject().put("plays",value.plays()).put("last",value.lastPlayed()));}}catch(Exception ignored){}
        prefs.edit().putString("searchCompletions",json.toString()).apply();remember(Collections.singletonList(entry));pin(entry);
    }
    public synchronized void learnContext(String query){if(!RelatedRules.moodQuery(query))return;JSONObject scores;try{scores=new JSONObject(prefs.getString("queryTaste","{}"));scores.put(query,Math.min(10000,scores.optInt(query)+1));if(scores.length()>20){String worst=null;Iterator<String> keys=scores.keys();while(keys.hasNext()){String key=keys.next();if(!key.equals(query)&&(worst==null||scores.optInt(key)<scores.optInt(worst)))worst=key;}if(worst!=null)scores.remove(worst);}prefs.edit().putString("queryTaste",scores.toString()).apply();}catch(Exception ignored){}}
    private String preferredContext(){String best="";try{JSONObject json=new JSONObject(prefs.getString("queryTaste","{}"));Iterator<String> keys=json.keys();int score=0;while(keys.hasNext()){String key=keys.next();if(json.optInt(key)>score){score=json.optInt(key);best=key;}}}catch(Exception ignored){}return best;}
    public List<MediaEntry> forgotten(boolean video){Map<String,DiscoveryRules.Listening> h=history();for(var entry:qualified().entrySet())h.putIfAbsent(entry.getKey(),entry.getValue());h.keySet().removeIf(id->id.endsWith("/video")!=video);return entriesFor(DiscoveryRules.forgotten(h,System.currentTimeMillis()),video);}
    private List<String> recentIds(){List<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("recent","[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
    public List<MediaEntry> recent(){prune(System.currentTimeMillis());Map<String,MediaEntry> map=new HashMap<>();for(MediaEntry e:known())map.put(e.id,e);List<MediaEntry> out=new ArrayList<>();for(String id:recentIds())if(map.containsKey(id))out.add(map.get(id));return out;}
    private Map<String,Integer> scores(){Map<String,Integer> result=new HashMap<>();try{JSONObject o=new JSONObject(prefs.getString("scores","{}"));Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();result.put(k,o.optInt(k));}}catch(Exception ignored){}return result;}
    public String preference(){return prefs.getString("preference","Hindi, Nepali, acoustic");}
    public void preference(String value){prefs.edit().putString("preference",value).apply();}
    public List<String> artists(){return OnlineRules.topArtists(scores(),3);}
    public List<String> recommendationQueries(boolean watch){List<String> recentArtists=new ArrayList<>();for(MediaEntry entry:recent())recentArtists.add(entry.artist);List<String> queries=new ArrayList<>(RecommendationRules.queries(artists(),recentArtists,preference(),watch));String mood=preferredContext();if(!mood.isEmpty())queries.add(0,mood+(watch?" music video":""));return queries;}
    private List<String> savedIds(){List<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("saved","[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
    public boolean isSaved(String id){return savedIds().contains(id);}
    public void toggleSaved(MediaEntry entry){List<String> ids=savedIds();if(!ids.remove(entry.id)){ids.add(0,entry.id);pin(entry);}prefs.edit().putString("saved",new JSONArray(ids).toString()).apply();}
    public List<MediaEntry> saved(){Map<String,MediaEntry> all=new HashMap<>();for(MediaEntry e:known())all.put(e.id,e);List<MediaEntry> out=new ArrayList<>();for(String id:savedIds())if(all.containsKey(id))out.add(all.get(id));return out;}
    private Map<String,Integer> totals(){Map<String,Integer> out=new HashMap<>();try{JSONObject json=new JSONObject(prefs.getString("playTotals","{}"));Iterator<String> keys=json.keys();while(keys.hasNext()){String id=keys.next();out.put(id,json.optInt(id));}}catch(Exception ignored){}return out;}
    synchronized void prune(long now){
        Map<String,DiscoveryRules.Listening> history=history();Map<String,Integer> counts=totals();
        for(var e:history.entrySet())counts.merge(e.getKey(),e.getValue().plays(),Math::max);
        List<String> ranked=new ArrayList<>(counts.keySet());ranked.sort(Comparator.<String>comparingInt(counts::get).reversed().thenComparing(Comparator.naturalOrder()));
        Set<String> keepCounts=new HashSet<>(ranked.subList(0,Math.min(300,ranked.size())));counts.keySet().retainAll(keepCounts);
        history.entrySet().removeIf(e->now-e.getValue().lastPlayed()>=15L*24*60*60*1000);
        List<String> recent=recentIds();recent.removeIf(id->!history.containsKey(id));
        Set<String> keep=new HashSet<>(savedIds());keep.addAll(qualified().keySet());keep.addAll(counts.keySet());keep.addAll(history.keySet());
        keep.addAll(context.getSharedPreferences("library",0).getStringSet("favorites",Collections.emptySet()));
        JSONArray pinned=new JSONArray();try{JSONArray old=new JSONArray(prefs.getString("pinned","[]"));for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);if(keep.contains(item.optString("id")))pinned.put(item);}}catch(Exception ignored){}
        JSONObject remaining=new JSONObject();try{for(var e:history.entrySet())remaining.put(e.getKey(),new JSONObject().put("plays",e.getValue().plays()).put("last",e.getValue().lastPlayed()));}catch(Exception ignored){}
        prefs.edit().putString("recent",new JSONArray(recent).toString()).putString("trackHistory",remaining.toString()).putString("playTotals",new JSONObject(counts).toString()).putString("pinned",pinned.toString()).apply();
    }
    public void clearHistory(){prefs.edit().remove("recent").remove("scores").remove("trackHistory").remove("playTotals").remove("searchCompletions").remove("queryTaste").remove("tasteRadios").apply();}
}
