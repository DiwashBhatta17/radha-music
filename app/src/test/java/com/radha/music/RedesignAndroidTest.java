package com.radha.music;
import android.app.*;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w393dp-h851dp-mdpi") @LooperMode(LooperMode.Mode.PAUSED) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RedesignAndroidTest {
 @Test public void homeHasStarterDialsAndArtworkDiscoveryWithoutLibraryManagement()throws Exception{
  try(var controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();List<OnlineClient.Track> tracks=new ArrayList<>();for(int i=0;i<30;i++)tracks.add(new OnlineClient.Track("https://www.youtube.com/watch?v="+String.format(Locale.US,"song%07d",i),"Song "+(i+1),"Artist "+i,"",180));
   OnlineScreen.SearchSource source=new OnlineScreen.SearchSource(){public OnlineClient.Results search(String q,boolean w){return new OnlineClient.Results(tracks,null,null,false);}public OnlineClient.Results more(OnlineClient.Results p){throw new AssertionError();}};
   OnlineScreen screen=new OnlineScreen(a,false,false,new OnlineStore(a),new OnlineScreen.Host(){public void play(MediaEntry e,List<MediaEntry> q){}public void menu(MediaEntry e){}public void preferences(){}public void image(MediaEntry e,ImageView target){target.setBackgroundColor(0xFF343D48);}},source);screen.setBackgroundColor(0xFF101113);a.setContentView(screen);
   try{OnlineScreenAndroidTest.awaitText(screen,"Song 1");OnlineScreenAndroidTest.layout(screen);OnlineScreenAndroidTest.layout(screen);assertNotNull(OnlineScreenAndroidTest.find(screen,"Discover more"));assertNull(OnlineScreenAndroidTest.find(screen,"Your playlists"));assertNull(OnlineScreenAndroidTest.find(screen,"Made for you"));assertNotNull(OnlineScreenAndroidTest.description(screen,"Swipe left or right through 5 pages"));Bitmap bitmap=Bitmap.createBitmap(393,851,Bitmap.Config.ARGB_8888);screen.draw(new Canvas(bitmap));File file=new File("build/reports/ui-home.png");file.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}assertEquals(0xFF101113,bitmap.getPixel(0,0));}finally{screen.close();}
  }
 }
 @Test public void savedTabsAndPlayerLayoutRenderWithoutLosingControls()throws Exception{
  Application app=RuntimeEnvironment.getApplication();Shadows.shadowOf(app).declareComponentUnbindable(new android.content.ComponentName(app,PlaybackService.class));OnlineClient.init();org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network(){@Override public org.schabi.newpipe.extractor.downloader.Response execute(org.schabi.newpipe.extractor.downloader.Request r)throws IOException{throw new IOException("offline test");}});
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity a=controller.get();java.lang.reflect.Field section=MainActivity.class.getDeclaredField("section");section.setAccessible(true);section.set(a,"Saved");java.lang.reflect.Method build=MainActivity.class.getDeclaredMethod("buildLibrary");build.setAccessible(true);build.invoke(a);View root=a.findViewById(android.R.id.content);OnlineScreenAndroidTest.layout(root);assertNotNull(OnlineScreenAndroidTest.find(root,"Playlists"));OnlineScreenAndroidTest.find(root,"Downloads").performClick();assertNull(OnlineScreenAndroidTest.find(root,"Your playlists"));OnlineScreenAndroidTest.find(root,"Library").performClick();assertNotNull(OnlineScreenAndroidTest.find(root,"Your playlists"));capture(root,"ui-saved.png");
   androidx.media3.exoplayer.ExoPlayer player=new androidx.media3.exoplayer.ExoPlayer.Builder(a).build();try{player.setMediaItem(FeatureAndroidTest.song("abcdefghijk",false).item());java.lang.reflect.Field f=MainActivity.class.getDeclaredField("player");f.setAccessible(true);f.set(a,player);java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("renderPlayer");render.setAccessible(true);render.invoke(a);root=a.findViewById(android.R.id.content);capture(root,"ui-player.png");assertNotNull(OnlineScreenAndroidTest.description(root,"Play or pause"));assertNotNull(OnlineScreenAndroidTest.description(root,"Lyrics"));assertFalse(OnlineScreenAndroidTest.description(root,"Lyrics").isEnabled());assertNotNull(OnlineScreenAndroidTest.description(root,"Download song"));player.setPlayWhenReady(true);assertFalse(player.isPlaying());java.lang.reflect.Method toggle=MainActivity.class.getDeclaredMethod("togglePlay");toggle.setAccessible(true);toggle.invoke(a);assertFalse("A second tap must cancel requested playback, not restart preparation",player.getPlayWhenReady());}finally{player.release();}
  }finally{org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network());}
 }
 private static void capture(View root,String name)throws Exception{OnlineScreenAndroidTest.layout(root);OnlineScreenAndroidTest.layout(root);Bitmap bitmap=Bitmap.createBitmap(393,851,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File("build/reports",name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}
 @Test public void lyricsTrackerAndQueueMatchFixedBottomLayout()throws Exception{
  try(var activity=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=activity.get();androidx.media3.exoplayer.ExoPlayer player=new androidx.media3.exoplayer.ExoPlayer.Builder(a).build();ArtworkStore art=new ArtworkStore(a);LyricsDialog lyrics=null;QueueDialog queue=null;
   try{player.setMediaItems(Arrays.asList(FeatureAndroidTest.song("abcdefghijk",false).item(),FeatureAndroidTest.song("bcdefghijkl",false).item()));String key=java.util.UUID.nameUUIDFromBytes("abcdefghijk\nArtist\n0".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();File dir=new File(a.getFilesDir(),"lyrics");dir.mkdirs();org.json.JSONObject data=new org.json.JSONObject().put("plainLyrics","The city settles into blue").put("syncedLyrics","[00:00]The city settles into blue\n[00:10]A quiet road leads back to you\n[00:20]We carry sunlight through the rain");try(FileOutputStream out=new FileOutputStream(new File(dir,key+".json"))){out.write(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    lyrics=new LyricsDialog(a,player);lyrics.show();View root=lyrics.getWindow().getDecorView();OnlineScreenAndroidTest.awaitText(root,"The city settles into blue");capture(root,"ui-lyrics.png");View tracker=OnlineScreenAndroidTest.description(root,"Song progress. Drag to seek");assertNotNull(tracker);android.graphics.Rect rect=new android.graphics.Rect();assertTrue(tracker.getGlobalVisibleRect(rect));assertTrue(rect.bottom<=851);assertTrue(((android.widget.TextView)OnlineScreenAndroidTest.find(root,"The city settles into blue")).getTextSize()<=22);assertNotNull(OnlineScreenAndroidTest.find(root,"Created, designed and developed by Diwash Bhatta"));lyrics.dismiss();lyrics=null;
    queue=new QueueDialog(a,player,art);queue.show();capture(queue.getWindow().getDecorView(),"ui-queue.png");assertNotNull(OnlineScreenAndroidTest.find(queue.getWindow().getDecorView(),"Next in queue"));
   }finally{if(lyrics!=null)lyrics.dismiss();if(queue!=null)queue.dismiss();art.close();player.release();}
  }
 }
 @Test public void darkThemeAndCompactHeaderSurviveLightDeviceMode()throws Exception{
  Application app=RuntimeEnvironment.getApplication();Shadows.shadowOf(app).declareComponentUnbindable(new android.content.ComponentName(app,PlaybackService.class));OnlineClient.init();org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network(){@Override public org.schabi.newpipe.extractor.downloader.Response execute(org.schabi.newpipe.extractor.downloader.Request r)throws IOException{throw new IOException("offline test");}});
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {MainActivity a=controller.get();View root=a.findViewById(android.R.id.content);OnlineScreenAndroidTest.layout(root);View brand=OnlineScreenAndroidTest.find(root,"Radha Music");assertNotNull(brand);int[] at=new int[2];brand.getLocationOnScreen(at);assertTrue("Header should not have oversized top space",at[1]<80);assertNotNull(OnlineScreenAndroidTest.description(root,"Search"));java.lang.reflect.Field dark=MainActivity.class.getDeclaredField("dark");dark.setAccessible(true);assertTrue(dark.getBoolean(a));}
  finally{org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network());}
 }
}
