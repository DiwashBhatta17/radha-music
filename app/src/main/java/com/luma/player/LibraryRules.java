package com.radha.music;

import java.util.Locale;

/** Display/filter rules do not move or alter any media files. */
public final class LibraryRules {
    private LibraryRules() {}
    public static String folderTitle(String path) {
        String s=path.replaceAll("[/\\\\]+$", "");
        int split=Math.max(s.lastIndexOf('/'),s.lastIndexOf('\\'));
        s=s.substring(split+1);
        if(s.isEmpty())return "Videos";
        int first=s.offsetByCodePoints(0,1);
        return s.substring(0,first).toUpperCase(Locale.ROOT)+s.substring(first);
    }
    public static boolean showMusic(String name,String folder) {
        // An audio extension identifies a format, not whether the file is a song.
        String p=folder.toLowerCase(Locale.ROOT).replace('\\','/');
        for(String segment:p.split("/")) {
            String normalized=segment.replace('_',' ').replace('-',' ').trim();
            if(normalized.matches("recordings?|call recordings?|call records?|call rec|sound recorder|voice recorder|voice recordings?|voice notes|voice messages|whatsapp voice notes|recorder"))return false;
        }
        return true;
    }
    /** 0: unknown, 1: portrait/square, 2: landscape, accounting for non-square pixels. */
    public static int videoOrientation(int width,int height,float pixelRatio){if(width<=0||height<=0)return 0;return width*(pixelRatio>0?pixelRatio:1)>height?2:1;}
    public static String mediaTitle(String name){int dot=name.lastIndexOf('.');return dot>0?name.substring(0,dot):name;}
}
