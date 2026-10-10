package com.radha.music;
import android.app.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @LooperMode(LooperMode.Mode.PAUSED)
public class TasteAndroidTest {
 @Test public void firstQualifiedSearchPersistsAndOldPlaysDoNotQualify(){OnlineStore store=new OnlineStore(RuntimeEnvironment.getApplication());MediaEntry home=FeatureAndroidTest.song("abcdefghijk",false),searched=FeatureAndroidTest.song("bcdefghijkl",false);for(int i=0;i<5;i++)store.listened(home);assertTrue(store.speedDials(false).isEmpty());store.completedSearch(searched);OnlineStore restored=new OnlineStore(RuntimeEnvironment.getApplication());assertEquals(1,restored.speedDials(false).size());assertEquals(searched.id,restored.speedDials(false).get(0).id);}
 @Test public void partyAndRelatedLoveTracksSurviveRestart(){OnlineStore store=new OnlineStore(RuntimeEnvironment.getApplication());store.learnContext("indian party songs");store.learnContext("die with a smile");MediaEntry seed=FeatureAndroidTest.song("abcdefghijk",false),related=FeatureAndroidTest.song("bcdefghijkl",false);store.learnRadio(seed,Collections.singletonList(related));OnlineStore restored=new OnlineStore(RuntimeEnvironment.getApplication());assertEquals("indian party songs",restored.recommendationQueries(false).get(0));assertEquals(1,restored.tasteTracks(false).size());assertEquals(related.id,OnlineRules.mediaId(restored.tasteTracks(false).get(0).url(),false));restored.clearHistory();assertTrue(restored.tasteTracks(false).isEmpty());}
 @Test public void homeAndBrowseAreNotExplicitSearchAndEmptySectionsAreAbsent()throws Exception{
  try(var controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();List<Boolean> origins=new ArrayList<>();OnlineStore store=new OnlineStore(activity);MediaEntry entry=FeatureAndroidTest.song("abcdefghijk",false);
   OnlineScreen.Host host=new OnlineScreen.Host(){public void play(MediaEntry e,List<MediaEntry> q){fail("Catalog list must not become the queue");}public void playOnline(MediaEntry e,String q,boolean searched){origins.add(searched);}public void menu(MediaEntry e){}public void image(MediaEntry e,ImageView v){}public void preferences(){}};
   OnlineScreen.SearchSource source=new OnlineScreen.SearchSource(){public OnlineClient.Results search(String q,boolean w){return new OnlineClient.Results(Collections.singletonList(new OnlineClient.Track("https://www.youtube.com/watch?v=abcdefghijk","Example","Artist","",200)),null,null,false);}public OnlineClient.Results more(OnlineClient.Results p){throw new AssertionError();}};
   OnlineScreen screen=new OnlineScreen(activity,false,true,store,host,source);activity.setContentView(screen);
   try{OnlineScreenAndroidTest.awaitText(screen,"Example");assertNull(OnlineScreenAndroidTest.find(screen,"Favorites"));assertNull(OnlineScreenAndroidTest.find(screen,"Forgotten Favorites"));assertNull(OnlineScreenAndroidTest.find(screen,"Acoustic moments"));
    java.lang.reflect.Method row=OnlineScreen.class.getDeclaredMethod("trackRow",MediaEntry.class);row.setAccessible(true);((View)row.invoke(screen,entry)).performClick();screen.search("Example");((View)row.invoke(screen,entry)).performClick();screen.setExplicitSearch(false);((View)row.invoke(screen,entry)).performClick();assertEquals(Arrays.asList(false,true,false),origins);
    LibraryStore library=new LibraryStore(activity);library.favorites.add(entry.id);library.save();store.pin(entry);screen.showHome();OnlineScreenAndroidTest.awaitText(screen,"Favorites");assertNotNull(OnlineScreenAndroidTest.find(screen,"Favorites"));library.favorites.clear();library.save();screen.refreshHome();assertNull(OnlineScreenAndroidTest.find(screen,"Favorites"));
   }finally{screen.close();}
  }
 }
}
