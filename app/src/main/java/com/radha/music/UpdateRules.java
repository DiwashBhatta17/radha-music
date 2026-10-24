package com.radha.music;
import org.json.*;
final class UpdateRules {
 static final String REPO="DiwashBhatta17/radha-music";
 record Release(String version,String url,String sha256,long size){}
 static boolean newer(String candidate,String current){try{int[] a=parts(candidate),b=parts(current);for(int i=0;i<3;i++){if(a[i]!=b[i])return a[i]>b[i];}}catch(Exception ignored){}return false;}
 private static int[] parts(String version){if(!version.matches("v?\\d+\\.\\d+\\.\\d+"))throw new IllegalArgumentException();String[] parts=version.replaceFirst("^v", "").split("\\.");return new int[]{Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2])};}
 static Release parse(String body,String current)throws JSONException{
  JSONObject json=new JSONObject(body);String version=json.optString("tag_name");if(json.optBoolean("draft")||json.optBoolean("prerelease")||!newer(version,current))return null;
  JSONArray assets=json.optJSONArray("assets");if(assets==null)return null;
  for(int i=0;i<assets.length();i++){JSONObject asset=assets.getJSONObject(i);String url=asset.optString("browser_download_url"),digest=asset.optString("digest");long size=asset.optLong("size");if(asset.optString("name").equals("Radha-Music.apk")&&url.startsWith("https://github.com/"+REPO+"/releases/download/")&&digest.matches("sha256:[0-9a-fA-F]{64}")&&size>0&&size<250L*1024*1024)return new Release(version,url,digest.substring(7),size);}
  return null;
 }
}
