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
        String n=name.toLowerCase(Locale.ROOT), p=folder.toLowerCase(Locale.ROOT).replace('\\','/');
        for(String ext:new String[]{".m4a",".ogg",".aac",".acc",".amr",".opus",".3ga"})if(n.endsWith(ext))return false;
        return !p.contains("recording")&&!p.contains("recorder")&&!p.contains("voice notes")&&!p.contains("voice messages")&&!p.contains("call records");
    }
    public static String mediaTitle(String name){int dot=name.lastIndexOf('.');return dot>0?name.substring(0,dot):name;}
}
