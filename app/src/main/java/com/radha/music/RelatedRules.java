package com.radha.music;

import java.util.*;

/** Search remains untouched; only the automatic radio queue is diversified. */
final class RelatedRules {
    static boolean moodQuery(String query){return query!=null&&query.toLowerCase(Locale.ROOT).matches(".*\\b(party|dance|workout|chill|romantic|sad|sleep|meditation|bhajan|devotional|focus|relaxing|acoustic|road trip)\\b.*");}
    private static String words(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();}
    private static String title(String name,String artist){String value=name.replaceAll("\\([^)]*\\)|\\[[^]]*\\]", " ");value=words(value);String who=words(artist).replace(" topic", "");if(!who.isEmpty())value=value.replace(who, " ");return value.replaceAll("\\b(official|music|video|audio|lyrics|lyric|hd|4k|cover|version|remastered|remaster)\\b", " ").replaceAll("\\s+"," ").trim();}
    static boolean sameSong(String left,String leftArtist,String right,String rightArtist){
        String a=title(left,leftArtist),b=title(right,rightArtist);if(a.isEmpty()||b.isEmpty())return false;
        if(a.equals(b))return true;
        return (a.split(" ").length>=3&&(" "+b+" ").contains(" "+a+" "))||(b.split(" ").length>=3&&(" "+a+" ").contains(" "+b+" "));
    }
    static List<OnlineClient.Track> diversify(MediaEntry seed,List<OnlineClient.Track> candidates){
        List<OnlineClient.Track> out=new ArrayList<>();Set<String> ids=new HashSet<>();ids.add(OnlineRules.videoId(seed.id));Map<String,Integer> artists=new HashMap<>();
        for(OnlineClient.Track track:candidates){
            String id=OnlineRules.videoId(track.url()),artist=words(track.artist());
            if(id==null||!ids.add(id)||sameSong(seed.name,seed.artist,track.name(),track.artist())||track.name().toLowerCase(Locale.ROOT).matches(".*\\b(cover|karaoke|reaction|tutorial)\\b.*"))continue;
            if(artists.getOrDefault(artist,0)>=3||out.stream().anyMatch(e->sameSong(e.name(),e.artist(),track.name(),track.artist())))continue;
            out.add(track);artists.merge(artist,1,Integer::sum);if(out.size()>=20)break;
        }
        return out;
    }
}
