package com.radha.music;
import java.util.*;
import java.util.regex.*;

public final class LyricsRules {
    public record Line(long time,String text) {}
    private static final Pattern TIME=Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
    public static List<Line> parse(String lrc){List<Line> lines=new ArrayList<>();long offset=0;Matcher off=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE).matcher(lrc);if(off.find())try{offset=Long.parseLong(off.group(1));}catch(Exception ignored){}
        for(String row:lrc.split("\\r?\\n")){Matcher m=TIME.matcher(row);List<Long> times=new ArrayList<>();int end=0;while(m.find()){String fraction=m.group(3);long millis=fraction==null?0:Integer.parseInt((fraction+"000").substring(0,3));times.add((Long.parseLong(m.group(1))*60+Integer.parseInt(m.group(2)))*1000+millis+offset);end=m.end();}String text=row.substring(end).trim();if(!text.isEmpty())for(long time:times)lines.add(new Line(Math.max(0,time),text));}lines.sort(Comparator.comparingLong(Line::time));return lines;}
    public static int current(List<Line> lines,long position){int lo=0,hi=lines.size()-1,result=-1;while(lo<=hi){int mid=(lo+hi)>>>1;if(lines.get(mid).time()<=position){result=mid;lo=mid+1;}else hi=mid-1;}return result;}
    public static String title(String value){return value.replaceAll("(?i)\\s*[\\[(](?:official|lyrics?|audio|video|visuali[sz]er|music video)[^\\])]*[\\])]","").trim();}
    public static String artist(String value){return value.replaceAll("(?i)\\s*-\\s*Topic$","").replaceAll("(?i)VEVO$","").trim();}
    public static boolean matches(String title,String artist,long seconds,String otherTitle,String otherArtist,double otherSeconds){return title.trim().equalsIgnoreCase(otherTitle.trim())&&artist.trim().equalsIgnoreCase(otherArtist.trim())&&(seconds<=0||Math.abs(seconds-otherSeconds)<=8);}
}
