package com.radha.music;

import org.junit.Test;
import static org.junit.Assert.*;

public class LibraryRulesTest {
    @Test public void songsAreNotHiddenByTheirFormat(){for(String extension:new String[]{"mp3","m4a","ogg","aac","acc","amr","opus","flac","wav"})assertTrue(extension,LibraryRules.showMusic("My song."+extension,"Download/"));}
    @Test public void recordingFoldersAreHiddenWithoutMatchingAlbumNames(){assertFalse(LibraryRules.showMusic("Call.mp3","MIUI/sound_recorder/call_rec/"));assertFalse(LibraryRules.showMusic("Memo.m4a","Recordings/"));assertFalse(LibraryRules.showMusic("Voice.opus","Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/202639/"));assertTrue(LibraryRules.showMusic("Song.flac","Music/Studio Recordings/"));assertTrue(LibraryRules.showMusic("Call Me.mp3","Music/"));}
    @Test public void orientationPreservesPortraitAndLandscape(){assertEquals(1,LibraryRules.videoOrientation(1080,1920,1));assertEquals(2,LibraryRules.videoOrientation(1920,1080,1));assertEquals(1,LibraryRules.videoOrientation(1080,1080,1));assertEquals(0,LibraryRules.videoOrientation(0,0,1));assertEquals(2,LibraryRules.videoOrientation(720,576,1.42f));}
}
