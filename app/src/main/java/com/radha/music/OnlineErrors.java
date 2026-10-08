package com.radha.music;

import java.net.*;
import javax.net.ssl.SSLException;
import java.util.*;

/** Safe, bounded diagnostics: never expose signed media URLs, cookies or request bodies. */
public final class OnlineErrors {
    private OnlineErrors() {}
    public static String message(Throwable error){
        Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Throwable t=error;t!=null&&seen.add(t);t=t.getCause()){
            if(t instanceof UnknownHostException)return "Your phone could not find YouTube's server. Check Wi-Fi/mobile data and Private DNS.";
            if(t instanceof SocketTimeoutException)return "YouTube took too long to respond. Please retry or try another connection.";
            if(t instanceof SSLException)return "The secure connection failed. Check your phone's date/time and try another connection.";
            if(t instanceof LinkageError)return "An online component is incompatible with this Android version. Copy the error details so this can be fixed.";
            if(t instanceof SecurityException)return "Internet access was blocked on this device. Check this app's Wi-Fi/mobile-data settings.";
        }
        return "YouTube did not return usable results. Retry, or copy the error details to help diagnose it.";
    }
    public static String details(Throwable error){
        StringBuilder out=new StringBuilder();Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Throwable t=error;t!=null&&seen.add(t)&&seen.size()<=6;t=t.getCause()){
            if(out.length()>0)out.append("\nCaused by: ");out.append(t.getClass().getSimpleName());
            String message=t.getMessage();if(message!=null){message=message.replaceAll("https?://[^\\s]+","[URL]").replaceAll("(?i)(cookie|authorization|token|key|signature)\\s*[:=]\\s*[^\\s,;]+","$1=[redacted]");out.append(": ").append(message.substring(0,Math.min(220,message.length())));}
        }
        return out.toString();
    }
}
