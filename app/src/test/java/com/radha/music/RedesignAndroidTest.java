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
   androidx.media3.exoplayer.ExoPlayer player=new androidx.media3.exoplayer.ExoPlayer.Builder(a).build();try{player.setMediaItem(FeatureAndroidTest.song("abcdefghijk",false).item());java.lang.reflect.Field f=MainActivity.class.getDeclaredField("player");f.setAccessible(true);f.set(a,player);java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("renderPlayer");render.setAccessible(true);render.invoke(a);root=a.findViewById(android.R.id.content);capture(root,"ui-player.png");assertNotNull(OnlineScreenAndroidTest.description(root,"Play or pause"));assertNotNull(OnlineScreenAndroidTest.find(root,"☷  Lyrics"));player.setPlayWhenReady(true);assertFalse(player.isPlaying());java.lang.reflect.Method toggle=MainActivity.class.getDeclaredMethod("togglePlay");toggle.setAccessible(true);toggle.invoke(a);assertFalse("A second tap must cancel requested playback, not restart preparation",player.getPlayWhenReady());}finally{player.release();}
  }finally{org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network());}
 }
 private static void capture(View root,String name)throws Exception{OnlineScreenAndroidTest.layout(root);OnlineScreenAndroidTest.layout(root);Bitmap bitmap=Bitmap.createBitmap(393,851,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File("build/reports",name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}
 @Test public void darkThemeAndCompactHeaderSurviveLightDeviceMode()throws Exception{
  Application app=RuntimeEnvironment.getApplication();Shadows.shadowOf(app).declareComponentUnbindable(new android.content.ComponentName(app,PlaybackService.class));OnlineClient.init();org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network(){@Override public org.schabi.newpipe.extractor.downloader.Response execute(org.schabi.newpipe.extractor.downloader.Request r)throws IOException{throw new IOException("offline test");}});
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {MainActivity a=controller.get();View root=a.findViewById(android.R.id.content);OnlineScreenAndroidTest.layout(root);View brand=OnlineScreenAndroidTest.find(root,"Radha Music");assertNotNull(brand);int[] at=new int[2];brand.getLocationOnScreen(at);assertTrue("Header should not have oversized top space",at[1]<80);assertNotNull(OnlineScreenAndroidTest.description(root,"Search"));java.lang.reflect.Field dark=MainActivity.class.getDeclaredField("dark");dark.setAccessible(true);assertTrue(dark.getBoolean(a));}
  finally{org.schabi.newpipe.extractor.NewPipe.init(new OnlineClient.Network());}
 }
}
