package com.radha.music;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DiscoveryRulesTest {
 @Test public void thresholdAndMostPlayedFirstWithRecentTies(){Map<String,DiscoveryRules.Listening> h=new HashMap<>();h.put("two",new DiscoveryRules.Listening(2,900));h.put("three",new DiscoveryRules.Listening(3,900));h.put("older",new DiscoveryRules.Listening(8,100));h.put("newer",new DiscoveryRules.Listening(8,200));assertEquals(Arrays.asList("newer","older","three","two"),DiscoveryRules.speedDials(h));}
 @Test public void thirtyDialsBecomeFivePagesOfSix(){List<Integer> items=new ArrayList<>();for(int i=0;i<37;i++)items.add(i);List<List<Integer>> pages=DiscoveryRules.pages(items,DiscoveryRules.SPEED_PAGE_SIZE,DiscoveryRules.MAX_PAGES);assertEquals(5,pages.size());for(List<Integer> page:pages)assertEquals(6,page.size());assertEquals(Integer.valueOf(29),pages.get(4).get(5));assertEquals(3,DiscoveryRules.COLUMNS);}
 @Test public void quickPicksUseTwentyFiveEntriesInFivePages(){List<Integer> items=new ArrayList<>();for(int i=0;i<29;i++)items.add(i);List<List<Integer>> pages=DiscoveryRules.pages(items,5,5);assertEquals(5,pages.size());assertEquals(Arrays.asList(20,21,22,23,24),pages.get(4));}
 @Test public void forgottenRequiresRepeatListeningAndFourteenDaysAway(){long now=40L*86400000;Map<String,DiscoveryRules.Listening> h=new HashMap<>();h.put("forgotten",new DiscoveryRules.Listening(5,now-15L*86400000));h.put("recent",new DiscoveryRules.Listening(8,now-86400000));h.put("once",new DiscoveryRules.Listening(1,0));assertEquals(Collections.singletonList("forgotten"),DiscoveryRules.forgotten(h,now));}
}
