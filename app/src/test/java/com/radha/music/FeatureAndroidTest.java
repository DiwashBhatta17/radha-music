package com.radha.music;
import android.app.*;
import android.content.pm.ActivityInfo;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MergingMediaSource;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @LooperMode(LooperMode.Mode.PAUSED)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class FeatureAndroidTest {
 static MediaEntry song(String id,boolean video){return new MediaEntry("radha://youtube/"+id+(video?"/video":"/audio"),id,"YouTube","Artist",video,200000,1);}
 @Test public void listeningCountsPersistAndOnlyEligibleSongsAppear(){Application c=RuntimeEnvironment.getApplication();OnlineStore s=new OnlineStore(c);MediaEntry a=song("abcdefghijk",false),b=song("bcdefghijkl",false);s.listened(a);s.listened(a);assertTrue(s.speedDials(false).isEmpty());s.completedSearch(a);for(int i=0;i<4;i++)s.completedSearch(b);OnlineStore restored=new OnlineStore(c);assertEquals(b.id,restored.speedDials(false).get(0).id);assertEquals(a.id,restored.speedDials(false).get(1).id);restored.clearHistory();assertTrue(restored.speedDials(false).isEmpty());}
 @Test public void downloadRequiresAllFilesAndOfflineFactoryUsesSavedMedia()throws Exception {Application c=RuntimeEnvironment.getApplication();DownloadStore d=new DownloadStore(c);MediaEntry e=song("abcdefghijk",true);d.state(e,"Downloading");try(FileOutputStream out=new FileOutputStream(d.file(e.id,"audio"))){out.write(1);}assertNull(d.files(e.id));try(FileOutputStream out=new FileOutputStream(d.file(e.id,"video"))){out.write(2);}d.complete(e,true,true);assertNotNull(new DownloadStore(c).files(e.id));assertTrue(d.file(e.id,"audio").getCanonicalPath().startsWith(c.getFilesDir().getCanonicalPath()+File.separator));assertTrue(new OnlineMediaSourceFactory(c).createMediaSource(e.item()) instanceof MergingMediaSource);d.file(e.id,"video").delete();assertNull(d.files(e.id));d.remove(e.id);assertFalse(d.file(e.id,"audio").exists());assertTrue(d.entries().isEmpty());}
 @Test public void videoDimensionsAndFullscreenDoNotOverrideManualRotation()throws Exception {
  Application c=RuntimeEnvironment.getApplication();Shadows.shadowOf(c).declareComponentUnbindable(new android.content.ComponentName(c,PlaybackService.class));OnlineClient.init();org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network(){@Override public org.schabi.newpipe.extractor.downloader.Response execute(org.schabi.newpipe.extractor.downloader.Request r)throws IOException{throw new IOException("offline test");}});
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {MainActivity a=controller.get();ExoPlayer player=new ExoPlayer.Builder(a).build();try{player.setMediaItem(song("abcdefghijk",true).item());Field f=MainActivity.class.getDeclaredField("player");f.setAccessible(true);f.set(a,player);invoke(a,"renderPlayer");Field bufferField=MainActivity.class.getDeclaredField("buffering");bufferField.setAccessible(true);android.view.View buffer=(android.view.View)bufferField.get(a);assertEquals(android.view.Gravity.CENTER,((android.widget.FrameLayout.LayoutParams)buffer.getLayoutParams()).gravity);assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,a.getRequestedOrientation());Method sync=MainActivity.class.getDeclaredMethod("syncVideoOrientation",VideoSize.class);sync.setAccessible(true);sync.invoke(a,new VideoSize(1920,1080));assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,a.getRequestedOrientation());invoke(a,"toggleFullscreen");assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,a.getRequestedOrientation());invoke(a,"rotateVideo");sync.invoke(a,new VideoSize(1080,1920));assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,a.getRequestedOrientation());invoke(a,"rotateVideo");assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,a.getRequestedOrientation());}finally{player.release();}}
  finally{org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network());}
 }
 @Test public void libraryActionWorksAndBackupControlsAreOnlyInSettings()throws Exception {
  Application c=RuntimeEnvironment.getApplication();Shadows.shadowOf(c).declareComponentUnbindable(new android.content.ComponentName(c,PlaybackService.class));OnlineClient.init();org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network(){@Override public org.schabi.newpipe.extractor.downloader.Response execute(org.schabi.newpipe.extractor.downloader.Request r)throws IOException{throw new IOException("offline test");}});
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();android.view.View root=a.findViewById(android.R.id.content);assertNull(OnlineScreenAndroidTest.find(root,"Export backup"));assertNull(OnlineScreenAndroidTest.find(root,"Import backup"));invoke(a,"libraryMenu");android.app.AlertDialog settings=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals("Export backup",settings.getListView().getAdapter().getItem(4));assertEquals("Import backup",settings.getListView().getAdapter().getItem(5));settings.dismiss();
   MediaEntry entry=song("abcdefghijk",false);Method menu=MainActivity.class.getDeclaredMethod("mediaMenu",MediaEntry.class);menu.setAccessible(true);menu.invoke(a,entry);android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.widget.ListView list=dialog.getListView();int index=-1;for(int i=0;i<list.getAdapter().getCount();i++)if("Add to library".equals(list.getAdapter().getItem(i)))index=i;assertTrue(index>=0);list.performItemClick(null,index,index);assertTrue(new OnlineStore(c).isSaved(entry.id));dialog.dismiss();
  }finally{org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network());}
 }
 private static void invoke(MainActivity a,String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
}
