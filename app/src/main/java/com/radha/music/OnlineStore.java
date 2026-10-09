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
    public Map<String,DiscoveryRules.Listening> history(){Map<String,DiscoveryRules.Listening> out=new HashMap<>();try{JSONObject o=new JSONObject(prefs.getString("trackHistory","{}"));Iterator<String> keys=o.keys();while(keys.hasNext()){String id=keys.next();JSONObject item=o.getJSONObject(id);out.put(id,new DiscoveryRules.Listening(item.optInt("plays"),item.optLong("last")));}}catch(Exception ignored){}return out;}
    private List<MediaEntry> entriesFor(List<String> ids,boolean video){Map<String,MediaEntry> all=new HashMap<>();for(MediaEntry e:known())all.put(e.id,e);List<MediaEntry> out=new ArrayList<>();for(String id:ids){MediaEntry e=all.get(id);if(e!=null&&e.video==video)out.add(e);}return out;}
    public List<MediaEntry> speedDials(boolean video){Map<String,DiscoveryRules.Listening> h=history();for(var e:totals().entrySet())h.putIfAbsent(e.getKey(),new DiscoveryRules.Listening(e.getValue(),0));h.keySet().removeIf(id->id.endsWith("/video")!=video);return entriesFor(DiscoveryRules.speedDials(h),video);}
    public List<MediaEntry> forgotten(boolean video){Map<String,DiscoveryRules.Listening> h=history();h.keySet().removeIf(id->id.endsWith("/video")!=video);return entriesFor(DiscoveryRules.forgotten(h,System.currentTimeMillis()),video);}
    private List<String> recentIds(){List<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("recent","[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
    public List<MediaEntry> recent(){prune(System.currentTimeMillis());Map<String,MediaEntry> map=new HashMap<>();for(MediaEntry e:known())map.put(e.id,e);List<MediaEntry> out=new ArrayList<>();for(String id:recentIds())if(map.containsKey(id))out.add(map.get(id));return out;}
    private Map<String,Integer> scores(){Map<String,Integer> result=new HashMap<>();try{JSONObject o=new JSONObject(prefs.getString("scores","{}"));Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();result.put(k,o.optInt(k));}}catch(Exception ignored){}return result;}
    public String preference(){return prefs.getString("preference","Hindi, Nepali, acoustic");}
    public void preference(String value){prefs.edit().putString("preference",value).apply();}
    public List<String> artists(){return OnlineRules.topArtists(scores(),3);}
    public List<String> recommendationQueries(boolean watch){List<String> recentArtists=new ArrayList<>();for(MediaEntry entry:recent())recentArtists.add(entry.artist);return RecommendationRules.queries(artists(),recentArtists,preference(),watch);}
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
        Set<String> keep=new HashSet<>(savedIds());keep.addAll(counts.keySet());keep.addAll(history.keySet());
        keep.addAll(context.getSharedPreferences("library",0).getStringSet("favorites",Collections.emptySet()));
        JSONArray pinned=new JSONArray();try{JSONArray old=new JSONArray(prefs.getString("pinned","[]"));for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);if(keep.contains(item.optString("id")))pinned.put(item);}}catch(Exception ignored){}
        JSONObject remaining=new JSONObject();try{for(var e:history.entrySet())remaining.put(e.getKey(),new JSONObject().put("plays",e.getValue().plays()).put("last",e.getValue().lastPlayed()));}catch(Exception ignored){}
        prefs.edit().putString("recent",new JSONArray(recent).toString()).putString("trackHistory",remaining.toString()).putString("playTotals",new JSONObject(counts).toString()).putString("pinned",pinned.toString()).apply();
    }
    public void clearHistory(){prefs.edit().remove("recent").remove("scores").remove("trackHistory").remove("playTotals").apply();}
}
