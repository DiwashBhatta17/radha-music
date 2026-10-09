package com.radha.music;
import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** App-private files: never exposed to MediaStore, other apps or a FileProvider. */
public final class DownloadStore {
    public record Files(File audio,File video){}
    private final File root;private final SharedPreferences prefs;
    public DownloadStore(Context c){root=new File(c.getFilesDir(),"downloads");prefs=c.getSharedPreferences("downloads",0);}
    public File file(String id,String part){String key=OnlineRules.videoId(id);if(key==null||!Arrays.asList("audio","video").contains(part))throw new IllegalArgumentException("Invalid download");root.mkdirs();return new File(root,key+(id.endsWith("/video")?"-watch-":"-music-")+part+".media");}
    public void state(MediaEntry e,String state){prefs.edit().putString("entry:"+e.id,e.json().toString()).putString("state:"+e.id,state).apply();}
    public String state(String id){return prefs.getString("state:"+id,"Not downloaded");}
    public void complete(MediaEntry e,boolean audio,boolean video){prefs.edit().putString("entry:"+e.id,e.json().toString()).putBoolean("audio:"+e.id,audio).putBoolean("video:"+e.id,video).putString("state:"+e.id,"Downloaded").commit();}
    public Files files(String id){if(!state(id).equals("Downloaded"))return null;File a=prefs.getBoolean("audio:"+id,false)?file(id,"audio"):null,v=prefs.getBoolean("video:"+id,false)?file(id,"video"):null;if((a!=null&&(!a.isFile()||a.length()==0))||(v!=null&&(!v.isFile()||v.length()==0))||(a==null&&v==null))return null;return new Files(a,v);}
    public List<MediaEntry> entries(){List<MediaEntry> out=new ArrayList<>();for(var e:prefs.getAll().entrySet())if(e.getKey().startsWith("entry:"))try{out.add(MediaEntry.from(new JSONObject(String.valueOf(e.getValue()))));}catch(Exception ignored){}out.sort(Comparator.comparingLong((MediaEntry e)->e.added).reversed());return out;}
    public void remove(String id){for(String part:new String[]{"audio","video"}){File f=file(id,part);f.delete();new File(f.getPath()+".part").delete();}prefs.edit().remove("entry:"+id).remove("state:"+id).remove("audio:"+id).remove("video:"+id).apply();}
}
