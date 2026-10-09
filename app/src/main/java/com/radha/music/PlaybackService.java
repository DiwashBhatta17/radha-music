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
    private String listeningId="";
    private int listenedSeconds;
    private final Runnable listeningTick=new Runnable(){public void run(){
        if(player!=null&&player.isPlaying()){
            MediaItem item=player.getCurrentMediaItem();
            if(item!=null&&OnlineRules.videoId(item.mediaId)!=null){
                if(!item.mediaId.equals(listeningId)){listeningId=item.mediaId;listenedSeconds=0;}
                listenedSeconds+=5;
                if(listenedSeconds==30){MediaEntry e=new MediaEntry(item.mediaId,String.valueOf(item.mediaMetadata.title),"YouTube",String.valueOf(item.mediaMetadata.artist),item.mediaId.endsWith("/video"),Math.max(0,player.getDuration()),System.currentTimeMillis()/1000,item.mediaMetadata.artworkUri==null?"":item.mediaMetadata.artworkUri.toString());onlineHistory.listened(e);}
            }
        }
        timer.postDelayed(this,5000);
    }};
    @Override public void onCreate() {
        super.onCreate(); instance=this;history=new LibraryStore(this);onlineHistory=new OnlineStore(this);
        player=new ExoPlayer.Builder(this).setLoadControl(new androidx.media3.exoplayer.DefaultLoadControl.Builder().setBufferDurationsMs(30000,90000,1500,5000).setPrioritizeTimeOverSizeThresholds(true).build()).setMediaSourceFactory(new OnlineMediaSourceFactory(this)).setSeekBackIncrementMs(6000).setSeekForwardIncrementMs(6000).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_NETWORK);
        timer.postDelayed(listeningTick,5000);
        player.addListener(new Player.Listener(){
            @Override public void onMediaItemTransition(MediaItem item,int reason){listeningId=item==null?"":item.mediaId;listenedSeconds=0;}
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
    @Override public void onDestroy(){timer.removeCallbacksAndMessages(null);if(enhancer!=null)enhancer.release();session.release();player.release();instance=null;super.onDestroy();}
}
