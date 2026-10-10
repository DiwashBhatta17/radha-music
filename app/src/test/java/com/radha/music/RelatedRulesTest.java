package com.radha.music;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class RelatedRulesTest {
    private OnlineClient.Track track(String id,String title,String artist){return new OnlineClient.Track("https://www.youtube.com/watch?v="+id,title,artist,"",200);}
    @Test public void coversStayInSearchButNotRadio(){
        MediaEntry seed=new MediaEntry("radha://youtube/abcdefghijk/audio","Die With A Smile","YouTube","Lady Gaga",false,200000,1);
        List<OnlineClient.Track> search=Arrays.asList(track("abcdefghijk","Die With A Smile","Lady Gaga"),track("bcdefghijkl","Die With A Smile (Official Video)","Bruno Mars"),track("cdefghijklm","Die With A Smile - Acoustic Cover","Singer"),track("defghijklmn","Perfect","Ed Sheeran"),track("efghijklmno","All of Me","John Legend"));
        List<OnlineClient.Track> queue=RelatedRules.diversify(seed,search);assertEquals(5,search.size());assertEquals(2,queue.size());assertEquals("Perfect",queue.get(0).name());
    }
    @Test public void partyIntentAndQueueVariety(){assertTrue(RelatedRules.moodQuery("indian party songs"));assertFalse(RelatedRules.moodQuery("die with a smile"));MediaEntry seed=new MediaEntry("radha://youtube/abcdefghijk/audio","Kala Chashma","YouTube","Artist",false,200000,1);List<OnlineClient.Track> queue=RelatedRules.diversify(seed,Arrays.asList(track("bcdefghijkl","Kala Chashma (Audio)","Other"),track("cdefghijklm","London Thumakda","A"),track("defghijklmn","Kar Gayi Chull","B")));assertEquals(2,queue.size());}
    @Test public void completeListenQualifiesOnce(){CompletedListen listen=new CompletedListen();for(long p=0;p<=200000;p+=1000)listen.position(p,true);assertTrue(listen.finish(200000));assertFalse(listen.finish(200000));}
    @Test public void seekToEndDoesNotQualify(){CompletedListen listen=new CompletedListen();for(long p=0;p<=30000;p+=1000)listen.position(p,true);listen.discontinuity();for(long p=195000;p<=200000;p+=1000)listen.position(p,true);assertFalse(listen.finish(200000));}
    @Test public void ReplayingSameHalfDoesNotQualify(){CompletedListen listen=new CompletedListen();for(int i=0;i<3;i++){listen.discontinuity();for(long p=0;p<=100000;p+=1000)listen.position(p,true);}assertFalse(listen.finish(200000));}
    @Test public void pauseResumePreservesRealCoverage(){CompletedListen listen=new CompletedListen();for(long p=0;p<=100000;p+=1000)listen.position(p,true);listen.position(100000,false);for(long p=100000;p<=200000;p+=1000)listen.position(p,true);assertTrue(listen.finish(200000));}
}
