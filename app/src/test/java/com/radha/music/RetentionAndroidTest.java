package com.radha.music;
import android.app.Application;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class RetentionAndroidTest {
 @Test public void oldHistoryExpiresButTasteAndSavedSongsRemain()throws Exception{
  Application c=RuntimeEnvironment.getApplication();OnlineStore store=new OnlineStore(c);MediaEntry song=FeatureAndroidTest.song("abcdefghijk",false);for(int i=0;i<4;i++)store.listened(song);store.toggleSaved(song);
  long now=System.currentTimeMillis();JSONObject old=new JSONObject().put(song.id,new JSONObject().put("plays",4).put("last",now-16L*86400000));c.getSharedPreferences("online",0).edit().putString("trackHistory",old.toString()).commit();store.prune(now);
  assertTrue(store.history().isEmpty());assertTrue(store.recent().isEmpty());assertEquals(song.id,store.speedDials(false).get(0).id);assertEquals(song.id,store.saved().get(0).id);assertEquals("Artist",store.artists().get(0));store.listened(song);assertEquals(5,store.history().get(song.id).plays());
 }
 @Test public void freshHistoryRemains(){OnlineStore store=new OnlineStore(RuntimeEnvironment.getApplication());store.listened(FeatureAndroidTest.song("abcdefghijk",false));store.prune(System.currentTimeMillis());assertEquals(1,store.recent().size());}
 @Test public void lyricCleanupRemovesOnlyExpiredCache()throws Exception{
  Application c=RuntimeEnvironment.getApplication();File dir=new File(c.getFilesDir(),"lyrics");dir.mkdirs();File old=new File(dir,"old.json"),fresh=new File(dir,"fresh.json");try(FileOutputStream out=new FileOutputStream(old)){out.write(1);}try(FileOutputStream out=new FileOutputStream(fresh)){out.write(2);}assertTrue(old.setLastModified(System.currentTimeMillis()-16L*86400000));File downloads=new File(c.getFilesDir(),"downloads");downloads.mkdirs();File song=new File(downloads,"saved.audio");try(FileOutputStream out=new FileOutputStream(song)){out.write(3);}LyricsClient.trimCache(c);assertFalse(old.exists());assertTrue(fresh.exists());assertTrue(song.exists());
 }
}
