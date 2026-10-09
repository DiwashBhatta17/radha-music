package com.radha.music;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LyricsRulesTest {
 @Test public void parsesMultipleTimestampsFractionsAndOffsets(){List<LyricsRules.Line> lines=LyricsRules.parse("[offset:-100]\n[00:02.50][00:04.005]Hello\n[00:00.09]Start\n[ar:Test]");assertEquals(3,lines.size());assertEquals(0,lines.get(0).time());assertEquals(2400,lines.get(1).time());assertEquals(3905,lines.get(2).time());assertEquals("Hello",lines.get(2).text());}
 @Test public void selectsLineAroundSeekBoundaries(){List<LyricsRules.Line> lines=LyricsRules.parse("[00:01]One\n[00:03]Two");assertEquals(-1,LyricsRules.current(lines,0));assertEquals(0,LyricsRules.current(lines,1000));assertEquals(1,LyricsRules.current(lines,5000));assertEquals(0,LyricsRules.current(lines,1500));}
 @Test public void rejectsDifferentSongOrDuration(){assertTrue(LyricsRules.matches("Home","Artist",200,"Home","Artist",203));assertFalse(LyricsRules.matches("Home","Artist",200,"Home Live","Artist",203));assertFalse(LyricsRules.matches("Home","Artist",200,"Home","Artist",250));}
}
