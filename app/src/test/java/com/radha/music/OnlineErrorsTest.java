package com.radha.music;

import java.io.IOException;
import java.net.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OnlineErrorsTest {
    @Test public void nestedDnsFailureHasSpecificGuidance(){assertTrue(OnlineErrors.message(new IOException("Search failed",new UnknownHostException("music.youtube.com"))).contains("Private DNS"));}
    @Test public void compatibilityFailuresAreNotReportedAsBadWifi(){assertTrue(OnlineErrors.message(new NoClassDefFoundError("missing.Component")).contains("incompatible"));}
    @Test public void diagnosticsRedactUrlsAndCredentials(){String details=OnlineErrors.details(new IOException("Failed https://host.test/path?signature=secret token=secret cookie=private"));assertFalse(details.contains("secret"));assertFalse(details.contains("private"));assertTrue(details.contains("IOException"));}
    @Test public void timeoutMessageIsActionable(){assertTrue(OnlineErrors.message(new SocketTimeoutException()).contains("retry"));}
}
