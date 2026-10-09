package com.radha.music;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class RecommendationRulesTest {
 @Test public void actualListeningAndRecentTasteLeadPreferences(){assertEquals(Arrays.asList("Favorite songs","New artist songs","Nepali songs"),RecommendationRules.queries(Arrays.asList("Favorite","Other"),Arrays.asList("Favorite","New artist"),"Nepali",false));}
 @Test public void newUserFallsBackToPreference(){assertEquals(Collections.singletonList("Hindi music video"),RecommendationRules.queries(Collections.emptyList(),Collections.emptyList(),"Hindi",true));}
 @Test public void mixAlternatesSourcesAndRemovesDuplicateVideos(){OnlineClient.Track a=t("abcdefghijk"),b=t("bcdefghijkl"),c=t("cdefghijklm");assertEquals(Arrays.asList(a,c,b),RecommendationRules.mix(Arrays.asList(Arrays.asList(a,b),Arrays.asList(c,a))));}
 @Test public void failedArtistDoesNotHideOtherRecommendations()throws Exception{SessionCatalog catalog=new SessionCatalog(new SessionCatalog.Backend(){public OnlineClient.Results search(String q,boolean w)throws Exception{if(q.equals("bad"))throw new java.io.IOException();return new OnlineClient.Results(Collections.singletonList(t("abcdefghijk")),null,null,false);}public OnlineClient.Results more(OnlineClient.Results p){throw new AssertionError();}});assertEquals(1,catalog.recommend(Arrays.asList("bad","good"),false).tracks().size());}
 private static OnlineClient.Track t(String id){return new OnlineClient.Track("https://www.youtube.com/watch?v="+id,id,"Artist","",200);}
}
