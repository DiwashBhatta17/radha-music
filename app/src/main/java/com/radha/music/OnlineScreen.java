package com.radha.music;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Online browsing is independent of local MediaStore permissions and scans. */
public final class OnlineScreen extends LinearLayout {
    public interface Host {void play(MediaEntry e,List<MediaEntry> queue);void menu(MediaEntry e);void image(MediaEntry e,ImageView target);void preferences();}
    private final Host host;
    private final OnlineStore store;
    private final boolean watch,dark;
    private final int ink,muted,surface;
    private final int coral=0xFFFF795F;
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private Future<?> request;
    private final LinearLayout body;
    private final List<MediaEntry> tracks=new ArrayList<>();
    private OnlineClient.Results result;
    private String query="";
    private int generation;
    private boolean closed,busy;
    public OnlineScreen(Context context,boolean watch,boolean dark,OnlineStore store,Host host){
        super(context);this.host=host;this.watch=watch;this.dark=dark;this.store=store;ink=dark?0xFFF8F6F2:0xFF171819;muted=dark?0xFFA4A4A6:0xFF727274;surface=dark?0xFF202123:0xFFF0ECE7;
        setOrientation(VERTICAL);ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);body=column();body.setPadding(dp(4),dp(12),dp(4),dp(20));scroll.addView(body);addView(scroll,new LayoutParams(-1,-1));showHome();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(getContext());v.setOrientation(VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(getContext());v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable shape(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(16));return g;}
    private void gap(int n){body.addView(new View(getContext()),new LayoutParams(1,dp(n)));}
    private TextView action(String label,Runnable run){TextView t=text(label,13,ink,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(16),dp(12),dp(16),dp(12));t.setMinHeight(dp(48));t.setBackground(shape(surface));t.setOnClickListener(v->run.run());return t;}
    private void chips(){HorizontalScrollView sc=new HorizontalScrollView(getContext());sc.setHorizontalScrollBarEnabled(false);LinearLayout row=row();String[] labels=watch?new String[]{"For you","Music videos","Acoustic","Live sessions"}:new String[]{"For you","Chill","Hindi","Nepali","Marathi"};for(String label:labels){TextView chip=action(label,()->search(label.equals("For you")?seed():label+(watch?" music video":" songs")));LayoutParams p=new LayoutParams(-2,-2);p.rightMargin=dp(8);row.addView(chip,p);}sc.addView(row);body.addView(sc);}
    private String seed(){List<String> artists=store.artists();return artists.isEmpty()?store.preference()+" songs":artists.get(0)+(watch?" music video":" songs");}
    public void showHome(){query="";body.removeAllViews();body.addView(text(watch?"Online Watch":"Your next favorite.",28,ink,true));gap(6);body.addView(text(store.artists().isEmpty()?"Start with your favorite sounds":"Inspired by what you play here",13,muted,false));gap(18);chips();gap(18);
        if(!watch){FrameLayout hero=new FrameLayout(getContext());hero.setBackground(shape(surface));hero.setClipToOutline(true);hero.addView(new Landscape(getContext(),false),new FrameLayout.LayoutParams(-1,-1));LinearLayout copy=column();copy.setPadding(dp(20),dp(50),dp(18),dp(18));copy.addView(text("MUSIC FOR YOUR MOMENTS",10,0xFFF1DDD3,true));copy.addView(text("Your daily mix",25,0xFFFFFFFF,true));copy.addView(text("Explore your favorite sounds   ›",13,0xFFF1DDD3,false));hero.addView(copy);hero.setContentDescription("Explore your daily mix");hero.setOnClickListener(v->search(seed()));body.addView(hero,new LayoutParams(-1,dp(174)));gap(22);body.addView(text("Made for you",21,ink,true));gap(12);LinearLayout mixes=row();mixes.addView(mix("Late night","chill",true),new LayoutParams(0,dp(146),1));View space=new View(getContext());mixes.addView(space,new LayoutParams(dp(12),1));mixes.addView(mix("Acoustic days","acoustic",false),new LayoutParams(0,dp(146),1));body.addView(mixes);gap(16);}
        body.addView(action("Listening preferences  ›",host::preferences));gap(20);body.addView(text(watch?"Recommended videos":"Quick picks",21,ink,true));gap(12);load(seed(),false,true);
    }
    private View mix(String title,String mood,boolean night){FrameLayout frame=new FrameLayout(getContext());frame.setBackground(shape(surface));frame.setClipToOutline(true);frame.addView(new Landscape(getContext(),night),new FrameLayout.LayoutParams(-1,-1));TextView t=text(title+"  ›",17,0xFFFFFFFF,true);t.setPadding(dp(14),dp(12),dp(10),dp(14));frame.addView(t,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));frame.setOnClickListener(v->search(store.preference()+" "+mood+" songs"));return frame;}
    public void search(String value){if(value.trim().isEmpty()){showHome();return;}query=value.trim();body.removeAllViews();body.addView(action("‹  Discover",this::showHome));gap(18);body.addView(text(query,23,ink,true));gap(14);load(query,false,false);}
    private void load(String q,boolean append,boolean home){
        if(request!=null)request.cancel(true);int token=++generation;busy=true;if(!append){tracks.clear();result=null;}
        TextView status=text("Finding your next favorite…",14,muted,false);status.setPadding(0,dp(18),0,dp(18));body.addView(status);
        OnlineClient.Results previous=result;
        request=worker.submit(()->{try{
            OnlineClient.Results found=append?OnlineClient.more(previous):OnlineClient.search(q,watch);List<MediaEntry> batch=new ArrayList<>();for(OnlineClient.Track t:found.tracks())batch.add(new MediaEntry(OnlineRules.mediaId(t.url(),watch),t.name(),"YouTube",t.artist(),watch,t.seconds()*1000,System.currentTimeMillis()/1000,t.image()));
            post(()->{if(closed||token!=generation)return;busy=false;status.setText(batch.size()+" results · "+(found.fallback()?"YouTube search":"YouTube"));result=found;Set<String> seen=new HashSet<>();for(MediaEntry e:tracks)seen.add(e.id);for(MediaEntry e:batch)if(seen.add(e.id)){tracks.add(e);body.addView(trackRow(e));}store.remember(batch);
                if(tracks.isEmpty()){body.addView(text("No results. Try a different artist or title.",15,muted,false));}
                if(OnlineClient.hasMore(result)){TextView more=action("Load more",()->{if(busy)return;load(q,true,home);});more.setOnClickListener(v->{if(busy)return;body.removeView(more);load(q,true,home);});body.addView(more);}
            });
        }catch(Exception|LinkageError error){post(()->{if(closed||token!=generation)return;busy=false;failed(status,error,q,home);});}});
        postDelayed(()->{if(closed||!busy||token!=generation)return;busy=false;generation++;request.cancel(true);failed(status,new java.net.SocketTimeoutException("Search exceeded 90 seconds"),q,home);},90000);
    }
    private void failed(TextView status,Throwable error,String q,boolean home){
        status.setText(OnlineErrors.message(error));
        body.addView(action("Retry search",()->{if(home)showHome();else search(q);}));
        body.addView(action("Error details",()->{
            String detail="Radha Music 1.3.1\nAndroid "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\n"+OnlineErrors.details(error);
            android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(getContext()).setTitle("Online connection details").setMessage(detail).setPositiveButton("Close",null);
            dialog.setNeutralButton("Copy error",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Radha Music error",detail));Toast.makeText(getContext(),"Error copied",Toast.LENGTH_SHORT).show();});dialog.show();
        }));
    }
    private View trackRow(MediaEntry entry){
        LinearLayout wrap=column();wrap.setPadding(0,0,0,dp(watch?18:6));LinearLayout line=row();line.setPadding(0,dp(6),0,dp(6));
        ImageView image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(shape(surface));image.setClipToOutline(true);host.image(entry,image);
        if(watch){FrameLayout frame=new FrameLayout(getContext());frame.addView(image,new FrameLayout.LayoutParams(-1,-1));TextView play=text("▶",32,0xFFFFFFFF,true);play.setGravity(Gravity.CENTER);frame.addView(play,new FrameLayout.LayoutParams(-1,-1));frame.setOnClickListener(v->host.play(entry,new ArrayList<>(tracks)));wrap.addView(frame,new LayoutParams(-1,dp(190)));}else line.addView(image,new LayoutParams(dp(54),dp(54)));
        LinearLayout labels=column();labels.setPadding(dp(watch?0:12),0,dp(8),0);TextView title=text(entry.name,15,ink,true);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);TextView artist=text(entry.artist+" · YouTube",12,muted,false);artist.setMaxLines(1);artist.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(artist);line.addView(labels,new LayoutParams(0,-2,1));TextView more=action("⋮",()->host.menu(entry));more.setContentDescription("Options for "+entry.name);more.setBackgroundColor(android.graphics.Color.TRANSPARENT);line.addView(more,new LayoutParams(dp(48),dp(48)));line.setOnClickListener(v->host.play(entry,new ArrayList<>(tracks)));wrap.addView(line);return wrap;
    }
    public void close(){closed=true;generation++;worker.shutdownNow();}
    /** Original landscape artwork echoes the approved sunset covers without bundling large images. */
    public static final class Landscape extends View {
        private final Paint p=new Paint(3);private final boolean night;
        public Landscape(Context c,boolean night){super(c);this.night=night;}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new LinearGradient(0,0,0,h,night?new int[]{0xFF1E344B,0xFF847079,0xFF15252B}:new int[]{0xFFFFA379,0xFFA6747E,0xFF1D303A},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(night?0xFFFFE8C6:0xFFFFCC9D);c.drawCircle(w*.72f,h*.23f,h*.075f,p);int[] colors={0xFF747786,0xFF414E61,0xFF233C47,0xFF172A32};for(int i=0;i<4;i++){Path path=new Path();path.moveTo(0,h);path.lineTo(0,h*(.36f+i*.14f));for(int j=0;j<=8;j++)path.lineTo(w*j/8,h*(.38f+i*.13f-(j%3)*.045f));path.lineTo(w,h);path.close();p.setColor(colors[i]);c.drawPath(path,p);}p.setShader(new LinearGradient(0,h*.5f,0,h,0x00000000,0xAA000000,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);}
    }
}
