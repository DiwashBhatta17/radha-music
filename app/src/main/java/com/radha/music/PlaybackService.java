package com.radha.music;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.audiofx.LoudnessEnhancer;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class PlaybackService extends MediaSessionService {
    public static PlaybackService instance;
    public ExoPlayer player;
    public boolean background=false, autoNext=true;
    public int boost=100;
    private MediaSession session;
    private LoudnessEnhancer enhancer;
    private LibraryStore history;
    private OnlineStore onlineHistory;
    private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());

    private int listenedSeconds;
    private MediaEntry selectedSearch,radioSeed;
    private String radioQuery="";
    private CompletedListen completion;
    private long listeningDuration;
    private int radioGeneration;
    private boolean radioRequested;
    private java.util.List<MediaEntry> radioEntries=java.util.Collections.emptyList();
    private final java.util.concurrent.ExecutorService radioWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Runnable listeningTick=new Runnable(){public void run(){
        if(player!=null){
            if(completion!=null){completion.position(player.getCurrentPosition(),player.isPlaying());if(player.getDuration()>0)listeningDuration=player.getDuration();}
            MediaItem item=player.getCurrentMediaItem();
            if(player.isPlaying()&&item!=null&&OnlineRules.videoId(item.mediaId)!=null){
                listenedSeconds++;
                if(listenedSeconds==30){onlineHistory.listened(entry(item));onlineHistory.learnContext(radioQuery);if(radioSeed!=null)onlineHistory.learnRadio(radioSeed,radioEntries);}
            }
        }
        timer.postDelayed(this,1000);
    }};
    private MediaEntry entry(MediaItem item){return new MediaEntry(item.mediaId,String.valueOf(item.mediaMetadata.title),"YouTube",String.valueOf(item.mediaMetadata.artist),item.mediaId.endsWith("/video"),Math.max(0,player.getDuration()),System.currentTimeMillis()/1000,item.mediaMetadata.artworkUri==null?"":item.mediaMetadata.artworkUri.toString());}
    public void startRadio(MediaEntry seed,String query,boolean searched){
        radioGeneration++;radioSeed=seed;radioQuery=RelatedRules.sameSong(query,"",seed.name,seed.artist)?"":query;radioRequested=false;radioEntries=java.util.Collections.emptyList();
        selectedSearch=searched?seed:null;completion=searched?new CompletedListen():null;listeningDuration=seed.duration;
        if(completion!=null)completion.position(0,true);
        requestRadio();
    }
    private void finishSearch(long endPosition){
        if(completion==null||selectedSearch==null)return;
        completion.position(endPosition,true);
        if(completion.finish(listeningDuration)){onlineHistory.completedSearch(selectedSearch);onlineHistory.learnContext(radioQuery);if(radioSeed!=null)onlineHistory.learnRadio(radioSeed,radioEntries);}
        selectedSearch=null;completion=null;
    }
    private void requestRadio(){
        if(radioSeed==null||radioRequested||player.getPlaybackState()!=Player.STATE_READY)return;
        radioRequested=true;MediaEntry seed=radioSeed;String query=radioQuery;int generation=radioGeneration;
        radioWorker.execute(()->{
            java.util.List<OnlineClient.Track> candidates=new java.util.ArrayList<>();
            try{if(RelatedRules.moodQuery(query))candidates.addAll(SessionCatalog.INSTANCE.search(query,seed.video).tracks());else candidates.addAll(seed.video?OnlineClient.related(seed.id):OnlineClient.musicRadio(seed.id));}catch(Exception ignored){}
            java.util.List<OnlineClient.Track> related=RelatedRules.diversify(seed,candidates);
            if(related.size()<5&&!Thread.currentThread().isInterrupted())try{String fallback=RelatedRules.moodQuery(query)?query:seed.artist+(seed.video?" music videos":" songs");candidates.addAll(SessionCatalog.INSTANCE.search(fallback,seed.video).tracks());related=RelatedRules.diversify(seed,candidates);}catch(Exception ignored){}
            java.util.List<OnlineClient.Track> ready=related;
            timer.post(()->{
                if(generation!=radioGeneration||player==null||player.getCurrentMediaItem()==null||!seed.id.equals(player.getCurrentMediaItem().mediaId))return;
                java.util.Set<String> ids=new java.util.HashSet<>();for(int i=0;i<player.getMediaItemCount();i++)ids.add(OnlineRules.videoId(player.getMediaItemAt(i).mediaId));
                java.util.List<MediaEntry> entries=new java.util.ArrayList<>();java.util.List<MediaItem> items=new java.util.ArrayList<>();
                for(OnlineClient.Track track:ready)if(ids.add(OnlineRules.videoId(track.url()))){MediaEntry entry=new MediaEntry(OnlineRules.mediaId(track.url(),seed.video),track.name(),"YouTube",track.artist(),seed.video,track.seconds()*1000,System.currentTimeMillis()/1000,track.image());entries.add(entry);items.add(entry.item());}
                radioEntries=entries;if(listenedSeconds>=30)onlineHistory.learnRadio(seed,entries);onlineHistory.remember(entries);player.addMediaItems(items);
            });
        });
    }
    @Override public void onCreate() {
        super.onCreate(); instance=this;history=new LibraryStore(this);onlineHistory=new OnlineStore(this);
        player=new ExoPlayer.Builder(this).setLoadControl(new androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(15000,60000,750,1500).setTargetBufferBytes(64*1024*1024).setPrioritizeTimeOverSizeThresholds(false).build()).setMediaSourceFactory(new OnlineMediaSourceFactory(this)).setSeekBackIncrementMs(6000).setSeekForwardIncrementMs(6000).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_NETWORK);
        timer.postDelayed(listeningTick,1000);
        player.addListener(new Player.Listener(){
            @Override public void onPositionDiscontinuity(Player.PositionInfo oldPosition,Player.PositionInfo newPosition,int reason){
                if(completion!=null){if(reason==Player.DISCONTINUITY_REASON_AUTO_TRANSITION)finishSearch(oldPosition.positionMs);else completion.discontinuity();}
            }
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED)finishSearch(player.getCurrentPosition());if(state==Player.STATE_READY){if(player.getDuration()>0)listeningDuration=player.getDuration();requestRadio();}}
            @Override public void onIsPlayingChanged(boolean playing){if(completion!=null){completion.position(player.getCurrentPosition(),true);if(!playing)completion.discontinuity();}}
            @Override public void onMediaItemTransition(MediaItem item,int reason){listenedSeconds=0;radioGeneration++;radioSeed=null;radioQuery="";radioEntries=java.util.Collections.emptyList();radioRequested=false;selectedSearch=null;completion=null;}
            @Override public void onAudioSessionIdChanged(int id){attachEnhancer(id);}
            @Override public void onRenderedFirstFrame(){markCurrentPlayed();}
            @Override public void onEvents(Player p,Player.Events events){if(background&&p.isPlaying())markCurrentPlayed();}
        });
        PendingIntent launch=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        session=new MediaSession.Builder(this,player).setSessionActivity(launch).build();
    }
    private void markCurrentPlayed(){MediaItem m=player.getCurrentMediaItem();if(m!=null&&java.util.Objects.equals(m.mediaMetadata.mediaType,MediaMetadata.MEDIA_TYPE_VIDEO))history.markPlayed(m.mediaId);}
    private void attachEnhancer(int id){
        if(enhancer!=null){enhancer.release();enhancer=null;}
        if(id==C.AUDIO_SESSION_ID_UNSET)return;
        try{enhancer=new LoudnessEnhancer(id);setBoost(boost);}catch(RuntimeException ignored){}
    }
    public boolean setBoost(int percent){
        boost=Math.max(100,Math.min(200,percent));
        if(enhancer==null)return boost==100;
        try{enhancer.setTargetGain(PlaybackRules.gainMillibels(boost));enhancer.setEnabled(boost>100);return true;}catch(RuntimeException e){return false;}
    }
    public void setAutoNext(boolean enabled){autoNext=enabled;player.setPauseAtEndOfMediaItems(!enabled);}
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo info){return session;}
    @Override public void onTaskRemoved(Intent intent){SessionCatalog.INSTANCE.reset();if(!background||!player.getPlayWhenReady()){player.pause();stopSelf();}}
    @Override public void onDestroy(){radioGeneration++;radioWorker.shutdownNow();timer.removeCallbacksAndMessages(null);if(enhancer!=null)enhancer.release();session.release();player.release();instance=null;super.onDestroy();}
}
