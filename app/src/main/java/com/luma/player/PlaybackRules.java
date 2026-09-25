package com.radha.music;

/** Pure playback rules shared by controls and unit tests. */
public final class PlaybackRules {
    private PlaybackRules() {}
    public static long seek(long current, long delta, long duration) {
        long next=Math.max(0,current+delta);
        return duration<0 ? next : Math.min(next,duration);
    }
    public static int insertNext(int current, int count) { return Math.min(count,Math.max(0,current+1)); }
    public static int gainMillibels(int percent) {return (int)Math.round(2000*Math.log10(Math.max(100,Math.min(200,percent))/100.0));}
}
