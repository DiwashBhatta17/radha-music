package com.radha.music;

import java.util.*;

public final class DiscoveryRules {
    private DiscoveryRules() {}
    public static final int COLUMNS=3, SPEED_PAGE_SIZE=6, MAX_PAGES=5, PICKS_PAGE_SIZE=5;
    public record Listening(int plays,long lastPlayed) {}
    public static List<String> speedDials(Map<String,Listening> history){
        List<String> ids=new ArrayList<>();for(var e:history.entrySet())if(e.getValue().plays()>=1)ids.add(e.getKey());
        ids.sort(Comparator.<String>comparingInt(id->history.get(id).plays()).reversed().thenComparing(Comparator.<String>comparingLong(id->history.get(id).lastPlayed()).reversed()));
        return new ArrayList<>(ids.subList(0,Math.min(ids.size(),SPEED_PAGE_SIZE*MAX_PAGES)));
    }
    public static List<String> forgotten(Map<String,Listening> history,long now){
        List<String> ids=new ArrayList<>();for(var e:history.entrySet())if(e.getValue().plays()>=2&&now-e.getValue().lastPlayed()>=14L*24*60*60*1000)ids.add(e.getKey());
        ids.sort(Comparator.<String>comparingInt(id->history.get(id).plays()).reversed());return new ArrayList<>(ids.subList(0,Math.min(20,ids.size())));
    }
    public static <T> List<List<T>> pages(List<T> items,int size,int max){
        List<List<T>> out=new ArrayList<>();for(int start=0;start<items.size()&&out.size()<max;start+=size)out.add(new ArrayList<>(items.subList(start,Math.min(items.size(),start+size))));return out;
    }
}
