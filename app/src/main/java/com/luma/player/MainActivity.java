package com.radha.music;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.*;
import androidx.media3.session.MediaController;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private LibraryStore library;
    private ArtworkStore artwork;
    private DiscView disc;
    private LinearLayout transport;
    private Symbol playGlyph;
    private TextView speedButton;
    private String pendingArtworkId;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService scanner=Executors.newSingleThreadExecutor();
    private final ExecutorService thumbnails=Executors.newFixedThreadPool(2);
    private final android.util.LruCache<String,Bitmap> imageCache=new android.util.LruCache<>(60);
    private ListenableFuture<MediaController> controllerFuture;
    private ExoPlayer player;
    private LinearLayout root, content, mini, controls, playerHeader;
    private FrameLayout playerFrame;
    private PlayerView videoView;
    private ListView listView;
    private TextView nowTitle, nowSubtitle, timeLabel, playButton, favoriteButton, backgroundButton, lockButton, feedback;
    private SeekBar progress;
    private final List<Object> rows=new ArrayList<>();
    private List<MediaEntry> visible=new ArrayList<>();
    private String section="Videos", folder=null, collection=null, search="";
    private int bg, surface, card, ink, muted, accent;
    private boolean dark, full=false, locked=false, controlsVisible=true, seeking=false, scanning=false, mutedAudio=false;
    private String scanError="";
    private long lastBack;
    private final Player.Listener listener=new Player.Listener(){
        @Override public void onEvents(Player p, Player.Events events){updatePlayback();}
        @Override public void onVideoSizeChanged(VideoSize size){syncVideoOrientation(size); }
        @Override public void onMediaItemTransition(MediaItem item,int reason){if(full)renderPlayer();else updateMini();}
        @Override public void onPlayerError(PlaybackException error){new AlertDialog.Builder(MainActivity.this).setTitle("This file couldn’t play")
            .setMessage("The file may have moved, access may have changed, or this phone may not support its audio/video codec.\n\n"+error.getErrorCodeName())
            .setPositiveButton("OK",null).setNeutralButton("Next",(d,w)->{if(player.hasNextMediaItem())player.seekToNextMediaItem();}).show();}
    };
    private final Runnable ticker=new Runnable(){public void run(){updatePlayback();handler.postDelayed(this,500);}};

    @Override public void onCreate(Bundle state){
        super.onCreate(state); library=new LibraryStore(this); artwork=new ArtworkStore(this); if(state!=null)pendingArtworkId=state.getString("artworkTarget"); colors(); buildLibrary();
        SessionToken token=new SessionToken(this,new ComponentName(this,PlaybackService.class));
        controllerFuture=new MediaController.Builder(this,token).buildAsync();
        controllerFuture.addListener(()->{try{controllerFuture.get();if(isDestroyed()||PlaybackService.instance==null)return;player=PlaybackService.instance.player;player.addListener(listener);updateMini();}catch(Exception e){toast("Playback service could not start. Please reopen Radha Music.");}},this::runOnUiThread);
        handler.post(ticker); requestMedia();
    }
    private PlaybackService service(){return PlaybackService.instance;}
    private void colors(){dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        bg=Color.parseColor(dark?"#111116":"#F8F7FC");surface=Color.parseColor(dark?"#191920":"#FFFFFF");card=Color.parseColor(dark?"#282535":"#ECE7FA");ink=Color.parseColor(dark?"#F6F3FF":"#211D30");muted=Color.parseColor(dark?"#A29DAC":"#797284");accent=Color.parseColor(dark?"#C3ABFF":"#7451C9");}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");return t;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=text(s,size,color);if(bold)t.setTypeface(null,1);return t;}
    private TextView button(String title,Runnable action){TextView b=text(title,14,ink);b.setGravity(Gravity.CENTER);b.setPadding(dp(14),dp(12),dp(14),dp(12));b.setMinHeight(dp(48));b.setBackground(shape(card,16));b.setOnClickListener(v->action.run());return b;}
    private void gap(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private void addWeighted(LinearLayout row,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(child,p);}
    private void immersive(){
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController c=getWindow().getInsetsController();if(c!=null){c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);c.hide(WindowInsets.Type.systemBars());}}
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
    }
    private void insetRoot(){
        immersive();root.setOnApplyWindowInsetsListener((v,insets)->{
            int left=0,right=0,top=0,bottom=0;
            if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout c=insets.getDisplayCutout();left=c.getSafeInsetLeft();right=c.getSafeInsetRight();top=c.getSafeInsetTop();bottom=c.getSafeInsetBottom();}
            if(!full)v.setPadding(left,top,right,bottom);
            else {v.setPadding(0,0,0,0);if(playerHeader!=null)playerHeader.setPadding(dp(12)+left,dp(10)+top,dp(78)+right,dp(12));if(controls!=null)controls.setPadding(dp(22)+left,dp(12),dp(22)+right,dp(12)+bottom);if(lockButton!=null&&lockButton.getLayoutParams() instanceof FrameLayout.LayoutParams lp){lp.topMargin=dp(10)+top;lp.rightMargin=dp(8)+right;lockButton.setLayoutParams(lp);}}
            return insets;
        });root.requestApplyInsets();
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();}

    private void requestMedia(){
        List<String> needed=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=33){if(checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.READ_MEDIA_VIDEO);if(checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.READ_MEDIA_AUDIO);if(Build.VERSION.SDK_INT>=34)needed.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);}
        else if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(!needed.isEmpty())requestPermissions(needed.toArray(new String[0]),10);else scan();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==10)scan();}
    private void scan(){if(scanning)return;scanning=true;scanError="";renderRows();scanner.execute(()->{List<MediaEntry> found;try{found=library.scan();}catch(Exception e){found=Collections.emptyList();scanError="Couldn’t read the library. Check media access and try again.";}List<MediaEntry> result=found;runOnUiThread(()->{if(isDestroyed())return;library.entries.clear();library.entries.addAll(result);scanning=false;if(!full)renderRows();});});}

    private void buildLibrary(){
        full=false;locked=false;handler.removeCallbacks(hideControls);videoViewDetach();if(disc!=null){disc.setPlaying(false);disc=null;}getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);colors();
        root=vertical();root.setBackgroundColor(bg);setContentView(root);insetRoot();
        LinearLayout top=vertical();top.setPadding(dp(24),dp(22),dp(20),dp(6));root.addView(top);
        LinearLayout brand=horizontal();LinearLayout names=vertical();TextView wordmark=label("Radha Music",27,ink,true);wordmark.setLetterSpacing(-.035f);names.addView(wordmark);gap(names,4);names.addView(text("A little closer to what you love.",13,muted));brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));brand.addView(iconButton("settings",muted,()->libraryMenu()));top.addView(brand);gap(top,20);
        EditText find=new EditText(this);find.setSingleLine(true);find.setTextSize(14);find.setTextColor(ink);find.setHintTextColor(muted);find.setHint("Search songs, videos, folders");find.setPadding(dp(18),0,dp(18),0);find.setBackground(shape(surface,16));find.setText(search);top.addView(find,new LinearLayout.LayoutParams(-1,dp(48)));
        find.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int start,int before,int count){search=s.toString();renderRows();}public void afterTextChanged(Editable e){}});
        content=vertical();content.setPadding(dp(18),0,dp(18),0);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        listView=new ListView(this);listView.setDivider(null);listView.setVerticalScrollBarEnabled(false);listView.setClipToPadding(false);listView.setPadding(0,0,0,dp(12));listView.setAdapter(new LibraryAdapter());content.addView(listView,new LinearLayout.LayoutParams(-1,0,1));
        mini=vertical();mini.setPadding(dp(18),dp(4),dp(18),dp(4));root.addView(mini);
        LinearLayout tabs=horizontal();tabs.setGravity(Gravity.CENTER);tabs.setPadding(dp(12),dp(5),dp(12),dp(5));tabs.setBackgroundColor(surface);root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(76)));
        for(String tab:new String[]{"Videos","Music","Saved"}){
            boolean selected=section.equals(tab);int color=selected?accent:muted;
            LinearLayout cell=vertical();cell.setGravity(Gravity.CENTER);cell.setContentDescription(tab);cell.setFocusable(true);cell.setSelected(selected);
            FrameLayout iconBox=new FrameLayout(this);if(selected)iconBox.setBackground(shape(card,14));iconBox.addView(new Symbol(this,tab.equals("Videos")?"video":tab.equals("Music")?"music":"heart",color),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));cell.addView(iconBox,new LinearLayout.LayoutParams(dp(56),dp(30)));
            TextView caption=label(tab,12,color,selected);caption.setGravity(Gravity.CENTER);caption.setIncludeFontPadding(false);LinearLayout.LayoutParams captionParams=new LinearLayout.LayoutParams(-1,dp(22));captionParams.topMargin=dp(3);cell.addView(caption,captionParams);
            tabs.addView(cell,new LinearLayout.LayoutParams(0,-1,1));cell.setOnClickListener(v->{section=tab;folder=null;collection=null;search="";buildLibrary();});
        }
        renderRows();updateMini();
    }
    private void libraryMenu(){new AlertDialog.Builder(this).setTitle("Your library").setItems(new String[]{"Refresh media","Choose an additional folder","Manage media permissions","About Radha Music"},(d,w)->{
        if(w==0)scan();if(w==1){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,21);}if(w==2)startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));
        if(w==3)new AlertDialog.Builder(this).setTitle("Radha Music · 1.2").setMessage("Created, designed and developed by Diwash Bhatta.\n\nYour music. Your movies. On your device.\n\nOriginal files, no conversion or upload. Music is ordered by Android’s date added. Videos stay in their source folders.\n\nSwipe left side vertically for brightness, right side for volume. Double tap either side to seek 6 seconds. Background playback is opt-in.\n\nCodecs and volume boost depend on your device. Boost can distort loud recordings.\n\nBuilt with AndroidX Media3 (Apache 2.0). No ads or Internet permission.").setPositiveButton("Close",null).show();}).show();}
    private void renderRows(){if(full||listView==null)return;rows.clear();visible=new ArrayList<>();
        if(section.equals("Saved")&&collection==null){rows.add("Saved for later");rows.add(new CollectionRow("Favorites",library.favorites.size()));for(String name:library.playlists.keySet())rows.add(new CollectionRow(name,library.playlists.get(name).size()));rows.add(new ActionRow("+  Create playlist / album",()->createPlaylist(null)));}
        else {
            List<MediaEntry> source=new ArrayList<>();
            if(section.equals("Saved")){if("Favorites".equals(collection)){for(MediaEntry e:library.entries)if(library.favorites.contains(e.id))source.add(e);}else source.addAll(library.playlists.getOrDefault(collection,Collections.emptyList()));}
            else for(MediaEntry e:library.entries)if(e.video==section.equals("Videos"))source.add(e);
            String q=search.trim().toLowerCase(Locale.ROOT);
            for(MediaEntry e:source)if((e.video||LibraryRules.showMusic(e.name,e.folder))&&(folder==null||folder.equals(e.folder))&&(q.isEmpty()||(e.name+" "+e.artist+" "+e.folder).toLowerCase(Locale.ROOT).contains(q)))visible.add(e);
            if(section.equals("Videos")&&folder==null&&q.isEmpty()){
                rows.add("Video folders  ·  A–Z");LinkedHashMap<String,List<MediaEntry>> groups=new LinkedHashMap<>();for(MediaEntry e:visible)groups.computeIfAbsent(e.folder,k->new ArrayList<>()).add(e);
                List<String> folders=new ArrayList<>(groups.keySet());folders.sort(Comparator.comparing(LibraryStore::folderName,String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));for(String f:folders)rows.add(new FolderRow(f,groups.get(f)));
            }else{rows.add((collection!=null?"‹  "+collection:folder!=null?"‹  "+LibraryStore.folderName(folder):section.equals("Music")?"All songs  ·  Newest first":"Search results")+"  ·  "+visible.size());rows.addAll(visible);}
        }
        if(rows.size()<=1){rows.add(new EmptyRow(scanning?"Finding your media…":!scanError.isEmpty()?scanError:search.isEmpty()?"Your library starts here":"No matching media",scanning?"Reading files on your device":search.isEmpty()?"Allow access to music and videos, then refresh. You can also choose a folder from the menu.":"Try another title, artist, or folder."));}
        ((BaseAdapter)listView.getAdapter()).notifyDataSetChanged();
    }
    private record FolderRow(String path,List<MediaEntry> items){}
    private record CollectionRow(String name,int count){}
    private record ActionRow(String title,Runnable action){}
    private record EmptyRow(String title,String note){}
    private final class LibraryAdapter extends BaseAdapter {
        public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return p;}
        public View getView(int position,View reusable,android.view.ViewGroup parent){Object item=rows.get(position);
            if(item instanceof String s){LinearLayout h=horizontal();h.setPadding(dp(6),dp(18),dp(6),dp(14));TextView t=label(s,14,muted,true);h.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(folder!=null||collection!=null)h.setOnClickListener(v->{folder=null;collection=null;renderRows();});return h;}
            if(item instanceof EmptyRow e){LinearLayout empty=vertical();empty.setPadding(dp(20),dp(44),dp(20),dp(24));empty.setGravity(Gravity.CENTER);empty.addView(new Symbol(MainActivity.this,"music",accent),new LinearLayout.LayoutParams(dp(60),dp(60)));gap(empty,18);TextView t=label(e.title(),22,ink,true);t.setGravity(Gravity.CENTER);empty.addView(t);gap(empty,12);TextView n=text(e.note(),15,muted);n.setGravity(Gravity.CENTER);empty.addView(n);gap(empty,22);empty.addView(button("Refresh library",()->scan()));return empty;}
            if(item instanceof ActionRow a)return button(a.title(),a.action());
            LinearLayout outer=vertical();outer.setPadding(0,0,0,dp(8));LinearLayout row=horizontal();row.setPadding(dp(12),dp(12),dp(2),dp(12));row.setBackground(shape(surface,16));outer.addView(row,new LinearLayout.LayoutParams(-1,-2));
            FrameLayout art=new FrameLayout(MainActivity.this);art.setBackground(shape(card,14));row.addView(art,new LinearLayout.LayoutParams(dp(58),dp(58)));String symbol=item instanceof FolderRow?"folder":item instanceof CollectionRow?"heart":((MediaEntry)item).video?"video":"music";
            Symbol icon=new Symbol(MainActivity.this,symbol,accent);FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.CENTER);art.addView(icon,ip);
            LinearLayout lines=vertical();lines.setPadding(dp(14),0,dp(8),0);row.addView(lines,new LinearLayout.LayoutParams(0,-2,1));String title,note;
            if(item instanceof FolderRow f){title=LibraryStore.folderName(f.path());note=f.items().size()+" videos";row.setOnClickListener(v->{folder=f.path();renderRows();});}
            else if(item instanceof CollectionRow c){title=c.name();note=c.count()+" items";row.setOnClickListener(v->{collection=c.name();renderRows();});row.setOnLongClickListener(v->{if(!c.name().equals("Favorites"))new AlertDialog.Builder(MainActivity.this).setTitle(c.name()).setItems(new String[]{"Rename","Delete playlist"},(d,w)->{if(w==1){library.playlists.remove(c.name());library.save();renderRows();}else renamePlaylist(c.name());}).show();return true;});}
            else{MediaEntry e=(MediaEntry)item;title=LibraryRules.mediaTitle(e.name);note=(e.video?format(e.duration):e.artist+"  ·  "+format(e.duration))+(library.favorites.contains(e.id)?"  ♥":"");row.setOnClickListener(v->play(e,visible));if(e.video)thumbnail(e,art);else albumThumbnail(e,art);}
            TextView name=label(title,16,ink,true);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);lines.addView(name);gap(lines,5);TextView info=text(note,12,muted);info.setMaxLines(1);info.setEllipsize(TextUtils.TruncateAt.MIDDLE);lines.addView(info);
            if(item instanceof FolderRow f){int fresh=0;for(MediaEntry e:f.items())if(library.isUnplayed(e.id))fresh++;if(fresh>0)row.addView(badge(String.valueOf(fresh)));}
            if(item instanceof MediaEntry e&&e.video&&library.isUnplayed(e.id))row.addView(badge("NEW"));
            if(item instanceof MediaEntry e)row.addView(iconButton("more",muted,()->mediaMenu(e)));
            else row.addView(iconButton("next",muted,()->row.performClick()));return outer;
        }
    }
    private void thumbnail(MediaEntry e,FrameLayout art){if(Build.VERSION.SDK_INT<29)return;ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(shape(card,14));image.setClipToOutline(true);art.addView(image,new FrameLayout.LayoutParams(-1,-1));Bitmap cached=imageCache.get(e.id);if(cached!=null){image.setImageBitmap(cached);return;}thumbnails.execute(()->{try{Bitmap b=getContentResolver().loadThumbnail(Uri.parse(e.id),new android.util.Size(160,160),null);imageCache.put(e.id,b);runOnUiThread(()->{if(!isDestroyed())image.setImageBitmap(b);});}catch(Exception ignored){}});}
    private void play(MediaEntry e,List<MediaEntry> context){if(player==null){toast("Player is starting. Try again in a moment.");return;}List<MediaItem> queue=new ArrayList<>();int index=0;for(MediaEntry m:context){if(m.id.equals(e.id))index=queue.size();queue.add(m.item());}if(queue.isEmpty())queue.add(e.item());player.setMediaItems(queue,index,0);player.prepare();player.play();renderPlayer();}
    private TextView badge(String value){TextView t=label(value,10,Color.WHITE,true);t.setPadding(dp(7),dp(4),dp(7),dp(4));t.setGravity(Gravity.CENTER);t.setBackground(shape(0xFFE5465A,10));t.setContentDescription(value.equals("NEW")?"Unplayed video":value+" unplayed videos");return t;}
    private void albumThumbnail(MediaEntry e,FrameLayout box){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(shape(card,12));image.setClipToOutline(true);image.setVisibility(View.GONE);box.addView(image,new FrameLayout.LayoutParams(-1,-1));artwork.load(e.id,b->{if(!isDestroyed()&&b!=null){image.setImageBitmap(b);image.setVisibility(View.VISIBLE);}});}
    private void mediaMenu(MediaEntry e){boolean fav=library.favorites.contains(e.id);List<String> options=new ArrayList<>(Arrays.asList("Play next","Add to queue",fav?"Remove favorite":"Add to favorites","Add to playlist / album"));if(!e.video){options.add("Add / change album image");if(artwork.hasCustom(e.id))options.add("Use original album image");}if(collection!=null&&!collection.equals("Favorites"))options.add("Remove from this playlist");new AlertDialog.Builder(this).setTitle(LibraryRules.mediaTitle(e.name)).setItems(options.toArray(new String[0]),(d,w)->{String option=options.get(w);if(w<2){if(player==null)return;if(player.getMediaItemCount()==0){player.setMediaItem(e.item());player.prepare();}else player.addMediaItem(w==0?PlaybackRules.insertNext(player.getCurrentMediaItemIndex(),player.getMediaItemCount()):player.getMediaItemCount(),e.item());toast(w==0?"Will play next":"Added to queue");updateMini();}else if(w==2)toggleFavorite(e.id);else if(w==3)choosePlaylist(e);else if(option.equals("Add / change album image"))pickArtwork(e.id);else if(option.equals("Use original album image")){artwork.remove(e.id);renderRows();}else if(option.equals("Remove from this playlist")){library.playlists.get(collection).removeIf(m->m.id.equals(e.id));library.save();renderRows();}}).show();}
    private void pickArtwork(String id){pendingArtworkId=id;Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(pick,23);}

    private void toggleFavorite(String id){if(!library.favorites.add(id))library.favorites.remove(id);library.save();if(full)updatePlayback();else renderRows();}
    private void choosePlaylist(MediaEntry e){List<String> names=new ArrayList<>(library.playlists.keySet());names.add("+ Create playlist / album");new AlertDialog.Builder(this).setTitle("Save to…").setItems(names.toArray(new String[0]),(d,w)->{if(w==names.size()-1)createPlaylist(e);else addToPlaylist(names.get(w),e);}).show();}
    private void createPlaylist(MediaEntry entry){EditText field=new EditText(this);field.setSingleLine();field.setHint("Playlist or album name");AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Make a collection").setView(field).setPositiveButton("Create",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String name=field.getText().toString().trim();if(name.isEmpty()||name.equalsIgnoreCase("Favorites")||library.playlists.containsKey(name)){field.setError("Choose a new name");return;}library.playlists.put(name,new ArrayList<>());library.save();if(entry!=null)addToPlaylist(name,entry);else renderRows();dialog.dismiss();}));dialog.show();}
    private void renamePlaylist(String old){EditText field=new EditText(this);field.setText(old);AlertDialog d=new AlertDialog.Builder(this).setTitle("Rename collection").setView(field).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String n=field.getText().toString().trim();if(n.isEmpty()||n.equalsIgnoreCase("Favorites")||library.playlists.containsKey(n)){field.setError("Choose a new name");return;}library.playlists.put(n,library.playlists.remove(old));library.save();renderRows();d.dismiss();}));d.show();}
    private void addToPlaylist(String name,MediaEntry e){List<MediaEntry> list=library.playlists.get(name);if(list.stream().noneMatch(m->m.id.equals(e.id)))list.add(e);library.save();toast("Saved to "+name);if(!full)renderRows();}
    private void updateMini(){if(full||mini==null)return;mini.removeAllViews();if(player==null||player.getCurrentMediaItem()==null)return;LinearLayout bar=horizontal();bar.setPadding(dp(14),dp(8),dp(5),dp(8));bar.setBackground(shape(card,20));mini.addView(bar);bar.addView(new Symbol(this,"music",accent),new LinearLayout.LayoutParams(dp(30),dp(30)));LinearLayout info=vertical();info.setPadding(dp(12),0,dp(6),0);TextView title=label(currentTitle(),14,ink,true);title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);info.addView(title);info.addView(text("Tap to open player",12,muted));bar.addView(info,new LinearLayout.LayoutParams(0,-2,1));bar.setOnClickListener(v->renderPlayer());bar.addView(iconButton(player.isPlaying()?"pause":"play",ink,()->togglePlay()));bar.addView(iconButton("next",ink,()->next()));}

    private String currentTitle(){MediaItem m=player==null?null:player.getCurrentMediaItem();return m==null?"Nothing playing":LibraryRules.mediaTitle(String.valueOf(m.mediaMetadata.title));}
    private boolean currentVideo(){MediaItem m=player==null?null:player.getCurrentMediaItem();return m!=null&&Objects.equals(m.mediaMetadata.mediaType,MediaMetadata.MEDIA_TYPE_VIDEO);}
    private void videoViewDetach(){if(videoView!=null){videoView.setPlayer(null);videoView=null;}}
    private final Runnable hideControls=()->{if(full&&currentVideo()&&player!=null&&player.isPlaying()&&!locked&&!seeking){controlsVisible=false;applyControls();}};
    private void scheduleHide(){handler.removeCallbacks(hideControls);if(full&&currentVideo()&&player!=null&&player.isPlaying())handler.postDelayed(hideControls,3500);}
    private View transportIcon(String icon,String description,int size,Runnable action){FrameLayout button=(FrameLayout)iconButton(icon,Color.WHITE,()->{action.run();scheduleHide();});button.setContentDescription(description);Symbol glyph=(Symbol)button.getChildAt(0);glyph.setLayoutParams(new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));if(icon.equals("play")||icon.equals("pause"))playGlyph=glyph;button.setMinimumHeight(dp(56));return button;}
    private GradientDrawable fade(boolean top){return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,top?new int[]{0xCC000000,0x00000000}:new int[]{0x00000000,0xCC000000});}
    private void renderPlayer(){
        if(player==null||player.getCurrentMediaItem()==null)return;boolean wasLocked=full&&locked;full=true;locked=wasLocked;controlsVisible=!locked;videoViewDetach();if(disc!=null){disc.setPlaying(false);disc=null;}getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        boolean video=currentVideo(), landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        root=vertical();root.setBackgroundColor(Color.BLACK);setContentView(root);playerFrame=new FrameLayout(this);root.addView(playerFrame,new LinearLayout.LayoutParams(-1,-1));
        if(video){videoView=new PlayerView(this);videoView.setUseController(false);videoView.setPlayer(player);videoView.setKeepContentOnPlayerReset(true);videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);playerFrame.addView(videoView,new FrameLayout.LayoutParams(-1,-1));}
        else {playerFrame.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF342B47,0xFF111018}));LinearLayout album=vertical();album.setGravity(Gravity.CENTER);FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(landscape?getResources().getDisplayMetrics().widthPixels/2:-1,-1,Gravity.LEFT);ap.topMargin=dp(75);ap.bottomMargin=landscape?dp(20):dp(310);playerFrame.addView(album,ap);disc=new DiscView(this);album.addView(disc,new LinearLayout.LayoutParams(-1,0,1));TextView hint=text("Swipe left for next · right for previous",11,0xFFBDB1CD);hint.setGravity(Gravity.CENTER);album.addView(hint);String id=player.getCurrentMediaItem().mediaId;DiscView target=disc;artwork.load(id,b->{if(!isDestroyed()&&disc==target)target.setArtwork(b);});disc.setPlaying(player.isPlaying());}
        View gestures=new View(this);playerFrame.addView(gestures,new FrameLayout.LayoutParams(-1,-1));attachGestures(gestures);
        playerHeader=horizontal();playerHeader.setPadding(dp(12),dp(12),dp(12),dp(18));if(video)playerHeader.setBackground(fade(true));playerHeader.addView(iconButton("back",Color.WHITE,()->leavePlayer()));LinearLayout heading=vertical();nowTitle=label(video?currentTitle():"NOW PLAYING",video?16:12,Color.WHITE,true);nowTitle.setMaxLines(1);nowTitle.setEllipsize(TextUtils.TruncateAt.END);heading.addView(nowTitle);nowSubtitle=text("",11,0xFFCDC6D7);heading.addView(nowSubtitle);playerHeader.addView(heading,new LinearLayout.LayoutParams(0,-2,1));if(video)playerHeader.addView(iconButton("rotate",Color.WHITE,()->{setRequestedOrientation(landscape?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);}));else playerHeader.addView(iconButton("more",Color.WHITE,()->playerMenu()));playerFrame.addView(playerHeader,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        controls=vertical();controls.setPadding(dp(22),dp(14),dp(22),dp(12));if(video)controls.setBackground(fade(false));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(!video&&landscape?getResources().getDisplayMetrics().widthPixels/2:-1,-2,Gravity.BOTTOM|Gravity.RIGHT);playerFrame.addView(controls,cp);
        favoriteButton=text("♡",28,0xFFCCB6FF);favoriteButton.setContentDescription("Toggle favorite");favoriteButton.setGravity(Gravity.CENTER);favoriteButton.setOnClickListener(v->toggleFavorite(player.getCurrentMediaItem().mediaId));
        if(!video){LinearLayout titleRow=horizontal();LinearLayout titleText=vertical();TextView title=label(currentTitle(),25,Color.WHITE,true);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);titleText.addView(title);MediaEntry entry=findCurrent();titleText.addView(text(entry!=null?entry.artist:"On your device",14,0xFFBDB1CD));titleRow.addView(titleText,new LinearLayout.LayoutParams(0,-2,1));titleRow.addView(favoriteButton,new LinearLayout.LayoutParams(dp(48),dp(48)));controls.addView(titleRow);gap(controls,12);}
        progress=new SeekBar(this);progress.setMax(1000);progress.setPadding(dp(8),0,dp(8),0);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFCEB9FF));progress.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));controls.addView(progress,new LinearLayout.LayoutParams(-1,dp(36)));
        timeLabel=text("",11,0xFFDDD4E6);timeLabel.setPadding(dp(8),0,dp(8),0);controls.addView(timeLabel);
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){seeking=true;handler.removeCallbacks(hideControls);}public void onStopTrackingTouch(SeekBar s){if(player.getDuration()>0)player.seekTo(player.getDuration()*s.getProgress()/1000);seeking=false;scheduleHide();}public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&player.getDuration()>0)timeLabel.setText(format(player.getDuration()*p/1000)+"  /  "+format(player.getDuration()));}});
        transport=horizontal();transport.setGravity(Gravity.CENTER);addWeighted(transport,transportIcon("previous","Previous",26,()->previous()));addWeighted(transport,transportIcon("rewind","Back 6 seconds",28,()->seek(-6000)));addWeighted(transport,transportIcon(player.isPlaying()?"pause":"play","Play or pause",44,()->togglePlay()));addWeighted(transport,transportIcon("forward","Forward 6 seconds",28,()->seek(6000)));addWeighted(transport,transportIcon("skip","Next",26,()->next()));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(video?60:72));controls.addView(transport,tp);
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(true);LinearLayout tools=horizontal();tools.setGravity(Gravity.CENTER);scroll.addView(tools);controls.addView(scroll);
        backgroundButton=playerAction("",()->toggleBackground());backgroundButton.setTextSize(12);tools.addView(backgroundButton);
        speedButton=playerAction("1×",()->speedDialog());tools.addView(speedButton);tools.addView(iconButton("queue",Color.WHITE,()->queueDialog()));tools.addView(iconButton("more",Color.WHITE,()->playerMenu()));
        if(video){tools.addView(iconButton("captions",Color.WHITE,()->trackMenu()));tools.addView(iconButton("fullscreen",Color.WHITE,()->toggleFullscreen()));tools.addView(favoriteButton,new LinearLayout.LayoutParams(dp(48),dp(48)));}
        else {TextView credit=text("Created, designed and developed by Diwash Bhatta",10,0xFFA99CB9);credit.setGravity(Gravity.CENTER);credit.setPadding(0,dp(8),0,dp(6));controls.addView(credit);}
        feedback=label("",16,Color.WHITE,true);feedback.setGravity(Gravity.CENTER);feedback.setPadding(dp(18),dp(12),dp(18),dp(12));feedback.setBackground(shape(0xDD24202E,16));feedback.setVisibility(View.GONE);playerFrame.addView(feedback,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
        lockButton=text(locked?"Unlock":"Lock",12,Color.WHITE);lockButton.setGravity(Gravity.CENTER);lockButton.setPadding(dp(10),dp(12),dp(10),dp(12));lockButton.setContentDescription("Lock or unlock controls");lockButton.setOnClickListener(v->{locked=!locked;controlsVisible=!locked;lockButton.setText(locked?"Unlock":"Lock");applyControls();scheduleHide();});FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(64),dp(48),Gravity.RIGHT|Gravity.TOP);lp.rightMargin=dp(8);lp.topMargin=dp(10);playerFrame.addView(lockButton,lp);
        insetRoot();updatePlayback();applyControls();scheduleHide();if(video&&player.isPlaying())syncVideoOrientation(player.getVideoSize());
    }
    private void syncVideoOrientation(VideoSize size){
        if(!full||!currentVideo())return;
        int kind=LibraryRules.videoOrientation(size.width,size.height,size.pixelWidthHeightRatio);if(kind==0)return;
        int desired=kind==1?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        if(getRequestedOrientation()!=desired)setRequestedOrientation(desired);
    }
    private void toggleFullscreen(){
        if(videoView!=null)videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        syncVideoOrientation(player.getVideoSize());immersive();controlsVisible=false;applyControls();
        toast("Fullscreen · original aspect ratio");
    }
    private void speedDialog(){handler.removeCallbacks(hideControls);float[] speeds={.2f,.3f,.5f,.8f,1f,1.25f,1.5f,2f,3f};String[] names={"0.2×","0.3×","0.5×","0.8×","1× · Normal","1.25×","1.5×","2×","3×"};int selected=4;for(int i=0;i<speeds.length;i++)if(Math.abs(player.getPlaybackParameters().speed-speeds[i])<.01)selected=i;AlertDialog d=new AlertDialog.Builder(this).setTitle("Playback speed").setSingleChoiceItems(names,selected,(a,w)->{player.setPlaybackSpeed(speeds[w]);updatePlayback();a.dismiss();}).setNegativeButton("Close",null).create();d.setOnDismissListener(a->scheduleHide());d.show();}

    private TextView playerAction(String name,Runnable action){TextView t=text(name,13,Color.WHITE);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));t.setPadding(dp(12),dp(10),dp(12),dp(10));t.setOnClickListener(v->action.run());return t;}
    private void updatePlayback(){if(player==null)return;if(!full){if(mini!=null&&mini.getChildCount()>0){View bar=mini.getChildAt(0);if(bar instanceof LinearLayout l&&l.getChildCount()>=3){View b=l.getChildAt(2);if(b instanceof FrameLayout f&&f.getChildAt(0) instanceof Symbol icon){icon.kind=player.isPlaying()?"pause":"play";icon.invalidate();}}}return;}
        if(playGlyph==null)return;playGlyph.kind=player.isPlaying()?"pause":"play";playGlyph.invalidate();if(disc!=null)disc.setPlaying(player.isPlaying());if(!player.isPlaying()&&!locked&&!controlsVisible){controlsVisible=true;applyControls();}
        if(!seeking){long duration=player.getDuration();progress.setProgress(duration>0?(int)(player.getCurrentPosition()*1000/duration):0);timeLabel.setText(format(player.getCurrentPosition())+"  /  "+format(duration));}
        backgroundButton.setText(service()!=null&&service().background?"BG on":"BG off");backgroundButton.setContentDescription(service()!=null&&service().background?"Background playback enabled":"Background playback disabled");backgroundButton.setTextColor(service()!=null&&service().background?0xFFCEB9FF:Color.WHITE);
        speedButton.setText(String.format(Locale.US,"%s×",new java.text.DecimalFormat("0.##").format(player.getPlaybackParameters().speed)));
        MediaItem m=player.getCurrentMediaItem();favoriteButton.setText(m!=null&&library.favorites.contains(m.mediaId)?"♥":"♡");
        Format f=player.getVideoFormat();String quality=currentVideo()?(f!=null&&f.height>0?f.width+" × "+f.height+" · Original":"Original video"):"RADHA MUSIC";nowSubtitle.setText(quality);
    }

    private void togglePlay(){if(player==null)return;if(player.isPlaying())player.pause();else{if(player.getPlaybackState()==Player.STATE_ENDED)player.seekToDefaultPosition();player.prepare();player.play();}updatePlayback();}
    private void seek(long delta){if(player==null)return;player.seekTo(PlaybackRules.seek(player.getCurrentPosition(),delta,player.getDuration()));showFeedback(delta>0?"+6 seconds":"−6 seconds");}
    private void next(){if(player.hasNextMediaItem()){player.seekToNextMediaItem();player.play();}else toast("End of queue");}
    private void previous(){if(player.hasPreviousMediaItem())player.seekToPreviousMediaItem();else player.seekTo(0);}
    private void toggleBackground(){if(service()==null)return;service().background=!service().background;if(service().background&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);toast(service().background?"Background enabled, including screen off":"Playback will pause when you leave the app");updatePlayback();}
    private void playerMenu(){String mute=mutedAudio?"Unmute":"Mute";new AlertDialog.Builder(this).setTitle("Playback options").setItems(new String[]{"Volume & boost",mute,"Playback order / repeat","Add to playlist / album","Playback speed","Attach subtitle file","Browse library",currentVideo()?"Audio & subtitles":"Add / change album image"},(d,w)->{switch(w){case 0:volumeDialog();break;case 1:mutedAudio=!mutedAudio;player.setVolume(mutedAudio?0:1);toast(mutedAudio?"Muted":"Sound on");break;case 2:orderDialog();break;case 3:MediaEntry e=findCurrent();if(e!=null)choosePlaylist(e);break;case 4:speedDialog();break;case 5:Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,22);break;case 6:leavePlayer();break;case 7:if(currentVideo())trackMenu();else pickArtwork(player.getCurrentMediaItem().mediaId);break;}}).show();}
    private MediaEntry findCurrent(){MediaItem m=player.getCurrentMediaItem();if(m==null)return null;for(MediaEntry e:library.entries)if(e.id.equals(m.mediaId))return e;for(List<MediaEntry> es:library.playlists.values())for(MediaEntry e:es)if(e.id.equals(m.mediaId))return e;return null;}
    private void orderDialog(){int choice=player.getRepeatMode()==Player.REPEAT_MODE_ONE?2:player.getRepeatMode()==Player.REPEAT_MODE_ALL?3:service().autoNext?1:0;new AlertDialog.Builder(this).setTitle("When this ends").setSingleChoiceItems(new String[]{"Stop after current item","Auto-next through this queue","Repeat this item","Repeat entire queue"},choice,(d,w)->{service().setAutoNext(w!=0);player.setRepeatMode(w==2?Player.REPEAT_MODE_ONE:w==3?Player.REPEAT_MODE_ALL:Player.REPEAT_MODE_OFF);d.dismiss();updatePlayback();}).show();}
    private void volumeDialog(){AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);LinearLayout panel=vertical();panel.setPadding(dp(24),dp(12),dp(24),dp(12));panel.addView(text("Device volume",16,ink));SeekBar volume=new SeekBar(this);volume.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));volume.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));panel.addView(volume);volume.setOnSeekBarChangeListener(slider(p->am.setStreamVolume(AudioManager.STREAM_MUSIC,p,0)));TextView gain=text("Boost · "+service().boost+"%",16,ink);panel.addView(gain);SeekBar boost=new SeekBar(this);boost.setMax(100);boost.setProgress(service().boost-100);panel.addView(boost);boost.setOnSeekBarChangeListener(slider(p->{boolean ok=service().setBoost(p+100);gain.setText(ok?"Boost · "+(p+100)+"%":"Boost unavailable on this audio output");}));panel.addView(text("100% is original volume. Boost up to 200% may distort loud audio. Device support varies.",13,muted));new AlertDialog.Builder(this).setTitle("Volume & boost").setView(panel).setPositiveButton("Done",null).show();}
    private interface IntAction{void run(int n);}
    private SeekBar.OnSeekBarChangeListener slider(IntAction a){return new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)a.run(p);}};}
    private void queueDialog(){if(player==null)return;String[] names=new String[player.getMediaItemCount()];for(int i=0;i<names.length;i++)names[i]=(i==player.getCurrentMediaItemIndex()?"▶  ":(i+1)+".  ")+player.getMediaItemAt(i).mediaMetadata.title;new AlertDialog.Builder(this).setTitle("Up next · "+names.length+" items").setItems(names,(d,w)->{player.seekTo(w,0);player.play();}).setPositiveButton("Close",null).setNeutralButton("Browse library",(d,w)->leavePlayer()).show();}
    private void trackMenu(){new AlertDialog.Builder(this).setTitle("Audio & subtitles").setItems(new String[]{"Audio language / track","Subtitle track"},(d,w)->trackDialog(w==0?C.TRACK_TYPE_AUDIO:C.TRACK_TYPE_TEXT)).show();}
    private void trackDialog(int type){List<String> labels=new ArrayList<>();List<TrackSelectionOverride> overrides=new ArrayList<>();labels.add(type==C.TRACK_TYPE_TEXT?"Subtitles off":"Automatic");overrides.add(null);int selected=0;for(Tracks.Group g:player.getCurrentTracks().getGroups()){if(g.getType()!=type)continue;for(int i=0;i<g.length;i++){if(!g.isTrackSupported(i))continue;Format f=g.getTrackFormat(i);String language=f.language==null?"Unknown language":Locale.forLanguageTag(f.language).getDisplayLanguage();String name=(f.label!=null?f.label+" · ":"")+language+(f.channelCount>0?" · "+f.channelCount+" ch":"");labels.add(name);overrides.add(new TrackSelectionOverride(g.getMediaTrackGroup(),i));if(g.isTrackSelected(i))selected=labels.size()-1;}}
        if(labels.size()==1){toast(type==C.TRACK_TYPE_TEXT?"No supported embedded subtitles. Use More → Attach subtitle file.":"No selectable audio tracks available yet.");return;}
        new AlertDialog.Builder(this).setTitle(type==C.TRACK_TYPE_TEXT?"Subtitles":"Audio language").setSingleChoiceItems(labels.toArray(new String[0]),selected,(d,w)->{TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT&&w==0);if(w>0)b.setOverrideForType(overrides.get(w));player.setTrackSelectionParameters(b.build());d.dismiss();}).setNegativeButton("Close",null).show();
    }
    private void attachGestures(View area){AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);GestureDetector detector=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){float brightness;int volume;boolean changed;
        @Override public boolean onDown(android.view.MotionEvent e){brightness=getWindow().getAttributes().screenBrightness;if(brightness<0)try{brightness=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS)/255f;}catch(Exception x){brightness=.5f;}volume=am.getStreamVolume(AudioManager.STREAM_MUSIC);changed=false;return true;}
        @Override public boolean onSingleTapConfirmed(android.view.MotionEvent e){if(!locked&&currentVideo()){controlsVisible=!controlsVisible;applyControls();scheduleHide();}return true;}
        @Override public boolean onDoubleTap(android.view.MotionEvent e){if(!locked)seek(e.getX()<area.getWidth()/2f?-6000:6000);return true;}
        @Override public boolean onScroll(android.view.MotionEvent first,android.view.MotionEvent current,float dx,float dy){if(locked||first==null)return true;if(!currentVideo()&&Math.abs(current.getX()-first.getX())>Math.abs(current.getY()-first.getY()))return true;float delta=(first.getY()-current.getY())/Math.max(1,area.getHeight());if(!changed&&Math.abs(first.getY()-current.getY())<dp(18))return true;changed=true;if(first.getX()<area.getWidth()/2f){float next=Math.max(.02f,Math.min(1,brightness+delta));WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=next;getWindow().setAttributes(p);showFeedback("Brightness "+Math.round(next*100)+"%");}else{int max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);int next=Math.max(0,Math.min(max,volume+Math.round(delta*max)));am.setStreamVolume(AudioManager.STREAM_MUSIC,next,0);showFeedback("Volume "+Math.round(next*100f/max)+"%");}return true;}
    });float[] origin=new float[2];area.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN){origin[0]=e.getX();origin[1]=e.getY();}boolean handled=detector.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP&&!locked&&!currentVideo()){float dx=e.getX()-origin[0],dy=e.getY()-origin[1];if(Math.abs(dx)>dp(60)&&Math.abs(dx)>Math.abs(dy)*1.5f){if(dx<0)next();else previous();return true;}}return handled;});}
    private void applyControls(){if(transport!=null)transport.setVisibility(controlsVisible&&!locked?View.VISIBLE:View.GONE);if(controls!=null)controls.setVisibility(controlsVisible&&!locked?View.VISIBLE:View.GONE);if(playerHeader!=null)playerHeader.setVisibility(controlsVisible&&!locked?View.VISIBLE:View.GONE);lockButton.setVisibility(locked||controlsVisible?View.VISIBLE:View.GONE);}
    private final Runnable hideFeedback=()->{if(feedback!=null)feedback.setVisibility(View.GONE);};
    private void showFeedback(String message){if(feedback==null||!full)return;feedback.setText(message);feedback.setVisibility(View.VISIBLE);handler.removeCallbacks(hideFeedback);handler.postDelayed(hideFeedback,900);}
    private void leavePlayer(){if(locked){toast("Unlock the player first");return;}setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=-1;getWindow().setAttributes(p);buildLibrary();}
    @Override public void onBackPressed(){if(full){leavePlayer();return;}if(folder!=null||collection!=null){folder=null;collection=null;renderRows();return;}super.onBackPressed();}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);colors();if(full){boolean wasLocked=locked;renderPlayer();if(wasLocked){locked=true;controlsVisible=false;lockButton.setText("Unlock");applyControls();}}else buildLibrary();}
    @Override protected void onStop(){super.onStop();if(player!=null&&service()!=null&&!service().background&&!isChangingConfigurations())player.pause();}
    @Override protected void onDestroy(){if(artwork!=null)artwork.close();handler.removeCallbacksAndMessages(null);scanner.shutdownNow();thumbnails.shutdownNow();videoViewDetach();if(player!=null)player.removeListener(listener);if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture);super.onDestroy();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
        if(request==23&&pendingArtworkId!=null){String target=pendingArtworkId;pendingArtworkId=null;artwork.save(target,uri,ok->{if(isDestroyed())return;toast(ok?"Album image saved":"Could not read this image");if(full&&!currentVideo()&&player.getCurrentMediaItem().mediaId.equals(target))renderPlayer();else if(!full)renderRows();});}
        if(request==21){library.addTree(uri.toString());scan();}
        if(request==22&&player!=null&&player.getCurrentMediaItem()!=null){String filename="";try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())filename=c.getString(0).toLowerCase(Locale.ROOT);}String mime=filename.endsWith(".srt")?MimeTypes.APPLICATION_SUBRIP:filename.endsWith(".ass")||filename.endsWith(".ssa")?MimeTypes.TEXT_SSA:filename.endsWith(".ttml")||filename.endsWith(".xml")?MimeTypes.APPLICATION_TTML:MimeTypes.TEXT_VTT;MediaItem old=player.getCurrentMediaItem();long pos=player.getCurrentPosition();boolean playing=player.getPlayWhenReady();MediaItem.SubtitleConfiguration sub=new MediaItem.SubtitleConfiguration.Builder(uri).setMimeType(mime).setLabel(filename.isEmpty()?"External subtitles":filename).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();player.replaceMediaItem(player.getCurrentMediaItemIndex(),old.buildUpon().setSubtitleConfigurations(Collections.singletonList(sub)).build());player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).build());player.seekTo(pos);player.prepare();player.setPlayWhenReady(playing);toast("Subtitle file attached");}
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("artworkTarget",pendingArtworkId);}
    private String format(long ms){if(ms<0)return "0:00";long s=ms/1000;return s>=3600?String.format(Locale.US,"%d:%02d:%02d",s/3600,(s/60)%60,s%60):String.format(Locale.US,"%d:%02d",s/60,s%60);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private View iconButton(String kind,int color,Runnable action){FrameLayout f=new FrameLayout(this);f.setContentDescription(kind);f.setFocusable(true);f.setBackgroundColor(Color.TRANSPARENT);f.addView(new Symbol(this,kind,color),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));f.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));f.setOnClickListener(v->action.run());return f;}
    private static final class Symbol extends View {
        String kind;final int color;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Symbol(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas real){super.onDraw(real);real.save();real.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);Path path=new Path();switch(kind){
            case "folder":path.moveTo(3,6);path.lineTo(9,6);path.lineTo(11,8);path.lineTo(21,8);path.lineTo(21,19);path.lineTo(3,19);path.close();real.drawPath(path,p);break;
            case "music":real.drawLine(9,17,9,5,p);real.drawLine(9,5,20,3,p);real.drawLine(20,3,20,15,p);real.drawOval(3,15,9,20,p);real.drawOval(14,13,20,18,p);break;
            case "video":real.drawRoundRect(2,5,22,19,3,3,p);path.moveTo(10,9);path.lineTo(16,12);path.lineTo(10,15);path.close();real.drawPath(path,p);break;
            case "heart":path.moveTo(12,21);path.cubicTo(-5,10,5,-1,12,7);path.cubicTo(19,-1,29,10,12,21);real.drawPath(path,p);break;
            case "play":p.setStyle(Paint.Style.FILL);path.moveTo(7,4);path.lineTo(20,12);path.lineTo(7,20);path.close();real.drawPath(path,p);break;
            case "pause":p.setStrokeWidth(4);real.drawLine(8,5,8,19,p);real.drawLine(16,5,16,19,p);break;
            case "previous":real.rotate(180,12,12);
            case "skip":p.setStyle(Paint.Style.FILL);path.moveTo(5,5);path.lineTo(16,12);path.lineTo(5,19);path.close();real.drawPath(path,p);real.drawRect(18,5,20,19,p);break;
            case "rewind":case "forward":boolean forward=kind.equals("forward");real.save();if(forward)real.scale(-1,1,12,12);real.drawArc(3,3,21,21,-40,280,false,p);real.drawLine(3,4,3,10,p);real.drawLine(3,10,9,10,p);real.restore();p.setStyle(Paint.Style.FILL);p.setTextSize(10);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);real.drawText("6",12,16,p);break;
            case "fullscreen":real.drawLine(3,9,3,3,p);real.drawLine(3,3,9,3,p);real.drawLine(15,3,21,3,p);real.drawLine(21,3,21,9,p);real.drawLine(3,15,3,21,p);real.drawLine(3,21,9,21,p);real.drawLine(15,21,21,21,p);real.drawLine(21,21,21,15,p);break;
            case "captions":real.drawRoundRect(2,5,22,19,3,3,p);p.setStyle(Paint.Style.FILL);p.setTextSize(9);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);real.drawText("CC",12,15,p);break;
            case "queue":real.drawLine(3,5,21,5,p);real.drawLine(3,11,21,11,p);real.drawLine(3,17,13,17,p);path.moveTo(18,15);path.lineTo(22,18);path.lineTo(18,21);path.close();real.drawPath(path,p);break;
            case "next":real.drawLine(8,6,15,12,p);real.drawLine(15,12,8,18,p);break;
            case "back":real.drawLine(15,5,8,12,p);real.drawLine(8,12,15,19,p);break;
            case "more":p.setStyle(Paint.Style.FILL);for(int y=5;y<21;y+=7)real.drawCircle(12,y,1.6f,p);break;
            case "rotate":real.drawRoundRect(5,2,19,22,2,2,p);real.drawLine(2,7,2,17,p);real.drawLine(22,7,22,17,p);break;
            default:real.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;real.drawLine((float)(12+7*Math.cos(a)),(float)(12+7*Math.sin(a)),(float)(12+10*Math.cos(a)),(float)(12+10*Math.sin(a)),p);}
        }real.restore();}
    }
}
