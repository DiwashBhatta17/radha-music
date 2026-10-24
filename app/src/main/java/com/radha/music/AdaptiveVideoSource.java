package com.radha.music;
import android.os.*;
import androidx.media3.common.*;
import androidx.media3.datasource.*;
import androidx.media3.exoplayer.source.*;
import androidx.media3.exoplayer.upstream.Allocator;
import androidx.media3.exoplayer.dash.*;
import androidx.media3.exoplayer.dash.manifest.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Resolve on a worker, then attach an adaptive source on the playback thread. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class AdaptiveVideoSource extends CompositeMediaSource<Void> {
 private static final ExecutorService workers=Executors.newFixedThreadPool(2);
 private final MediaItem item,audio;private final ProgressiveMediaSource.Factory progressive;private final DataSource.Factory http;
 private MediaSource child;private volatile IOException error;private boolean released;private Handler handler;private int generation;
 AdaptiveVideoSource(MediaItem item,MediaItem audio,ProgressiveMediaSource.Factory progressive,DataSource.Factory http){this.item=item;this.audio=audio;this.progressive=progressive;this.http=http;}
 @Override public MediaItem getMediaItem(){return item;}
 @Override protected void prepareSourceInternal(TransferListener listener){super.prepareSourceInternal(listener);released=false;error=null;handler=new Handler(Looper.myLooper());int token=++generation;Handler callback=handler;workers.execute(()->{
  try{OnlineClient.Streams streams=OnlineClient.resolve(item.mediaId);DashManifest manifest=streams.dash()==null?null:new DashManifestParser().parse(android.net.Uri.parse("https://www.youtube.com/"),new ByteArrayInputStream(streams.dash().getBytes(StandardCharsets.UTF_8)));
   callback.post(()->{if(released||token!=generation)return;MediaSource video=manifest==null?progressive.createMediaSource(item):new DashMediaSource.Factory(http).createMediaSource(manifest,item);child=new MergingMediaSource(true,video,progressive.createMediaSource(audio));prepareChildSource(null,child);});
  }catch(Exception e){callback.post(()->{if(!released&&token==generation)error=new IOException("Could not prepare online video",e);});}
 });}
 @Override protected void onChildSourceInfoRefreshed(Void id,MediaSource source,Timeline timeline){refreshSourceInfo(timeline);}
 @Override public void maybeThrowSourceInfoRefreshError()throws IOException{if(error!=null)throw error;super.maybeThrowSourceInfoRefreshError();}
 @Override public MediaPeriod createPeriod(MediaSource.MediaPeriodId id,Allocator allocator,long position){return child.createPeriod(id,allocator,position);}
 @Override public void releasePeriod(MediaPeriod period){child.releasePeriod(period);}
 @Override protected void releaseSourceInternal(){released=true;generation++;if(handler!=null)handler.removeCallbacksAndMessages(null);super.releaseSourceInternal();child=null;}
}
