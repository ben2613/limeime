package org.limeime.clipboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Strips common tracking query parameters from URLs (YouTube, X/Twitter, generic).
 */
public final class UrlSanitizer {

    private static final Set<String> GENERIC_TRACKING = new HashSet<>(Arrays.asList(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
            "fbclid", "gclid", "gclsrc", "mc_cid", "mc_eid", "igshid", "mkt_tok",
            "si", "feature", "ref_src", "ref_url", "ref", "s", "t"));

    private UrlSanitizer() {
    }

    /**
     * Returns a cleaner URL when the input looks like http(s); otherwise returns input unchanged.
     */
    @NonNull
    public static String sanitize(@NonNull String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        URI uri = parseHttpUri(trimmed);
        if (uri == null || uri.getHost() == null) {
            return trimmed;
        }
        String host = uri.getHost().toLowerCase(Locale.US);
        Set<String> stripNames = paramsToStrip(host);
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return trimmed;
        }
        List<String> kept = new ArrayList<>();
        for (String part : rawQuery.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String name = (eq >= 0 ? part.substring(0, eq) : part).toLowerCase(Locale.US);
            if (stripNames.contains(name)) {
                continue;
            }
            kept.add(part);
        }
        if (kept.isEmpty()) {
            return withoutQuery(trimmed, uri);
        }
        try {
            URI rebuilt = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    String.join("&", kept),
                    uri.getFragment());
            return rebuilt.toString();
        } catch (URISyntaxException e) {
            return trimmed;
        }
    }

    @NonNull
    private static String withoutQuery(@NonNull String fallback, @NonNull URI uri) {
        try {
            URI rebuilt = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment());
            String out = rebuilt.toString();
            return out.isEmpty() ? fallback : out;
        } catch (URISyntaxException e) {
            return fallback;
        }
    }

    @Nullable
    private static URI parseHttpUri(@NonNull String trimmed) {
        try {
            URI uri = new URI(trimmed);
            if (uri.getScheme() == null) {
                if (trimmed.regionMatches(true, 0, "www.", 0, 4)) {
                    uri = new URI("https://" + trimmed);
                } else {
                    return null;
                }
            }
            String scheme = uri.getScheme();
            if (scheme == null) {
                return null;
            }
            String lower = scheme.toLowerCase(Locale.US);
            if (!"http".equals(lower) && !"https".equals(lower)) {
                return null;
            }
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @NonNull
    private static Set<String> paramsToStrip(@NonNull String host) {
        Set<String> names = new HashSet<>(GENERIC_TRACKING);
        if (isYouTubeHost(host)) {
            names.add("pp");
            names.add("embed");
        }
        if (isXHost(host)) {
            names.add("twclid");
        }
        return names;
    }

    private static boolean isYouTubeHost(@NonNull String host) {
        return host.equals("youtu.be")
                || host.endsWith(".youtu.be")
                || host.equals("youtube.com")
                || host.endsWith(".youtube.com")
                || host.equals("m.youtube.com")
                || host.equals("music.youtube.com");
    }

    private static boolean isXHost(@NonNull String host) {
        return host.equals("x.com")
                || host.endsWith(".x.com")
                || host.equals("twitter.com")
                || host.endsWith(".twitter.com")
                || host.equals("mobile.twitter.com");
    }
}