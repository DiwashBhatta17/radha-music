package com.radha.music;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.*;
import androidx.media3.datasource.*;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.source.*;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import java.io.IOException;

/** Resolves expiring links on Media3 loader threads, including next/previous and queued tracks. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class OnlineMediaSourceFactory implements MediaSource.Factory {
    private final DefaultMediaSourceFactory local;
    private final ProgressiveMediaSource.Factory online;
    private final DownloadStore downloads;
    public OnlineMediaSourceFactory(Context context){
        downloads=new DownloadStore(context);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0").setConnectTimeoutMs(15000).setReadTimeoutMs(20000);
        local=new DefaultMediaSourceFactory(context);
        ResolvingDataSource.Factory resolving=new ResolvingDataSource.Factory(http,spec->{
            String value=spec.uri.toString();if(!"radha".equals(spec.uri.getScheme()))return spec;
            OnlineClient.Streams streams=OnlineClient.resolve(value);boolean video=value.endsWith("/video");
            String url=video?(streams.video()!=null?streams.video():streams.muxed()):(streams.audio()!=null?streams.audio():streams.muxed());
            if(url==null)throw new IOException("This item has no supported "+(video?"video":"audio")+" stream");
            return spec.withUri(Uri.parse(url));
        });
        online=new ProgressiveMediaSource.Factory(resolving);
    }
    @Override public MediaSource createMediaSource(MediaItem item){
        if(OnlineRules.videoId(item.mediaId)==null)return local.createMediaSource(item);
        DownloadStore.Files saved=downloads.files(item.mediaId);
        if(saved!=null){
            MediaSource video=saved.video()==null?null:local.createMediaSource(item.buildUpon().setUri(Uri.fromFile(saved.video())).build());
            MediaSource audio=saved.audio()==null?null:local.createMediaSource(item.buildUpon().setUri(Uri.fromFile(saved.audio())).build());
            return video!=null&&audio!=null?new MergingMediaSource(true,video,audio):video!=null?video:audio;
        }
        if(!item.mediaId.endsWith("/video"))return online.createMediaSource(item);
        String audioId=item.mediaId.substring(0,item.mediaId.length()-5)+"audio";
        MediaItem audio=item.buildUpon().setUri(audioId).build();
        return new MergingMediaSource(true,online.createMediaSource(item),online.createMediaSource(audio));
    }
    @Override public int[] getSupportedTypes(){return local.getSupportedTypes();}
    @Override public MediaSource.Factory setDrmSessionManagerProvider(DrmSessionManagerProvider provider){local.setDrmSessionManagerProvider(provider);online.setDrmSessionManagerProvider(provider);return this;}
    @Override public MediaSource.Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy policy){local.setLoadErrorHandlingPolicy(policy);online.setLoadErrorHandlingPolicy(policy);return this;}
}
