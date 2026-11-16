package org.limeime.clipboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlSanitizerTest {

    @Test
    public void stripsYouTubeSiParameter() {
        String in = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=abc123";
        String out = UrlSanitizer.sanitize(in);
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", out);
    }

    @Test
    public void stripsXTrackingParameters() {
        String in = "https://x.com/user/status/1?s=20&t=track";
        String out = UrlSanitizer.sanitize(in);
        assertFalse(out.contains("s=20"));
        assertFalse(out.contains("t=track"));
        assertTrue(out.contains("/status/1"));
    }

    @Test
    public void stripsGenericUtm() {
        String in = "https://example.com/path?utm_source=x&keep=yes";
        String out = UrlSanitizer.sanitize(in);
        assertEquals("https://example.com/path?keep=yes", out);
    }

    @Test
    public void nonUrlPassthrough() {
        assertEquals("hello", UrlSanitizer.sanitize("hello"));
    }
}