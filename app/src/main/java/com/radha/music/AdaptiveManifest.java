package com.radha.music;
import java.util.*;
import org.schabi.newpipe.extractor.stream.*;

/** A memory-only DASH manifest over the provider's indexed original streams. */
final class AdaptiveManifest {
 record Variant(String id,String url,String mime,String codec,int width,int height,int bitrate,int initStart,int initEnd,int indexStart,int indexEnd){}
 static String from(List<VideoStream> streams,long seconds){
  List<Variant> variants=new ArrayList<>();
  for(VideoStream s:streams)if(s.isUrl()&&s.getDeliveryMethod()==DeliveryMethod.PROGRESSIVE_HTTP&&s.getIndexEnd()>s.getIndexStart()&&s.getInitEnd()>s.getInitStart()&&Math.min(s.getWidth(),s.getHeight())<=1080&&s.getWidth()>0&&s.getHeight()>0&&s.getFormat()!=null){variants.add(new Variant(String.valueOf(s.getItag()),s.getContent(),s.getFormat().getMimeType(),s.getCodec(),s.getWidth(),s.getHeight(),Math.max(1,s.getBitrate()),s.getInitStart(),s.getInitEnd(),s.getIndexStart(),s.getIndexEnd()));}
  // AVC has the widest hardware support, and avoids cross-codec switching stalls.
  boolean avc=variants.stream().anyMatch(v->v.codec()!=null&&v.codec().startsWith("avc"));
  if(avc)variants.removeIf(v->v.codec()==null||!v.codec().startsWith("avc"));
  return build(variants,seconds);
 }
 static String build(List<Variant> variants,long seconds){
  if(variants.isEmpty()||seconds<=0)return null;
  StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" type=\"static\" profiles=\"urn:mpeg:dash:profile:isoff-on-demand:2011\" minBufferTime=\"PT1.5S\" mediaPresentationDuration=\"PT"+seconds+"S\"><Period start=\"PT0S\">");
  Map<String,List<Variant>> groups=new LinkedHashMap<>();for(Variant v:variants)groups.computeIfAbsent(v.mime()+":"+(v.codec()==null?"":v.codec().split("\\.")[0]),k->new ArrayList<>()).add(v);
  int id=0;for(List<Variant> group:groups.values()){xml.append("<AdaptationSet id=\"").append(id++).append("\" contentType=\"video\" segmentAlignment=\"true\">");for(Variant v:group){xml.append("<Representation id=\"").append(escape(v.id())).append("\" mimeType=\"").append(escape(v.mime())).append("\" codecs=\"").append(escape(v.codec())).append("\" width=\"").append(v.width()).append("\" height=\"").append(v.height()).append("\" bandwidth=\"").append(v.bitrate()).append("\"><BaseURL>").append(escape(v.url())).append("</BaseURL><SegmentBase indexRange=\"").append(v.indexStart()).append('-').append(v.indexEnd()).append("\"><Initialization range=\"").append(v.initStart()).append('-').append(v.initEnd()).append("\"/></SegmentBase></Representation>");}xml.append("</AdaptationSet>");}
  return xml.append("</Period></MPD>").toString();
 }
 private static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");}
}
