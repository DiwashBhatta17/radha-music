package com.radha.music;
import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicit user downloads, persisted only once every required stream finishes. */
public final class DownloadService extends Service {
    private static volatile DownloadService instance;
    public static boolean isBusy(){DownloadService s=instance;return s!=null&&!s.pending.isEmpty();}
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Set<String> pending=ConcurrentHashMap.newKeySet();
    private final Handler main=new Handler(Looper.getMainLooper());private DownloadStore store;private volatile HttpURLConnection active;private volatile boolean closed;private static final String CHANNEL="radha_downloads";private int latestStart;
    @Override public void onCreate(){super.onCreate();instance=this;store=new DownloadStore(this);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Radha downloads",NotificationManager.IMPORTANCE_LOW));}
    private Notification notification(String label,int percent){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).putExtra("downloads",true),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent cancel=PendingIntent.getService(this,1,new Intent(this,DownloadService.class).setAction("cancel"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_luma).setContentTitle("Radha Music · Downloads").setContentText(label).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"Cancel downloads",cancel).build());b.setProgress(100,Math.max(0,percent),percent<0);return b.build();}
    public static void start(Context c,MediaEntry e){c.startForegroundService(new Intent(c,DownloadService.class).putExtra("entry",e.json().toString()));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){latestStart=startId;if(intent==null){stopSelf();return START_NOT_STICKY;}if("cancel".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}startForeground(82,notification("Preparing download",-1));
        try{MediaEntry entry=MediaEntry.from(new org.json.JSONObject(intent.getStringExtra("entry")));if(!entry.online())throw new IllegalArgumentException();if(store.files(entry.id)!=null){if(pending.isEmpty())stopSelf(startId);return START_NOT_STICKY;}if(!pending.add(entry.id))return START_NOT_STICKY;store.state(entry,"Queued");worker.submit(()->download(entry));}catch(Exception e){if(pending.isEmpty())stopSelf(startId);}return START_NOT_STICKY;}
    private void download(MediaEntry e){boolean audio=false,video=false;try{if(closed)throw new InterruptedIOException();store.state(e,"Downloading");OnlineClient.Streams streams=OnlineClient.resolve(e.id);
        if(e.video&&streams.video()!=null&&streams.audio()!=null){transfer(streams.audio(),store.file(e.id,"audio"),e.name+" · audio");audio=true;transfer(streams.video(),store.file(e.id,"video"),e.name+" · video");video=true;}
        else if(e.video){if(streams.muxed()==null)throw new IOException("No downloadable video stream");transfer(streams.muxed(),store.file(e.id,"video"),e.name);video=true;}
        else{String url=streams.audio()!=null?streams.audio():streams.muxed();if(url==null)throw new IOException("No downloadable audio stream");transfer(url,store.file(e.id,"audio"),e.name);audio=true;}
        if(closed)throw new InterruptedIOException();store.complete(e,audio,video);
    }catch(Exception error){for(String part:new String[]{"audio","video"}){File file=store.file(e.id,part);file.delete();new File(file.getPath()+".part").delete();}store.state(e,closed?"Cancelled · tap to retry":"Download failed · tap to retry");}
    finally{pending.remove(e.id);main.post(()->{if(pending.isEmpty())stopSelf(latestStart);});}}
    private void transfer(String url,File target,String label)throws IOException {File part=new File(target.getPath()+".part");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();active=c;c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Mozilla/5.0");try{int code=c.getResponseCode();if(code!=200&&code!=206)throw new IOException("Download response "+code);long total=c.getContentLengthLong(),read=0,last=0;if(total>0&&target.getParentFile().getUsableSpace()<total+32L*1024*1024)throw new IOException("Insufficient storage");try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(part)){byte[] buffer=new byte[64*1024];int n;while((n=in.read(buffer))!=-1){if(closed||Thread.currentThread().isInterrupted())throw new InterruptedIOException();out.write(buffer,0,n);read+=n;if(System.currentTimeMillis()-last>800){last=System.currentTimeMillis();if(target.getParentFile().getUsableSpace()<16L*1024*1024)throw new IOException("Insufficient storage");getSystemService(NotificationManager.class).notify(82,notification(label,total>0?(int)Math.min(99,read*100/total):-1));}}out.getFD().sync();}if(read==0||(total>=0&&read!=total))throw new IOException("Incomplete download");if(!part.renameTo(target))throw new IOException("Could not save download");}finally{c.disconnect();active=null;part.delete();}}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
    @Override public void onTimeout(int startId,int fgsType){stopSelf();}
    @Override public void onDestroy(){closed=true;instance=null;if(active!=null)active.disconnect();worker.shutdownNow();for(MediaEntry e:store.entries())if(pending.contains(e.id)&&!store.state(e.id).equals("Downloaded"))store.state(e,"Cancelled · tap to retry");main.removeCallbacksAndMessages(null);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
