package com.radha.music;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class OnlineRulesTest {
    @Test public void stableIdsDoNotStoreExpiringLinks(){String url="https://www.youtube.com/watch?v=dQw4w9WgXcQ";assertEquals("radha://youtube/dQw4w9WgXcQ/audio",OnlineRules.mediaId(url,false));assertEquals("dQw4w9WgXcQ",OnlineRules.videoId(OnlineRules.mediaId(url,true)));}
    @Test public void onlyExpectedYoutubeLinksAreAccepted(){assertNull(OnlineRules.videoId("https://youtube.com.evil.test/watch?v=dQw4w9WgXcQ"));assertNull(OnlineRules.videoId("file:///secret"));assertNull(OnlineRules.videoId("radha://youtube/../audio"));assertEquals("dQw4w9WgXcQ",OnlineRules.videoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=hello"));}
    @Test public void preferencesRankActualListeningAndBreakTiesDeterministically(){Map<String,Integer> scores=new HashMap<>();scores.put("B",3);scores.put("A",3);scores.put("C",10);scores.put("",50);assertEquals(Arrays.asList("C","A"),OnlineRules.topArtists(scores,2));assertTrue(OnlineRules.topArtists(Collections.emptyMap(),3).isEmpty());}
}
