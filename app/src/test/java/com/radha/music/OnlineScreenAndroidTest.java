package com.radha.music;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.UnknownHostException;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w393dp-h851dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
public class OnlineScreenAndroidTest {
 private static final OnlineScreen.Host HOST=new OnlineScreen.Host(){
  public void play(MediaEntry e,List<MediaEntry> q){} public void menu(MediaEntry e){}
  public void image(MediaEntry e,ImageView v){} public void preferences(){}
 };
 @Test public void openingAppStartsYoutubeRequestAndExposesFailure() throws Exception {
  Shadows.shadowOf(RuntimeEnvironment.getApplication()).declareComponentUnbindable(new android.content.ComponentName(RuntimeEnvironment.getApplication(),PlaybackService.class));
  CountDownLatch requested=new CountDownLatch(1);
  OnlineClient.init();
  NewPipe.init(new OnlineClient.Network(){@Override public Response execute(Request r)throws java.io.IOException {requested.countDown();throw new UnknownHostException("Test offline");}});
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
   MainActivity activity=controller.get();
   assertTrue("Online did not start a request",requested.await(8,TimeUnit.SECONDS));
   View root=activity.findViewById(android.R.id.content);
   awaitText(root,"Error details");layout(root);
   assertVisible(root,"Retry search");assertVisible(root,"Error details");assertNotNull(description(root,"Search"));
  }finally{NewPipe.init(new OnlineClient.Network());}
 }
 @Test public void automaticResultsAreVisibleAndSearchReplacesThem()throws Exception {
  AtomicInteger calls=new AtomicInteger();
  OnlineScreen.SearchSource source=new OnlineScreen.SearchSource(){
   public OnlineClient.Results search(String q,boolean watch){return result(calls.incrementAndGet()==1?"First song":"Searched song");}
   public OnlineClient.Results more(OnlineClient.Results previous){throw new AssertionError();}
  };
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   Activity a=controller.get();OnlineScreen screen=new OnlineScreen(a,false,false,new OnlineStore(a),HOST,source);
   a.setContentView(screen);
   try{awaitText(screen,"First song");layout(screen);assertVisible(screen,"First song");
    screen.search("Artist");awaitText(screen,"Searched song");layout(screen);assertVisible(screen,"Searched song");assertNull(find(screen,"First song"));
   }finally{screen.close();}
  }
 }
 @Test public void retryRecoversFromFailureWithoutReopeningScreen()throws Exception {
  AtomicInteger calls=new AtomicInteger();
  OnlineScreen.SearchSource source=new OnlineScreen.SearchSource(){
   public OnlineClient.Results search(String q,boolean watch)throws Exception{if(calls.incrementAndGet()==1)throw new UnknownHostException("Test");return result("Recovered song");}
   public OnlineClient.Results more(OnlineClient.Results previous){throw new AssertionError();}
  };
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   Activity a=controller.get();OnlineScreen screen=new OnlineScreen(a,false,true,new OnlineStore(a),HOST,source);a.setContentView(screen);
   try{awaitText(screen,"Retry search");layout(screen);assertVisible(screen,"Error details");find(screen,"Retry search").performClick();awaitText(screen,"Recovered song");assertNull(find(screen,"Error details"));}
   finally{screen.close();}
  }
 }
 @Test public void emptyResponseIsExplicit()throws Exception {
  OnlineScreen.SearchSource source=new OnlineScreen.SearchSource(){public OnlineClient.Results search(String q,boolean watch){return new OnlineClient.Results(Collections.emptyList(),null,null,false);}public OnlineClient.Results more(OnlineClient.Results previous){throw new AssertionError();}};
  try(var controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();OnlineScreen screen=new OnlineScreen(a,true,false,new OnlineStore(a),HOST,source);a.setContentView(screen);
   try{awaitText(screen,"No results. Try a different artist or title.");layout(screen);assertVisible(screen,"No results. Try a different artist or title.");}finally{screen.close();}
  }
 }
 @Test public void speedDialGridHasThreeColumnsAndFiveSwipePages()throws Exception {
  try(var controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=controller.get();OnlineStore store=new OnlineStore(a);for(int i=0;i<30;i++){String id=String.format(java.util.Locale.US,"song%07d",i);MediaEntry e=new MediaEntry("radha://youtube/"+id+"/audio","Dial "+i,"YouTube","Artist",false,100000,1);for(int j=0;j<3;j++)store.listened(e);}
   OnlineScreen.SearchSource source=new OnlineScreen.SearchSource(){public OnlineClient.Results search(String q,boolean w){return result("Quick song");}public OnlineClient.Results more(OnlineClient.Results previous){throw new AssertionError();}};
   OnlineScreen screen=new OnlineScreen(a,false,false,store,HOST,source);a.setContentView(screen);
   try{awaitText(screen,"Quick song");layout(screen);View carousel=description(screen,"Swipe left or right through 5 pages");assertNotNull(carousel);ViewGroup pages=(ViewGroup)((ViewGroup)carousel).getChildAt(0);assertEquals(5,pages.getChildCount());ViewGroup first=(ViewGroup)pages.getChildAt(0);assertEquals(2,first.getChildCount());assertEquals(3,((ViewGroup)first.getChildAt(0)).getChildCount());assertEquals(3,((ViewGroup)first.getChildAt(1)).getChildCount());layout(screen);ViewGroup row=(ViewGroup)first.getChildAt(0);assertTrue(row.getChildAt(0).getWidth()>0);assertEquals(row.getChildAt(0).getWidth(),row.getChildAt(2).getWidth(),1);assertTrue(row.getChildAt(2).getRight()<=carousel.getWidth());}finally{screen.close();}
  }
 }
 static View description(View v,String value){if(value.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){View result=description(g.getChildAt(i),value);if(result!=null)return result;}return null;}
 static OnlineClient.Results result(String name){return new OnlineClient.Results(Collections.singletonList(new OnlineClient.Track("https://www.youtube.com/watch?v=abcdefghijk",name,"Test artist","",123)),null,null,false);}
 static void awaitText(View root,String value)throws Exception{for(int i=0;i<200;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(find(root,value)!=null)return;Thread.sleep(10);}fail("Missing UI text: "+value);}
 static void layout(View root){root.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(851,View.MeasureSpec.EXACTLY));root.layout(0,0,393,851);}
 static void assertVisible(View root,String value){View v=find(root,value);assertNotNull(value,v);Rect rect=new Rect();assertTrue(value+" is outside the viewport",v.getGlobalVisibleRect(rect)&&rect.height()>0&&rect.width()>0);}
 static View find(View v,String value){if(v instanceof TextView t&&t.getText().toString().equals(value))return v;if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){View r=find(g.getChildAt(i),value);if(r!=null)return r;}return null;}
}
