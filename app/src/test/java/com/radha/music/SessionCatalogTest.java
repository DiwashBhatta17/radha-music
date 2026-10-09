package com.radha.music;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class SessionCatalogTest {
 @Test public void completedPageIsImmediatelyAvailableWithoutAnotherFetch()throws Exception{
  AtomicInteger calls=new AtomicInteger();SessionCatalog cache=new SessionCatalog(new SessionCatalog.Backend(){public OnlineClient.Results search(String q,boolean w){calls.incrementAndGet();return result();}public OnlineClient.Results more(OnlineClient.Results p){throw new AssertionError();}});
  assertNull(cache.peek("artist",false));OnlineClient.Results loaded=cache.search("artist",false);assertSame(loaded,cache.peek("artist",false));assertSame(loaded,cache.search("artist",false));assertEquals(1,calls.get());
 }
 private OnlineClient.Results result(){return new OnlineClient.Results(Collections.emptyList(),null,null,false);}
 @Test public void revisitingAndConcurrentWaitersShareOneFetch()throws Exception{
  SessionCatalog cache=new SessionCatalog();AtomicInteger calls=new AtomicInteger();CountDownLatch release=new CountDownLatch(1);
  Callable<OnlineClient.Results> fetch=()->{calls.incrementAndGet();release.await();return result();};
  FutureTask<OnlineClient.Results> first=cache.task("music",fetch),second=cache.task("music",fetch);assertSame(first,second);
  release.countDown();first.get(3,TimeUnit.SECONDS);assertSame(first,cache.task("music",fetch));assertEquals(1,calls.get());
 }
 @Test public void leavingPageDoesNotCancelSharedRequest()throws Exception{
  SessionCatalog cache=new SessionCatalog();CountDownLatch release=new CountDownLatch(1),waiting=new CountDownLatch(1);
  FutureTask<OnlineClient.Results> shared=cache.task("watch",()->{release.await();return result();});
  Thread page=new Thread(()->{waiting.countDown();try{SessionCatalog.await(shared);}catch(Exception expected){}});page.start();waiting.await();page.interrupt();page.join(1000);
  assertFalse(shared.isCancelled());release.countDown();assertNotNull(shared.get(3,TimeUnit.SECONDS));
 }
 @Test public void newSessionFetchesAgain()throws Exception{
  SessionCatalog cache=new SessionCatalog();AtomicInteger calls=new AtomicInteger();Callable<OnlineClient.Results> fetch=()->{calls.incrementAndGet();return result();};
  cache.task("home",fetch).get(3,TimeUnit.SECONDS);cache.reset();cache.task("home",fetch).get(3,TimeUnit.SECONDS);assertEquals(2,calls.get());
 }
 @Test public void failedRequestCanRetry()throws Exception{
  SessionCatalog cache=new SessionCatalog();FutureTask<OnlineClient.Results> failed=cache.task("home",()->{throw new java.io.IOException("offline");});try{failed.get(3,TimeUnit.SECONDS);fail();}catch(ExecutionException expected){}
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);FutureTask<OnlineClient.Results> retry;
  do{retry=cache.task("home",this::result);Thread.yield();}while(retry==failed&&System.nanoTime()<end);
  assertNotSame(failed,retry);assertNotNull(retry.get(3,TimeUnit.SECONDS));
 }
}
