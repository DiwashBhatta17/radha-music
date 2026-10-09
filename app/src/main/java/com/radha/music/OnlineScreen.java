package com.radha.music;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class OnlineScreen extends LinearLayout {
    public interface Host {
        void play(MediaEntry e,List<MediaEntry> queue);void menu(MediaEntry e);void image(MediaEntry e,ImageView target);void preferences();
        default void downloads(){} default Map<String,List<MediaEntry>> playlists(){return Collections.emptyMap();}
    }
    interface SearchSource {OnlineClient.Results search(String query,boolean watch)throws Exception;OnlineClient.Results more(OnlineClient.Results previous)throws Exception;}
    private static final SearchSource YOUTUBE=new SearchSource(){public OnlineClient.Results search(String q,boolean w)throws Exception{return SessionCatalog.INSTANCE.search(q,w);}public OnlineClient.Results more(OnlineClient.Results p)throws Exception{return SessionCatalog.INSTANCE.more(p);}};
    private final SearchSource source;private final Host host;private final OnlineStore store;
    private final boolean watch;private final int ink,muted,surface;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final ExecutorService worker=Executors.newFixedThreadPool(2);private Future<?> request;
    private final LinearLayout body,results;private final ScrollView scroll;
    private final List<MediaEntry> tracks=new ArrayList<>();private OnlineClient.Results result;
    private String query="";private int generation,autoPages;private boolean closed,busy,homeMode;
    public OnlineScreen(Context c,boolean w,boolean d,OnlineStore s,Host h){this(c,w,d,s,h,YOUTUBE);}
    OnlineScreen(Context c,boolean w,boolean d,OnlineStore s,Host h,SearchSource source){
        super(c);this.source=source;watch=w;store=s;host=h;ink=d?0xFFF8F6F2:0xFF171819;muted=d?0xFFA4A4A6:0xFF727274;surface=d?0xFF202123:0xFFF0ECE7;
        setOrientation(VERTICAL);scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);body=column();results=column();body.setPadding(dp(4),dp(12),dp(4),dp(20));scroll.addView(body);addView(scroll,new LayoutParams(-1,-1));showHome();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(getContext());v.setOrientation(VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(getContext());v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable shape(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(14));return g;}
    private void gap(int height){body.addView(new View(getContext()),new LayoutParams(1,dp(height)));}
    private TextView action(String label,Runnable run){TextView t=text(label,13,ink,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(10),dp(12),dp(10));t.setMinHeight(dp(48));t.setBackground(shape(surface));t.setOnClickListener(v->run.run());return t;}
    private void heading(String label,Runnable more){LinearLayout line=row();line.addView(text(label,21,ink,true),new LayoutParams(0,-2,1));if(more!=null)line.addView(action("Show more ›",more));body.addView(line);gap(10);}
    private String seed(){return SessionCatalog.INSTANCE.seed(store,watch);}
    private void chips(){HorizontalScrollView sc=new HorizontalScrollView(getContext());sc.setHorizontalScrollBarEnabled(false);LinearLayout line=row();for(String label:watch?new String[]{"For you","Music videos","Live sessions","Acoustic"}:new String[]{"For you","Chill","Hindi","Nepali","Marathi"}){LayoutParams p=new LayoutParams(-2,-2);p.rightMargin=dp(8);line.addView(action(label,()->search(label.equals("For you")?seed():label+(watch?" music video":" songs"))),p);}sc.addView(line);body.addView(sc);gap(18);}
    public void showHome(){
        homeMode=true;query=seed();autoPages=0;body.removeAllViews();scroll.scrollTo(0,0);chips();
        List<MediaEntry> speed=store.speedDials(watch);
        if(!watch){heading("Speed Dials",null);if(speed.isEmpty())body.addView(text("Your songs appear here after 3 plays.",13,muted,false));else speedGrid(speed);gap(18);}
        else {List<MediaEntry> recent=new ArrayList<>();for(MediaEntry e:store.recent())if(e.video)recent.add(e);if(!recent.isEmpty()){heading("Listen again",null);rail(recent);gap(20);}}
        heading(watch?"Recommended videos":"Quick Picks",this::showAll);body.addView(results);gap(22);
        List<MediaEntry> forgotten=store.forgotten(watch);heading("Forgotten Favorites",null);if(forgotten.isEmpty())body.addView(text("Old favorites return after 14 days away.",13,muted,false));else rail(forgotten);gap(20);
        heading("Your playlists",null);playlistRail();gap(20);
        heading("Made for you",null);HorizontalScrollView mixes=new HorizontalScrollView(getContext());mixes.setHorizontalScrollBarEnabled(false);LinearLayout mixRow=row();for(String mood:new String[]{"Acoustic days","Late night","Fresh discoveries"}){LinearLayout tile=column();FrameLayout art=new FrameLayout(getContext());art.setBackground(shape(surface));art.setClipToOutline(true);art.addView(new Landscape(getContext(),mood.equals("Late night")),new FrameLayout.LayoutParams(-1,-1));tile.addView(art,new LayoutParams(dp(152),dp(110)));tile.addView(text(mood,14,ink,true));tile.addView(text("Explore a mix",11,muted,false));tile.setOnClickListener(v->search(store.preference()+" "+mood+" songs"));LayoutParams lp=new LayoutParams(dp(164),-2);mixRow.addView(tile,lp);}mixes.addView(mixRow);body.addView(mixes);gap(18);
        body.addView(action("↓  Downloads · Only in Radha",host::downloads));gap(8);body.addView(action("Listening preferences ›",host::preferences));load(query,false);
    }
    private void showAll(){if(tracks.isEmpty()){search(query);return;}homeMode=false;body.removeAllViews();body.addView(action("‹ Discover",this::showHome));gap(14);heading(watch?"All videos":"All quick picks",null);body.addView(results);renderResults();scroll.scrollTo(0,0);}
    public void search(String value){if(value.trim().isEmpty()){showHome();return;}query=value.trim();homeMode=false;body.removeAllViews();body.addView(action("‹ Discover",this::showHome));gap(14);body.addView(text(query,23,ink,true));gap(14);body.addView(results);scroll.scrollTo(0,0);load(query,false);}
    private void load(String q,boolean append){
        if(closed)return;if(request!=null)request.cancel(true);int token=++generation;busy=true;if(!append){tracks.clear();result=null;results.removeAllViews();}
        if(!append&&source==YOUTUBE){OnlineClient.Results cached=SessionCatalog.INSTANCE.peek(q,watch);if(cached!=null){accept(cached,q);return;}}
        RadhaLoadingView loading=new RadhaLoadingView(getContext());results.addView(loading,new LayoutParams(-1,dp(56)));OnlineClient.Results previous=result;
        request=worker.submit(()->{try{OnlineClient.Results found=append?source.more(previous):source.search(q,watch);
            main.post(()->{if(closed||token!=generation)return;accept(found,q);});
        }catch(Exception|LinkageError error){main.post(()->{if(closed||token!=generation)return;busy=false;results.removeView(loading);failed(error,q);});}});
        main.postDelayed(()->{if(closed||!busy||token!=generation)return;busy=false;generation++;request.cancel(true);results.removeView(loading);failed(new java.net.SocketTimeoutException("Search exceeded 90 seconds"),q);},90000);
    }
    private void accept(OnlineClient.Results found,String q){
        busy=false;result=found;List<MediaEntry> batch=new ArrayList<>();Set<String> seen=new HashSet<>();for(MediaEntry e:tracks)seen.add(e.id);
        for(OnlineClient.Track t:found.tracks()){MediaEntry e=new MediaEntry(OnlineRules.mediaId(t.url(),watch),t.name(),"YouTube",t.artist(),watch,t.seconds()*1000,System.currentTimeMillis()/1000,t.image());batch.add(e);if(seen.add(e.id))tracks.add(e);}
        store.remember(batch);renderResults();if(homeMode&&!watch&&tracks.size()<25&&OnlineClient.hasMore(result)&&autoPages++<4)load(q,true);
    }
    private void renderResults(){results.removeAllViews();if(tracks.isEmpty()){results.addView(text("No results. Try a different artist or title.",15,muted,false));return;}
        if(homeMode&&!watch){List<View> pages=new ArrayList<>();for(List<MediaEntry> page:DiscoveryRules.pages(tracks,5,5)){LinearLayout panel=column();for(MediaEntry e:page)panel.addView(trackRow(e));pages.add(panel);}results.addView(pager(pages));}
        else if(homeMode){results.addView(makeRail(tracks.subList(0,Math.min(15,tracks.size()))));}
        else {for(MediaEntry e:tracks)results.addView(trackRow(e));if(OnlineClient.hasMore(result))results.addView(action("Load more",()->{if(!busy)load(query,true);}));}
    }
    private void failed(Throwable error,String q){results.addView(text(OnlineErrors.message(error),14,muted,false));results.addView(action("Retry search",()->{if(homeMode)showHome();else search(q);}));results.addView(action("Error details",()->{
        String detail="Radha Music 1.4\nAndroid "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\n"+OnlineErrors.details(error);
        new android.app.AlertDialog.Builder(getContext()).setTitle("Online connection details").setMessage(detail).setPositiveButton("Close",null).setNeutralButton("Copy error",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Radha Music error",detail));Toast.makeText(getContext(),"Error copied",Toast.LENGTH_SHORT).show();}).show();}));}
    private ImageView artwork(MediaEntry e){ImageView image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(shape(surface));image.setClipToOutline(true);host.image(e,image);return image;}
    private View trackRow(MediaEntry e){LinearLayout line=row();line.setPadding(0,dp(5),0,dp(5));line.addView(artwork(e),new LayoutParams(dp(watch?96:46),dp(watch?60:46)));LinearLayout labels=column();labels.setPadding(dp(10),0,dp(4),0);TextView title=text(e.name,14,ink,true);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);TextView artist=text(e.artist,12,muted,false);artist.setMaxLines(1);labels.addView(artist);line.addView(labels,new LayoutParams(0,-2,1));TextView menu=action("⋮",()->host.menu(e));menu.setContentDescription("Options for "+e.name);menu.setBackgroundColor(Color.TRANSPARENT);line.addView(menu,new LayoutParams(dp(44),dp(48)));line.setOnClickListener(v->host.play(e,new ArrayList<>(tracks)));return line;}
    private View tile(MediaEntry e,List<MediaEntry> queue,int width,boolean compact){LinearLayout tile=column();tile.setPadding(0,0,dp(8),0);ImageView image=artwork(e);tile.addView(image,new LayoutParams(-1,dp(compact?76:watch?100:124)));TextView title=text(e.name,compact?11:13,ink,true);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(title);tile.setContentDescription(e.name+", "+e.artist);tile.setOnClickListener(v->host.play(e,queue));tile.setOnLongClickListener(v->{host.menu(e);return true;});return tile;}
    private void speedGrid(List<MediaEntry> entries){List<View> pages=new ArrayList<>();for(List<MediaEntry> page:DiscoveryRules.pages(entries,6,5)){LinearLayout panel=column();for(int start=0;start<page.size();start+=3){LinearLayout line=row();for(int col=0;col<3;col++){int index=start+col;line.addView(index<page.size()?tile(page.get(index),entries,0,true):new View(getContext()),new LayoutParams(0,dp(118),1));}panel.addView(line);}pages.add(panel);}body.addView(pager(pages));}
    private void rail(List<MediaEntry> entries){body.addView(makeRail(entries));}
    private View makeRail(List<MediaEntry> entries){HorizontalScrollView sc=new HorizontalScrollView(getContext());sc.setHorizontalScrollBarEnabled(false);LinearLayout line=row();for(MediaEntry e:entries)line.addView(tile(e,entries,watch?188:144,false),new LayoutParams(dp(watch?188:144),-2));sc.addView(line);return sc;}
    private void playlistRail(){HorizontalScrollView sc=new HorizontalScrollView(getContext());sc.setHorizontalScrollBarEnabled(false);LinearLayout line=row();for(var entry:host.playlists().entrySet()){List<MediaEntry> filtered=new ArrayList<>();for(MediaEntry e:entry.getValue())if(e.video==watch)filtered.add(e);if(filtered.isEmpty())continue;LinearLayout tile=column();tile.addView(artwork(filtered.get(0)),new LayoutParams(dp(144),dp(112)));tile.addView(text(entry.getKey(),14,ink,true));tile.addView(text(filtered.size()+" tracks",11,muted,false));tile.setOnClickListener(v->host.play(filtered.get(0),filtered));LayoutParams lp=new LayoutParams(dp(156),-2);line.addView(tile,lp);}if(line.getChildCount()==0)body.addView(text("Create playlists from a song’s ⋮ menu.",13,muted,false));else{sc.addView(line);body.addView(sc);}}
    private View pager(List<View> pages){LinearLayout group=column();TextView dots=text("",13,0xFFFF795F,true);dots.setGravity(Gravity.CENTER);SnapCarousel carousel=new SnapCarousel(getContext(),pages,dots);group.addView(carousel);if(pages.size()>1)group.addView(dots);return group;}
    private final class SnapCarousel extends HorizontalScrollView {
        final LinearLayout line;final TextView dots;final int count;int pageWidth;boolean didFling;
        SnapCarousel(Context c,List<View> pages,TextView dots){super(c);this.dots=dots;count=pages.size();setHorizontalScrollBarEnabled(false);line=row();pageWidth=Math.max(dp(250),getResources().getDisplayMetrics().widthPixels-dp(44));for(View v:pages)line.addView(v,new LayoutParams(pageWidth,-2));addView(line);setContentDescription("Swipe left or right through "+count+" pages");updateDots(0);}
        void updateDots(int selected){StringBuilder b=new StringBuilder();for(int i=0;i<count;i++)b.append(i==selected?"● ":"○ ");dots.setText(b.toString().trim());}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(w>0&&w!=pageWidth){pageWidth=w;for(int i=0;i<line.getChildCount();i++){View v=line.getChildAt(i);v.setLayoutParams(new LinearLayout.LayoutParams(w,-2));}}}
        @Override protected void onScrollChanged(int l,int t,int oldl,int oldt){super.onScrollChanged(l,t,oldl,oldt);if(pageWidth>0)updateDots(Math.min(count-1,Math.round(l/(float)pageWidth)));}
        @Override public void fling(int velocity){didFling=true;int page=Math.round(getScrollX()/(float)Math.max(1,pageWidth));smoothScrollTo(Math.max(0,Math.min(count-1,page+(velocity>0?1:-1)))*pageWidth,0);}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)didFling=false;boolean handled=super.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP&&!didFling&&Math.abs(getScrollX()%Math.max(1,pageWidth))>0)post(()->smoothScrollTo(Math.round(getScrollX()/(float)Math.max(1,pageWidth))*pageWidth,0));return handled;}
    }
    public void close(){closed=true;generation++;main.removeCallbacksAndMessages(null);worker.shutdownNow();}
    /** Original landscape artwork echoes the approved sunset covers without bundling large images. */
    public static final class Landscape extends View {
        private final Paint p=new Paint(3);private final boolean night;
        public Landscape(Context c,boolean night){super(c);this.night=night;}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new LinearGradient(0,0,0,h,night?new int[]{0xFF1E344B,0xFF847079,0xFF15252B}:new int[]{0xFFFFA379,0xFFA6747E,0xFF1D303A},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(night?0xFFFFE8C6:0xFFFFCC9D);c.drawCircle(w*.72f,h*.23f,h*.075f,p);int[] colors={0xFF747786,0xFF414E61,0xFF233C47,0xFF172A32};for(int i=0;i<4;i++){Path path=new Path();path.moveTo(0,h);path.lineTo(0,h*(.36f+i*.14f));for(int j=0;j<=8;j++)path.lineTo(w*j/8,h*(.38f+i*.13f-(j%3)*.045f));path.lineTo(w,h);path.close();p.setColor(colors[i]);c.drawPath(path,p);}p.setShader(new LinearGradient(0,h*.5f,0,h,0x00000000,0xAA000000,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);}
    }
}
