package com.radha.music;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.Files;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @LooperMode(LooperMode.Mode.PAUSED)
public class DownloadTransferAndroidTest {
 @Test public void transferCommitsCompleteBytesAndRemovesIncompleteParts()throws Exception {
  byte[] payload=new byte[8192];for(int i=0;i<payload.length;i++)payload[i]=(byte)i;
  ServerSocket server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));java.util.concurrent.ExecutorService network=java.util.concurrent.Executors.newSingleThreadExecutor();
  network.submit(()->{while(!server.isClosed())try(Socket socket=server.accept()){BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));String request=reader.readLine(),line;while((line=reader.readLine())!=null&&!line.isEmpty()){}boolean partial=request.contains("/partial");OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Length: "+(partial?payload.length*2:payload.length)+"\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(payload);out.flush();}catch(IOException ignored){}});
  var controller=Robolectric.buildService(DownloadService.class).create();
  try {
   DownloadService service=controller.get();DownloadStore store=new DownloadStore(service);String id="radha://youtube/abcdefghijk/audio";File target=store.file(id,"audio");Method transfer=DownloadService.class.getDeclaredMethod("transfer",String.class,File.class,String.class);transfer.setAccessible(true);String base="http://127.0.0.1:"+server.getLocalPort();
   transfer.invoke(service,base+"/ok",target,"Test");assertArrayEquals(payload,Files.readAllBytes(target.toPath()));assertFalse(new File(target+".part").exists());target.delete();
   try{transfer.invoke(service,base+"/partial",target,"Test");fail("Incomplete transfer accepted");}catch(InvocationTargetException expected){assertTrue(expected.getCause() instanceof IOException);}assertFalse(target.exists());assertFalse(new File(target+".part").exists());
  }finally{controller.destroy();server.close();network.shutdownNow();}
 }
}
