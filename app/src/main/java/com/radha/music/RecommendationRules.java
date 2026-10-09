package com.radha.music;

import java.util.*;

/** Small, on-device taste profile; no YouTube account or remote profile. */
final class RecommendationRules {
    static List<String> queries(List<String> favorites,List<String> recent,String preference,boolean watch){
        LinkedHashSet<String> artists=new LinkedHashSet<>();
        if(!favorites.isEmpty())add(artists,favorites.get(0));
        for(String artist:recent){add(artists,artist);if(artists.size()>=2)break;}
        for(String artist:favorites){if(artists.size()>=2)break;add(artists,artist);}
        add(artists,preference);
        List<String> result=new ArrayList<>();for(String artist:artists)result.add(artist+(watch?" music video":" songs"));return result;
    }
    private static void add(Set<String> values,String text){if(text==null||text.trim().isEmpty()||text.equalsIgnoreCase("Unknown artist"))return;for(String existing:values)if(existing.equalsIgnoreCase(text.trim()))return;values.add(text.trim());}
    static List<OnlineClient.Track> mix(List<List<OnlineClient.Track>> groups){
        LinkedHashMap<String,OnlineClient.Track> out=new LinkedHashMap<>();int longest=0;for(List<?> group:groups)longest=Math.max(longest,group.size());
        for(int row=0;row<longest;row++)for(List<OnlineClient.Track> group:groups)if(row<group.size()){OnlineClient.Track track=group.get(row);String id=OnlineRules.videoId(track.url());out.putIfAbsent(id==null?track.url():id,track);}
        return new ArrayList<>(out.values());
    }
}
