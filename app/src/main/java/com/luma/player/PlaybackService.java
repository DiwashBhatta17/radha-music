package com.radha.music;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.audiofx.LoudnessEnhancer;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;

public final class PlaybackService extends MediaSessionService {
    public static PlaybackService instance;
    public ExoPlayer player;
    public boolean background=false, autoNext=true;
    public int boost=100;
    private MediaSession session;
    private LoudnessEnhancer enhancer;
    private LibraryStore history;
    @Override public void onCreate() {
        super.onCreate(); instance=this;history=new LibraryStore(this);
        player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(6000).setSeekForwardIncrementMs(6000).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        player.addListener(new Player.Listener(){
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
    @Override public void onTaskRemoved(Intent intent){if(!background||!player.getPlayWhenReady()){player.pause();stopSelf();}}
    @Override public void onDestroy(){if(enhancer!=null)enhancer.release();session.release();player.release();instance=null;super.onDestroy();}
}
