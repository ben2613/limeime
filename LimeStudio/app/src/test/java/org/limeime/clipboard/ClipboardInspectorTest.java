package org.limeime.clipboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClipboardInspectorTest {

    @Test
    public void looksLikeUrlRecognizesHttps() {
        assertTrue(ClipboardInspector.looksLikeUrl("https://youtu.be/abc"));
    }

    @Test
    public void ellipsizeTruncates() {
        String longText = "abcdefghijklmnopqrstuvwxyz0123456789";
        assertEquals(20, ClipboardInspector.ellipsize(longText).length());
        assertTrue(ClipboardInspector.ellipsize(longText).endsWith("…"));
    }

    @Test
    public void shortTextUnchanged() {
        assertEquals("hi", ClipboardInspector.ellipsize("hi"));
    }
}