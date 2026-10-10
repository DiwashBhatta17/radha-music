package com.radha.music;
import android.app.*;
import android.os.Looper;
import android.view.*;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w393dp-h851dp-mdpi") @LooperMode(LooperMode.Mode.PAUSED)
public class PlayerFlowAndroidTest {
 @Test public void startupWaitsPastOldTimeoutAndUntilHomeRendered()throws Exception{
  CountDownLatch release=new CountDownLatch(1);AtomicBoolean rendered=new AtomicBoolean(false);
  SessionCatalog catalog=new SessionCatalog(new SessionCatalog.Backend(){public OnlineClient.Results search(String q,boolean w)throws Exception{release.await();return new OnlineClient.Results(Collections.emptyList(),null,null,false);}public OnlineClient.Results more(OnlineClient.Results p){throw new AssertionError();}});
  try(var activity=Robolectric.buildActivity(Activity.class).setup()){
   catalog.warm(new OnlineStore(activity.get()));StartupDialog dialog=new StartupDialog(activity.get(),rendered::get,catalog);dialog.show();assertNull(OnlineScreenAndroidTest.find(dialog.getWindow().getDecorView(),"Open local music"));Shadows.shadowOf(Looper.getMainLooper()).idleFor(20,TimeUnit.SECONDS);assertTrue(dialog.isShowing());release.countDown();for(int i=0;i<100&&!catalog.startupReady();i++)Thread.sleep(10);assertTrue(catalog.startupReady());Shadows.shadowOf(Looper.getMainLooper()).idleFor(1,TimeUnit.SECONDS);assertTrue("Do not reveal Home before it has rendered",dialog.isShowing());rendered.set(true);Shadows.shadowOf(Looper.getMainLooper()).idleFor(1,TimeUnit.SECONDS);assertFalse(dialog.isShowing());
  }finally{release.countDown();}
 }
 @Test public void queueFollowsActualOrderAndSupportsRemoval()throws Exception{
  try(var activity=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=activity.get();ExoPlayer player=new ExoPlayer.Builder(a).build();ArtworkStore art=new ArtworkStore(a);QueueDialog dialog=null;
   try{player.setMediaItems(Arrays.asList(FeatureAndroidTest.song("abcdefghijk",false).item(),FeatureAndroidTest.song("bcdefghijkl",false).item(),FeatureAndroidTest.song("cdefghijklm",false).item()));assertEquals(Arrays.asList(1,2),QueueDialog.upcoming(player));dialog=new QueueDialog(a,player,art);dialog.show();View root=dialog.getWindow().getDecorView();OnlineScreenAndroidTest.layout(root);assertNotNull(OnlineScreenAndroidTest.find(root,"Next in queue"));assertNotNull(OnlineScreenAndroidTest.find(root,"bcdefghijkl"));player.moveMediaItem(2,1);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("cdefghijklm",player.getMediaItemAt(1).mediaMetadata.title);player.removeMediaItem(1);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(Collections.singletonList(1),QueueDialog.upcoming(player));assertNotNull(OnlineScreenAndroidTest.find(root,"Created, designed and developed by Diwash Bhatta"));}
   finally{if(dialog!=null)dialog.dismiss();art.close();player.release();}
  }
 }
}
