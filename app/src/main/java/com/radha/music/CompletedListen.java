package com.radha.music;

import java.util.*;

/** Unique played ranges prevent seeks and repeats from faking a full listen. */
final class CompletedListen {
    private final TreeMap<Long,Long> ranges=new TreeMap<>();
    private long previous=-1;private boolean consumed;
    void position(long position,boolean playing){
        if(playing&&previous>=0&&position>=previous&&position-previous<=15000)add(previous,position);
        previous=playing?position:-1;
    }
    void discontinuity(){previous=-1;}
    private void add(long start,long end){
        Map.Entry<Long,Long> lower=ranges.floorEntry(start);if(lower!=null&&lower.getValue()>=start){start=lower.getKey();end=Math.max(end,lower.getValue());ranges.remove(lower.getKey());}
        Map.Entry<Long,Long> next;while((next=ranges.ceilingEntry(start))!=null&&next.getKey()<=end){end=Math.max(end,next.getValue());ranges.remove(next.getKey());}ranges.put(start,end);
    }
    boolean finish(long duration){if(consumed)return false;consumed=true;long covered=0;for(var e:ranges.entrySet())covered+=e.getValue()-e.getKey();return duration>0&&covered>=duration*.95;}
}
