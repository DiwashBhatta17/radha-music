package com.radha.music;
import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.*;
import java.util.concurrent.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class LyricsDialog extends Dialog {
    private final ExoPlayer player;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final LinearLayout lines;private final ScrollView scroll;private final TextView title,toggle,position;private final List<TextView> labels=new ArrayList<>();
    private List<LyricsRules.Line> synced=Collections.emptyList();private String id="";private int token,index=-2;private boolean closed;private long manualUntil;
    public LyricsDialog(Activity activity,ExoPlayer player){super(activity,android.R.style.Theme_Material_NoActionBar);this.player=player;LinearLayout root=column();root.setBackgroundColor(0xFF121315);root.setPadding(dp(20),dp(24),dp(20),dp(20));
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.addView(button("‹",this::dismiss),new LinearLayout.LayoutParams(dp(48),dp(48)));title=text("Lyrics",20);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
        scroll=new ScrollView(activity);scroll.setVerticalScrollBarEnabled(false);lines=column();lines.setPadding(dp(8),dp(36),dp(8),dp(48));scroll.addView(lines);scroll.setOnTouchListener((v,e)->{manualUntil=System.currentTimeMillis()+8000;return false;});root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView source=text("Lyrics · LRCLIB",11);source.setGravity(Gravity.CENTER);root.addView(source);position=text("",12);position.setGravity(Gravity.CENTER);root.addView(position);
        LinearLayout transport=new LinearLayout(activity);transport.setGravity(Gravity.CENTER);transport.addView(button("|‹",()->{if(player.hasPreviousMediaItem())player.seekToPreviousMediaItem();else player.seekTo(0);}),new LinearLayout.LayoutParams(dp(72),dp(64)));toggle=button("▶",()->{if(player.isPlaying())player.pause();else player.play();});transport.addView(toggle,new LinearLayout.LayoutParams(dp(80),dp(64)));transport.addView(button("›|",()->{if(player.hasNextMediaItem())player.seekToNextMediaItem();}),new LinearLayout.LayoutParams(dp(72),dp(64)));root.addView(transport);setContentView(root);
    }
    private int dp(int n){return Math.round(n*getContext().getResources().getDisplayMetrics().density);}private LinearLayout column(){LinearLayout v=new LinearLayout(getContext());v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String s,int size){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(0xFFF8F6F2);return t;}private TextView button(String s,Runnable run){TextView t=text(s,28);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->run.run());return t;}
    @Override protected void onStart(){super.onStart();getWindow().setLayout(-1,-1);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);main.post(tick);}
    private final Runnable tick=new Runnable(){public void run(){if(closed)return;MediaItem item=player.getCurrentMediaItem();if(item!=null&&!id.equals(item.mediaId)){id=item.mediaId;load(item);}toggle.setText(player.isPlaying()?"Ⅱ":"▶");position.setText(clock(player.getCurrentPosition())+"  /  "+clock(player.getDuration()));int selected=LyricsRules.current(synced,player.getCurrentPosition());if(selected!=index){index=selected;for(int i=0;i<labels.size();i++){labels.get(i).setTextColor(i==index?0xFFFF795F:0xFF8E8F92);labels.get(i).setTypeface(null,i==index?Typeface.BOLD:Typeface.NORMAL);}if(index>=0&&index<labels.size()&&System.currentTimeMillis()>manualUntil){TextView line=labels.get(index);scroll.smoothScrollTo(0,Math.max(0,line.getTop()-scroll.getHeight()/3));}}main.postDelayed(this,350);}};
    private static String clock(long ms){long seconds=Math.max(0,ms/1000);return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);}
    private void load(MediaItem item){int generation=++token;title.setText(String.valueOf(item.mediaMetadata.title));lines.removeAllViews();labels.clear();synced=Collections.emptyList();index=-2;lines.addView(new RadhaLoadingView(getContext()),new LinearLayout.LayoutParams(-1,dp(64)));
        MediaEntry entry=new MediaEntry(item.mediaId,String.valueOf(item.mediaMetadata.title),"",String.valueOf(item.mediaMetadata.artist),false,Math.max(0,player.getDuration()),0);
        worker.submit(()->{try{LyricsClient.Lyrics lyric=LyricsClient.fetch(getContext().getApplicationContext(),entry);main.post(()->{if(closed||generation!=token)return;lines.removeAllViews();synced=lyric.lines();if(!synced.isEmpty()){for(LyricsRules.Line l:synced){TextView t=text(l.text(),25);t.setPadding(0,dp(14),0,dp(14));t.setOnClickListener(v->player.seekTo(l.time()));labels.add(t);lines.addView(t);}}else{TextView t=text(lyric.instrumental()?"Instrumental · no lyrics":lyric.plain().isEmpty()?"Lyrics aren’t available for this song yet.":lyric.plain(),23);t.setLineSpacing(dp(12),1);lines.addView(t);}index=-2;scroll.scrollTo(0,0);});}catch(Exception error){main.post(()->{if(closed||generation!=token)return;lines.removeAllViews();lines.addView(text("Couldn’t get lyrics. Check your connection and try again.",18));TextView retry=button("Retry",()->load(item));retry.setTextSize(16);lines.addView(retry);});}});
    }
    @Override public void dismiss(){closed=true;token++;main.removeCallbacksAndMessages(null);worker.shutdownNow();super.dismiss();}
}
