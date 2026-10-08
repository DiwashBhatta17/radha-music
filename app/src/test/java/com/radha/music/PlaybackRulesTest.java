package com.radha.music;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackRulesTest {
 @Test public void seekClampsAtBothEnds(){assertEquals(0,PlaybackRules.seek(1000,-6000,90000));assertEquals(90000,PlaybackRules.seek(89000,6000,90000));assertEquals(21000,PlaybackRules.seek(15000,6000,90000));assertEquals(21000,PlaybackRules.seek(15000,6000,-1));}
 @Test public void playNextInsertsImmediatelyAfterCurrent(){assertEquals(3,PlaybackRules.insertNext(2,7));assertEquals(7,PlaybackRules.insertNext(6,7));assertEquals(0,PlaybackRules.insertNext(-1,0));}
 @Test public void doubleAmplitudeUsesSixDecibels(){assertEquals(0,PlaybackRules.gainMillibels(100));assertEquals(602,PlaybackRules.gainMillibels(200));assertEquals(602,PlaybackRules.gainMillibels(300));}
}
