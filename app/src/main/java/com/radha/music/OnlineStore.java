package com.radha.music;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Only metadata and listening preferences are stored; audio/video is never cached to disk. */
public final class OnlineStore {
    private final SharedPreferences prefs;
    public OnlineStore(Context context){prefs=context.getSharedPreferences("online",0);}
    public synchronized void remember(List<MediaEntry> entries){
        LinkedHashMap<String,MediaEntry> all=new LinkedHashMap<>();for(MediaEntry e:entries)all.put(e.id,e);for(MediaEntry e:known())all.putIfAbsent(e.id,e);
        JSONArray json=new JSONArray();int count=0;for(MediaEntry e:all.values()){if(count++>=300)break;json.put(e.json());}prefs.edit().putString("known",json.toString()).apply();
    }
    public List<MediaEntry> known(){LinkedHashMap<String,MediaEntry> out=new LinkedHashMap<>();for(String key:new String[]{"pinned","known"})try{JSONArray a=new JSONArray(prefs.getString(key,"[]"));for(int i=0;i<a.length();i++){MediaEntry entry=MediaEntry.from(a.getJSONObject(i));out.putIfAbsent(entry.id,entry);}}catch(Exception ignored){}return new ArrayList<>(out.values());}
    public void pin(MediaEntry entry){JSONArray out=new JSONArray();out.put(entry.json());try{JSONArray old=new JSONArray(prefs.getString("pinned","[]"));for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);if(!entry.id.equals(item.optString("id")))out.put(item);}}catch(Exception ignored){}prefs.edit().putString("pinned",out.toString()).apply();}
    public void listened(MediaEntry entry){
        remember(Collections.singletonList(entry));List<String> ids=recentIds();ids.remove(entry.id);ids.add(0,entry.id);if(ids.size()>80)ids=ids.subList(0,80);
        Map<String,Integer> scores=scores();scores.put(entry.artist,Math.min(10000,scores.getOrDefault(entry.artist,0)+1));
        prefs.edit().putString("recent",new JSONArray(ids).toString()).putString("scores",new JSONObject(scores).toString()).apply();
    }
    private List<String> recentIds(){List<String> out=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("recent","[]"));for(int i=0;i<a.length();i++)out.add(a.getString(i));}catch(Exception ignored){}return out;}
    public List<MediaEntry> recent(){Map<String,MediaEntry> map=new HashMap<>();for(MediaEntry e:known())map.put(e.id,e);List<MediaEntry> out=new ArrayList<>();for(String id:recentIds())if(map.containsKey(id))out.add(map.get(id));return out;}
    private Map<String,Integer> scores(){Map<String,Integer> result=new HashMap<>();try{JSONObject o=new JSONObject(prefs.getString("scores","{}"));Iterator<String> keys=o.keys();while(keys.hasNext()){String k=keys.next();result.put(k,o.optInt(k));}}catch(Exception ignored){}return result;}
    public String preference(){return prefs.getString("preference","Hindi, Nepali, acoustic");}
    public void preference(String value){prefs.edit().putString("preference",value).apply();}
    public List<String> artists(){return OnlineRules.topArtists(scores(),3);}
    public void clearHistory(){prefs.edit().remove("recent").remove("scores").apply();}
}
