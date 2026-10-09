package com.radha.music;

import java.util.*;

/** Small session cache; failures stay distinct from confirmed missing lyrics. */
final class LyricsAvailability {
    enum State { CHECKING, AVAILABLE, MISSING, FAILED }
    record Entry(State state,long at){}
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    synchronized State state(String id,long now){Entry entry=entries.get(id);if(entry==null)return null;if(entry.state()==State.FAILED&&now-entry.at()>60000){entries.remove(id);return null;}return entry.state();}
    synchronized boolean begin(String id,long now){if(state(id,now)!=null)return false;if(entries.size()>=64)entries.remove(entries.keySet().iterator().next());entries.put(id,new Entry(State.CHECKING,now));return true;}
    synchronized void finish(String id,LyricsClient.Lyrics lyrics,long now){entries.put(id,new Entry(usable(lyrics)?State.AVAILABLE:State.MISSING,now));}
    synchronized void failed(String id,long now){entries.put(id,new Entry(State.FAILED,now));}
    static boolean usable(LyricsClient.Lyrics lyrics){return lyrics!=null&&!lyrics.instrumental()&&(!lyrics.plain().trim().isEmpty()||!lyrics.lines().isEmpty());}
}
