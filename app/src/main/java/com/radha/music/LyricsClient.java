package com.radha.music;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class LyricsClient {
    static synchronized void trimCache(Context context){
        File[] files=new File(context.getFilesDir(),"lyrics").listFiles();if(files==null)return;
        Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());long bytes=0;int count=0;
        for(File file:files)if(file.isFile()&&file.getName().endsWith(".json")){
            if(System.currentTimeMillis()-file.lastModified()>=15L*24*60*60*1000||count>=100||bytes+file.length()>8L*1024*1024)file.delete();
            else{count++;bytes+=file.length();}
        }
    }
    public record Lyrics(String plain,List<LyricsRules.Line> lines,boolean instrumental){}
    private static String encode(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    public static Lyrics fetch(Context context,MediaEntry entry)throws Exception {
        trimCache(context);
        String title=LyricsRules.title(entry.name),artist=LyricsRules.artist(entry.artist);long seconds=entry.duration/1000;
        String key=java.util.UUID.nameUUIDFromBytes((title+"\n"+artist+"\n"+seconds).getBytes(StandardCharsets.UTF_8)).toString();File dir=new File(context.getFilesDir(),"lyrics"),cache=new File(dir,key+".json");
        if(cache.isFile())try(FileInputStream in=new FileInputStream(cache)){return parse(new JSONObject(read(in)));}
        String params="track_name="+encode(title)+"&artist_name="+encode(artist)+(seconds>0?"&duration="+seconds:"");JSONObject value=null;
        try{value=new JSONObject(get("https://lrclib.net/api/get?"+params));}catch(FileNotFoundException ignored){}
        if(value==null){JSONArray found=new JSONArray(get("https://lrclib.net/api/search?track_name="+encode(title)+"&artist_name="+encode(artist)));for(int i=0;i<found.length();i++){JSONObject item=found.getJSONObject(i);if(LyricsRules.matches(title,artist,seconds,item.optString("trackName"),item.optString("artistName"),item.optDouble("duration"))){value=item;break;}}}
        if(value==null)return new Lyrics("",Collections.emptyList(),false);Lyrics result=parse(value);if(!result.plain().isEmpty()||!result.lines().isEmpty()||result.instrumental()){dir.mkdirs();File[] files=dir.listFiles();if(files!=null&&files.length>=200){Arrays.sort(files,Comparator.comparingLong(File::lastModified));files[0].delete();}try(FileOutputStream out=new FileOutputStream(cache)){out.write(value.toString().getBytes(StandardCharsets.UTF_8));}}return result;
    }
    private static Lyrics parse(JSONObject o){String plain=o.isNull("plainLyrics")?"":o.optString("plainLyrics");String synced=o.isNull("syncedLyrics")?"":o.optString("syncedLyrics");return new Lyrics(plain,LyricsRules.parse(synced),o.optBoolean("instrumental"));}
    private static String get(String url)throws IOException {HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","RadhaMusic/1.4 (https://github.com/DiwashBhatta17/radha-music)");try{int code=c.getResponseCode();if(code==404)throw new FileNotFoundException();if(code!=200)throw new IOException("Lyrics service returned "+code);try(InputStream in=c.getInputStream()){return read(in);}}finally{c.disconnect();}}
    private static String read(InputStream in)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){if(out.size()+n>512*1024)throw new IOException("Lyrics response too large");out.write(chunk,0,n);}return out.toString("UTF-8");}
}
