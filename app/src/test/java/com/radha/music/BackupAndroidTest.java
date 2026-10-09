package com.radha.music;
import android.app.Application;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BackupAndroidTest {
 @Test public void roundTripRestoresPreferencesHistoryLibraryAndPrivateDownload()throws Exception {
  Application c=RuntimeEnvironment.getApplication();OnlineStore online=new OnlineStore(c);online.preference("Nepali acoustic");MediaEntry entry=new MediaEntry("radha://youtube/abcdefghijk/audio","Test song","YouTube","Artist",false,100000,1);online.toggleSaved(entry);online.listened(entry);online.listened(entry);online.listened(entry);
  DownloadStore downloads=new DownloadStore(c);byte[] bytes={1,2,3,4};try(FileOutputStream out=new FileOutputStream(downloads.file(entry.id,"audio"))){out.write(bytes);}downloads.complete(entry,true,false);
  LibraryStore library=new LibraryStore(c);library.favorites.add(entry.id);library.playlists.put("Travel",new java.util.ArrayList<>(java.util.Collections.singletonList(entry)));library.save();ByteArrayOutputStream backup=new ByteArrayOutputStream();BackupStore.exportTo(c,backup);
  online.preference("Changed");online.clearHistory();online.toggleSaved(entry);downloads.remove(entry.id);library.favorites.clear();library.playlists.clear();library.save();BackupStore.importFrom(c,new ByteArrayInputStream(backup.toByteArray()));
  OnlineStore restored=new OnlineStore(c);assertEquals("Nepali acoustic",restored.preference());assertEquals(1,restored.speedDials(false).size());assertEquals(entry.id,restored.saved().get(0).id);assertTrue(new LibraryStore(c).favorites.contains(entry.id));assertEquals(1,new LibraryStore(c).playlists.get("Travel").size());assertArrayEquals(bytes,Files.readAllBytes(new DownloadStore(c).files(entry.id).audio().toPath()));
 }
 @Test public void rejectsTraversalWithoutChangingPreferences()throws Exception {
  Application c=RuntimeEnvironment.getApplication();new OnlineStore(c).preference("Keep me");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry("files/downloads/../../escape.media"));zip.write(1);zip.closeEntry();}
  try{BackupStore.importFrom(c,new ByteArrayInputStream(bytes.toByteArray()));fail("Traversal accepted");}catch(IOException expected){}assertEquals("Keep me",new OnlineStore(c).preference());assertFalse(BackupStore.safeEntry("files/downloads/../escape.media"));assertFalse(BackupStore.safeEntry("/manifest.json"));
 }
 @Test public void invalidMetadataCannotReplaceExistingMedia()throws Exception {
  Application c=RuntimeEnvironment.getApplication();File original=new DownloadStore(c).file("radha://youtube/abcdefghijk/audio","audio");try(FileOutputStream out=new FileOutputStream(original)){out.write(9);}ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry("files/downloads/"+original.getName()));zip.write(2);zip.closeEntry();zip.putNextEntry(new ZipEntry("manifest.json"));zip.write("{\"format\":\"wrong\",\"version\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}
  try{BackupStore.importFrom(c,new ByteArrayInputStream(bytes.toByteArray()));fail("Invalid backup accepted");}catch(IOException expected){}assertArrayEquals(new byte[]{9},Files.readAllBytes(original.toPath()));
 }
}
