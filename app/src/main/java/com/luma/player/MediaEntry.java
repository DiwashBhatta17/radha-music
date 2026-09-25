package com.radha.music;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import org.json.JSONObject;

public final class MediaEntry {
    public final String id, name, folder, artist;
    public final boolean video;
    public final long duration, added;
    public MediaEntry(String id, String name, String folder, String artist, boolean video, long duration, long added) {
        this.id=id; this.name=name; this.folder=folder; this.artist=artist; this.video=video; this.duration=duration; this.added=added;
    }
    public MediaItem item() {
        return new MediaItem.Builder().setMediaId(id).setUri(Uri.parse(id)).setMediaMetadata(new MediaMetadata.Builder()
            .setTitle(name).setArtist(video ? folder : artist).setMediaType(video ? MediaMetadata.MEDIA_TYPE_VIDEO : MediaMetadata.MEDIA_TYPE_MUSIC).build()).build();
    }
    public JSONObject json() {
        JSONObject o=new JSONObject();
        try {o.put("id",id);o.put("name",name);o.put("folder",folder);o.put("artist",artist);o.put("video",video);o.put("duration",duration);o.put("added",added);} catch(Exception ignored){}
        return o;
    }
    public static MediaEntry from(JSONObject o) {return new MediaEntry(o.optString("id"),o.optString("name"),o.optString("folder"),o.optString("artist"),o.optBoolean("video"),o.optLong("duration"),o.optLong("added"));}
}
