package com.radha.music;

import java.util.*;
import java.util.regex.*;

/** Stable IDs are saved instead of short-lived CDN links. */
public final class OnlineRules {
    private OnlineRules() {}
    public static String videoId(String value) {
        if(value==null)return null;
        Matcher m=Pattern.compile("^radha://youtube/([A-Za-z0-9_-]{11})/(audio|video)$").matcher(value);
        if(m.matches())return m.group(1);
        m=Pattern.compile("^https://(?:www\\.|music\\.)?youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})(?:&.*)?$").matcher(value);
        return m.matches()?m.group(1):null;
    }
    public static String mediaId(String url,boolean video) {
        String id=videoId(url);if(id==null)throw new IllegalArgumentException("Unsupported YouTube link");
        return "radha://youtube/"+id+(video?"/video":"/audio");
    }
    public static List<String> topArtists(Map<String,Integer> scores,int limit) {
        List<String> artists=new ArrayList<>(scores.keySet());artists.removeIf(s->s.trim().isEmpty());
        artists.sort(Comparator.<String>comparingInt(s->scores.getOrDefault(s,0)).reversed().thenComparing(String.CASE_INSENSITIVE_ORDER));
        return new ArrayList<>(artists.subList(0,Math.min(limit,artists.size())));
    }
}
