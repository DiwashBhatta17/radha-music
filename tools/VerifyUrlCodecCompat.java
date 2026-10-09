import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Checks the packaged DEX, not desktop JVM classes, for the Android 11 regression.
 * Run: java tools/VerifyUrlCodecCompat.java path/to/app.apk
 */
class VerifyUrlCodecCompat {
    static final String SIGNATURE="(Ljava/lang/String;Ljava/nio/charset/Charset;)Ljava/lang/String;";
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Provide an APK path");
        Set<String> references=new TreeSet<>(), implemented=new TreeSet<>();
        try(ZipFile apk=new ZipFile(args[0])) {
            for(var entries=apk.entries();entries.hasMoreElements();) {
                ZipEntry entry=entries.nextElement();
                if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
                try(InputStream in=apk.getInputStream(entry)){new Dex(in.readAllBytes()).read(references,implemented);}
            }
        }
        for(String codec:new String[]{"URLEncoder","URLDecoder"}) {
            String name=codec.equals("URLEncoder")?"encode":"decode";
            String platform="Ljava/net/"+codec+";->"+name+SIGNATURE;
            String bundled="Lj$/net/"+codec+";->"+name+SIGNATURE;
            if(references.contains(platform))throw new AssertionError("Android 11 incompatible method reference: "+platform);
            if(!references.contains(bundled)||!implemented.contains(bundled))throw new AssertionError("Missing bundled compatibility implementation: "+bundled);
            System.out.println("PASS: platform Charset overload absent; bundled "+codec+" implementation present");
        }
    }
    static final class Dex {
        final byte[] data;final ByteBuffer buffer;String[] strings,types,protos,methods;
        Dex(byte[] data){this.data=data;buffer=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);}
        int i(int off){return buffer.getInt(off);} int s(int off){return Short.toUnsignedInt(buffer.getShort(off));}
        int uleb(int[] cursor){int value=0;for(int shift=0;shift<35;shift+=7){int b=data[cursor[0]++]&255;value|=(b&127)<<shift;if((b&128)==0)return value;}throw new IllegalArgumentException("Invalid DEX ULEB128");}
        void read(Set<String> refs,Set<String> implementations) {
            if(data.length<112||data[0]!='d'||data[1]!='e'||data[2]!='x')throw new IllegalArgumentException("Invalid DEX");
            strings=new String[i(0x38)];int off=i(0x3c);
            for(int n=0;n<strings.length;n++){int[] p={i(off+n*4)};uleb(p);int end=p[0];while(data[end]!=0)end++;strings[n]=new String(data,p[0],end-p[0],StandardCharsets.UTF_8);}
            types=new String[i(0x40)];off=i(0x44);for(int n=0;n<types.length;n++)types[n]=strings[i(off+n*4)];
            protos=new String[i(0x48)];off=i(0x4c);
            for(int n=0;n<protos.length;n++){int p=off+n*12;StringBuilder signature=new StringBuilder("(");int params=i(p+8);if(params!=0)for(int j=0;j<i(params);j++)signature.append(types[s(params+4+j*2)]);protos[n]=signature.append(')').append(types[i(p+4)]).toString();}
            methods=new String[i(0x58)];off=i(0x5c);
            for(int n=0;n<methods.length;n++){int p=off+n*8;methods[n]=types[s(p)]+"->"+strings[i(p+4)]+protos[s(p+2)];refs.add(methods[n]);}
            int classes=i(0x60);off=i(0x64);
            for(int n=0;n<classes;n++){int cdata=i(off+n*32+24);if(cdata==0)continue;int[] p={cdata};int sf=uleb(p),inf=uleb(p),direct=uleb(p),virtual=uleb(p);for(int j=0;j<sf+inf;j++){uleb(p);uleb(p);}readMethods(p,direct,implementations);readMethods(p,virtual,implementations);}
        }
        void readMethods(int[] p,int count,Set<String> implemented){int index=0;for(int j=0;j<count;j++){index+=uleb(p);uleb(p);int code=uleb(p);if(code!=0)implemented.add(methods[index]);}}
    }
}
