package com.radha.music;
import android.app.Application;
import android.net.Uri;
import androidx.media3.common.*;
import androidx.media3.exoplayer.dash.manifest.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class UpdateAndQualityTest {
 @Test public void versionOrderingIsNumericAndSkipsOldOrPrereleaseNames(){assertTrue(UpdateRules.newer("v1.10.0","1.6.0"));assertFalse(UpdateRules.newer("v1.6.0","1.6.0"));assertFalse(UpdateRules.newer("v1.5.2","1.6.0"));assertFalse(UpdateRules.newer("v1.7.0-beta","1.6.0"));}
 private JSONObject release()throws Exception{return new JSONObject().put("tag_name","v1.7.0").put("assets",new JSONArray().put(new JSONObject().put("name","Radha-Music.apk").put("size",8000000).put("digest","sha256:"+"ab".repeat(32)).put("browser_download_url","https://github.com/DiwashBhatta17/radha-music/releases/download/v1.7.0/Radha-Music.apk")));}
 @Test public void releaseRequiresOfficialAssetAndChecksum()throws Exception{JSONObject json=release();assertNotNull(UpdateRules.parse(json.toString(),"1.6.0"));json.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://example.com/app.apk");assertNull(UpdateRules.parse(json.toString(),"1.6.0"));json=release();json.getJSONArray("assets").getJSONObject(0).remove("digest");assertNull(UpdateRules.parse(json.toString(),"1.6.0"));}
 @Test public void releaseIgnoresDraftsAndPrereleases()throws Exception{JSONObject json=release();json.put("draft",true);assertNull(UpdateRules.parse(json.toString(),"1.6.0"));json=release().put("prerelease",true);assertNull(UpdateRules.parse(json.toString(),"1.6.0"));}
 @Test public void viewsAndDurationRemainInSavedMetadata()throws Exception{MediaEntry entry=new MediaEntry("radha://youtube/abcdefghijk/video","Video","YouTube","Artist",true,184000,1,"",1234567);MediaEntry restored=MediaEntry.from(entry.json());assertEquals(1234567,restored.views);assertEquals("3:04 · 1.2M views",SearchDetails.describe(restored.duration,restored.views));assertEquals("1:01:01",SearchDetails.describe(3661000,-1));assertFalse(SearchDetails.describe(1000,-1).contains("views"));}
 @Test public void adaptiveManifestExposesAllOriginalResolutionsAndByteRanges()throws Exception{
  List<AdaptiveManifest.Variant> variants=new ArrayList<>();for(int h:new int[]{144,240,360,480,720,1080})variants.add(new AdaptiveManifest.Variant("v"+h,"https://example.com/video?quality="+h+"&x=1","video/mp4","avc1.4d401f",h*16/9,h,h*3000,0,739,740,1000));
  String xml=AdaptiveManifest.build(variants,180);DashManifest manifest=new DashManifestParser().parse(Uri.parse("https://example.com/"),new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));assertEquals(180000,manifest.durationMs);List<Representation> tracks=manifest.getPeriod(0).adaptationSets.get(0).representations;assertEquals(6,tracks.size());assertEquals(360,tracks.get(2).format.height);assertEquals(720,tracks.get(4).format.height);assertEquals(1080,tracks.get(5).format.height);assertEquals(740,tracks.get(0).getIndexUri().start);assertTrue(tracks.get(0).baseUrls.get(0).url.contains("&x=1"));
 }
 @Test public void missingAdaptiveIndexesUseFallbackInsteadOfBrokenManifest(){assertNull(AdaptiveManifest.build(Collections.emptyList(),180));}
 @Test public void autoAndManualVideoSelectionKeepAudioAndTextOverrides(){Application app=RuntimeEnvironment.getApplication();TrackGroup video=new TrackGroup("video",new Format.Builder().setSampleMimeType("video/avc").setWidth(640).setHeight(360).build(),new Format.Builder().setSampleMimeType("video/avc").setWidth(1280).setHeight(720).build());TrackGroup audio=new TrackGroup("audio",new Format.Builder().setSampleMimeType("audio/mp4a-latm").build());TrackSelectionParameters manual=new TrackSelectionParameters.Builder(app).setOverrideForType(new TrackSelectionOverride(audio,0)).setOverrideForType(new TrackSelectionOverride(video,1)).build();assertEquals(2,manual.overrides.size());TrackSelectionParameters auto=manual.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build();assertFalse(auto.overrides.containsKey(video));assertTrue(auto.overrides.containsKey(audio));}
}
