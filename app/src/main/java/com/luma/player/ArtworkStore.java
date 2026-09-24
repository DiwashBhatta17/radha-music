package com.radha.music;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Artwork is decoded off the UI thread; custom art is copied into private app storage. */
public final class ArtworkStore {
    public interface Result {void ready(Bitmap bitmap);}
    public interface Saved {void complete(boolean success);}
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(16*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getByteCount();}};
    private final Set<String> missing=Collections.synchronizedSet(new HashSet<>());
    private final Map<String,List<Result>> pending=new HashMap<>();
    public ArtworkStore(Context c){context=c.getApplicationContext();}
    private File file(String id){try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.US,"%02x",b));return new File(new File(context.getFilesDir(),"artwork"),s+".jpg");}catch(Exception e){throw new IllegalStateException(e);}}
    public boolean hasCustom(String id){return file(id).exists();}
    public void load(String id,Result result){Bitmap b=cache.get(id);if(b!=null){result.ready(b);return;}if(missing.contains(id)){result.ready(null);return;}synchronized(pending){if(pending.containsKey(id)){pending.get(id).add(result);return;}pending.put(id,new ArrayList<>(Collections.singletonList(result)));}
        worker.execute(()->{Bitmap image=null;try{File f=file(id);if(f.exists())image=BitmapFactory.decodeFile(f.toString());else{MediaMetadataRetriever r=new MediaMetadataRetriever();try{r.setDataSource(context,Uri.parse(id));byte[] bytes=r.getEmbeddedPicture();if(bytes!=null)image=decode(bytes);}finally{r.release();}}}catch(Exception ignored){}if(image!=null)cache.put(id,image);else missing.add(id);Bitmap loaded=image;main.post(()->{List<Result> callbacks;synchronized(pending){callbacks=pending.remove(id);}if(callbacks!=null)for(Result cb:callbacks)cb.ready(loaded);});});
    }
    private Bitmap decode(byte[] bytes){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>768)o.inSampleSize*=2;o.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);}
    public void save(String id,Uri uri,Saved callback){worker.execute(()->{boolean ok=false;File temp=null;try{BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=context.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>768)o.inSampleSize*=2;o.inJustDecodeBounds=false;Bitmap b;try(InputStream in=context.getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(in,null,o);}if(b!=null){File dest=file(id);dest.getParentFile().mkdirs();temp=new File(dest.getPath()+".tmp");try(OutputStream out=new FileOutputStream(temp)){ok=b.compress(Bitmap.CompressFormat.JPEG,92,out);}if(ok)ok=temp.renameTo(dest);if(ok){cache.put(id,b);missing.remove(id);}}}catch(Exception ignored){}finally{if(temp!=null&&temp.exists())temp.delete();}boolean done=ok;main.post(()->callback.complete(done));});}
    public void remove(String id){file(id).delete();cache.remove(id);missing.remove(id);}
    public void close(){worker.shutdownNow();synchronized(pending){pending.clear();}}
}
