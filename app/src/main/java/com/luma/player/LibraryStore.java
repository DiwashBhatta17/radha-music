package com.radha.music;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.DocumentsContract;
import org.json.*;
import java.util.*;

public final class LibraryStore {
    private final Context context;
    private final android.content.SharedPreferences prefs;
    public final List<MediaEntry> entries=new ArrayList<>();
    public final Set<String> favorites=new HashSet<>();
    public final LinkedHashMap<String,List<MediaEntry>> playlists=new LinkedHashMap<>();
    public LibraryStore(Context context) {
        this.context=context; prefs=context.getSharedPreferences("library",0);
        favorites.addAll(prefs.getStringSet("favorites",Collections.emptySet()));
        try {JSONObject o=new JSONObject(prefs.getString("playlists","{}")); Iterator<String> keys=o.keys(); while(keys.hasNext()){String key=keys.next();JSONArray a=o.getJSONArray(key);List<MediaEntry> list=new ArrayList<>();for(int i=0;i<a.length();i++)list.add(MediaEntry.from(a.getJSONObject(i)));playlists.put(key,list);}}catch(Exception ignored){}
    }
    public List<MediaEntry> scan() {
        List<MediaEntry> result=new ArrayList<>();
        query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,true,result);
        query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,false,result);
        for(String tree:prefs.getStringSet("trees",Collections.emptySet())) {
            Uri uri=Uri.parse(tree);
            try {String id=DocumentsContract.getTreeDocumentId(uri);String label=id;try(Cursor c=context.getContentResolver().query(DocumentsContract.buildDocumentUriUsingTree(uri,id),new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())label=c.getString(0);}scanTree(uri,id,label+"/",result,0);}catch(Exception ignored){}
        }
        result.removeIf(e->!e.video&&!LibraryRules.showMusic(e.name,e.folder));
        result.sort((a,b)->Long.compare(b.added,a.added));
        return result;
    }
    public void addTree(String uri){Set<String> trees=new HashSet<>(prefs.getStringSet("trees",Collections.emptySet()));trees.add(uri);prefs.edit().putStringSet("trees",trees).apply();}
    private void scanTree(Uri tree,String id,String path,List<MediaEntry> out,int depth){
        if(depth>32||Thread.currentThread().isInterrupted())return;
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
        try(Cursor c=context.getContentResolver().query(children,new String[]{"document_id","_display_name","mime_type","last_modified"},null,null,null)){
            if(c==null)return;
            while(c.moveToNext()){
                String child=c.getString(0),name=c.getString(1),mime=c.getString(2);
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){scanTree(tree,child,path+name+"/",out,depth+1);continue;}
                if(mime==null||(!mime.startsWith("audio/")&&!mime.startsWith("video/")))continue;
                String mediaUri=DocumentsContract.buildDocumentUriUsingTree(tree,child).toString();
                boolean duplicate=false;
                for(MediaEntry existing:out)if(existing.name.equals(name)&&(path.endsWith(existing.folder)||existing.folder.endsWith(path))){duplicate=true;break;}
                if(!duplicate)out.add(new MediaEntry(mediaUri,name,path,"Added folder",mime.startsWith("video/"),0,c.getLong(3)/1000));
            }
        }catch(Exception ignored){}
    }
    private void query(Uri uri, boolean video, List<MediaEntry> out) {
        query(uri,video,out,!video&&Build.VERSION.SDK_INT>=31);
    }
    private void query(Uri uri,boolean video,List<MediaEntry> out,boolean recordingColumn) {
        String path=Build.VERSION.SDK_INT>=29 ? "relative_path" : "_data";
        String[] cols=video?new String[]{"_id","_display_name",path,"duration","date_added"}:new String[]{"_id","_display_name",path,"duration","date_added","artist"};
        if(recordingColumn){cols=Arrays.copyOf(cols,cols.length+1);cols[cols.length-1]="is_recording";}
        try(Cursor c=context.getContentResolver().query(uri,cols,null,null,"date_added DESC, _id DESC")) {
            if(c==null)return;
            while(c.moveToNext()) {
                if(recordingColumn&&c.getInt(6)!=0)continue;
                String folder=c.getString(2); if(folder==null)folder="Unknown folder";
                if(Build.VERSION.SDK_INT<29){int p=folder.lastIndexOf('/');folder=p>=0?folder.substring(0,p+1):folder;}
                String artist=video?"":c.getString(5); if(artist==null||artist.equals("<unknown>"))artist="Unknown artist";
                String name=c.getString(1);if(name==null)name="Untitled";
                out.add(new MediaEntry(ContentUris.withAppendedId(uri,c.getLong(0)).toString(),name,folder,artist,video,c.getLong(3),c.getLong(4)));
            }
        } catch(IllegalArgumentException unsupportedColumn){if(recordingColumn)query(uri,video,out,false);else throw unsupportedColumn;}
        catch(SecurityException ignored) { /* A denied category remains empty; other category can still load. */ }
    }
    public void save() {
        JSONObject o=new JSONObject();try{for(Map.Entry<String,List<MediaEntry>> e:playlists.entrySet()){JSONArray a=new JSONArray();for(MediaEntry m:e.getValue())a.put(m.json());o.put(e.getKey(),a);}}catch(Exception ignored){}
        prefs.edit().putStringSet("favorites",new HashSet<>(favorites)).putString("playlists",o.toString()).apply();
    }
    public boolean isUnplayed(String id){return !prefs.getStringSet("played",Collections.emptySet()).contains(id);}
    public void markPlayed(String id){Set<String> played=new HashSet<>(prefs.getStringSet("played",Collections.emptySet()));if(played.add(id))prefs.edit().putStringSet("played",played).apply();}
    public static String folderName(String path){return LibraryRules.folderTitle(path);}
}
