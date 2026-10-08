package com.radha.music;

import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.downloader.*;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Public, unauthenticated extraction only. No Google credentials or downloaded media. */
public final class OnlineClient {
    public record Track(String url,String name,String artist,String image,long seconds) {}
    public record Results(List<Track> tracks,SearchQueryHandler query,Page next) {}
    public record Streams(String audio,String video,String muxed,long created) {}
    private static boolean initialized;
    private static final Map<String,Streams> cache=new LinkedHashMap<>();
    public static synchronized void init(){if(!initialized){NewPipe.init(new Network(),new Localization("en","IN"));initialized=true;}}
    public static Results search(String text,boolean video) throws Exception {
        init();StreamingService service=ServiceList.YouTube;
        SearchQueryHandler query=service.getSearchQHFactory().fromQuery(text,Collections.singletonList(video?"videos":"music_songs"),"");
        SearchInfo info=SearchInfo.getInfo(service,query);
        if(info.getRelatedItems().isEmpty()&&!info.getErrors().isEmpty())throw new IOException("Search is temporarily unavailable",info.getErrors().get(0));
        return new Results(tracks(info.getRelatedItems()),query,info.getNextPage());
    }
    public static Results more(Results prior) throws Exception {
        init();ListExtractor.InfoItemsPage<InfoItem> page=SearchInfo.getMoreItems(ServiceList.YouTube,prior.query(),prior.next());
        return new Results(tracks(page.getItems()),prior.query(),page.getNextPage());
    }
    public static boolean hasMore(Results result){return result!=null&&Page.isValid(result.next());}
    public static List<Track> tracks(List<? extends InfoItem> items){
        List<Track> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(InfoItem item:items)if(item instanceof StreamInfoItem s&&s.getStreamType()!=StreamType.LIVE_STREAM&&s.getStreamType()!=StreamType.POST_LIVE_STREAM&&OnlineRules.videoId(s.getUrl())!=null&&ids.add(s.getUrl())){
            String image=s.getThumbnails().isEmpty()?"":s.getThumbnails().get(s.getThumbnails().size()-1).getUrl();
            result.add(new Track(s.getUrl(),s.getName(),s.getUploaderName(),image,Math.max(0,s.getDuration())));
        }
        return result;
    }
    public static synchronized Streams resolve(String id) throws IOException {
        init();String key=OnlineRules.videoId(id);if(key==null)throw new IOException("Invalid online media ID");
        Streams saved=cache.get(key);if(saved!=null&&System.currentTimeMillis()-saved.created()<10*60*1000)return saved;
        try {
            StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,"https://www.youtube.com/watch?v="+key);
            AudioStream audio=info.getAudioStreams().stream().filter(s->s.isUrl()&&s.getDeliveryMethod()==DeliveryMethod.PROGRESSIVE_HTTP)
                .max(Comparator.comparingInt(AudioStream::getAverageBitrate)).orElse(null);
            VideoStream video=bestVideo(info.getVideoOnlyStreams()),muxed=bestVideo(info.getVideoStreams());
            if(audio==null&&muxed==null)throw new IOException("No playable stream is currently available");
            Streams result=new Streams(audio==null?null:audio.getContent(),video==null?null:video.getContent(),muxed==null?null:muxed.getContent(),System.currentTimeMillis());
            if(cache.size()>=24)cache.remove(cache.keySet().iterator().next());cache.put(key,result);return result;
        }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new IOException("YouTube could not provide this stream. Retry later or open it in YouTube.",e);}
    }
    public static synchronized void invalidate(String id){cache.remove(OnlineRules.videoId(id));}
    private static int resolution(VideoStream s){try{return Integer.parseInt(s.getResolution().replaceAll("[^0-9].*",""));}catch(Exception e){return 0;}}
    private static VideoStream bestVideo(List<VideoStream> streams){return streams.stream().filter(s->s.isUrl()&&s.getDeliveryMethod()==DeliveryMethod.PROGRESSIVE_HTTP&&resolution(s)<=1080)
        .max(Comparator.comparingInt(OnlineClient::resolution)).orElse(null);}
    public static class Network extends Downloader {
        @Override public Response execute(Request request) throws IOException {
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
            HttpURLConnection connection=(HttpURLConnection)new URL(request.url()).openConnection();
            connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setRequestMethod(request.httpMethod());
            connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept-Encoding","identity");
            request.headers().forEach((k,values)->{if(!k.equalsIgnoreCase("Accept-Encoding"))connection.setRequestProperty(k,String.join(", ",values));});
            try {
                byte[] body=request.dataToSend();if(body!=null){connection.setDoOutput(true);try(OutputStream out=connection.getOutputStream()){out.write(body);}}
                int code=connection.getResponseCode();InputStream input=code>=400?connection.getErrorStream():connection.getInputStream();String text="";
                if(input!=null)try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] chunk=new byte[8192];int n;while((n=in.read(chunk))!=-1){if(out.size()+n>16*1024*1024)throw new IOException("Response too large");out.write(chunk,0,n);}text=out.toString(StandardCharsets.UTF_8.name());}
                return new Response(code,connection.getResponseMessage(),connection.getHeaderFields(),text,connection.getURL().toString());
            }finally{connection.disconnect();}
        }
    }
}
