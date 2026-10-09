package com.radha.music;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LyricsAvailabilityTest {
 @Test public void onlyUsableLyricsEnableNavigation(){assertFalse(LyricsAvailability.usable(new LyricsClient.Lyrics("",Collections.emptyList(),false)));assertFalse(LyricsAvailability.usable(new LyricsClient.Lyrics("Instrumental",Collections.emptyList(),true)));assertTrue(LyricsAvailability.usable(new LyricsClient.Lyrics("Some words",Collections.emptyList(),false)));}
 @Test public void checkingDeduplicatesAndMissingStaysDisabled(){LyricsAvailability cache=new LyricsAvailability();assertTrue(cache.begin("a",0));assertFalse(cache.begin("a",10));cache.finish("a",new LyricsClient.Lyrics("",Collections.emptyList(),false),20);assertEquals(LyricsAvailability.State.MISSING,cache.state("a",600000));}
 @Test public void networkFailureCanRetryWithoutClaimingNoLyrics(){LyricsAvailability cache=new LyricsAvailability();cache.begin("a",0);cache.failed("a",10);assertEquals(LyricsAvailability.State.FAILED,cache.state("a",20));assertTrue(cache.begin("a",60011));cache.finish("a",new LyricsClient.Lyrics("Words",Collections.emptyList(),false),60012);assertEquals(LyricsAvailability.State.AVAILABLE,cache.state("a",60013));}
}
