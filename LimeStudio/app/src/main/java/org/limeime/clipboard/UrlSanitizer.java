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
 * Strips common tracking query parameters from URLs.
 *
 * <p>Host-specific lists avoid stripping {@code t=} on YouTube (playback offset) while still
 * removing X/Twitter share tokens ({@code a=}, {@code t=}, {@code s=} since late 2026 {@code a}
 * often replaces {@code t=} on share URLs).
 */
public final class UrlSanitizer {

    /** Analytics / ads click IDs — safe on any host. */
    private static final Set<String> GLOBAL_STRIP = new HashSet<>(Arrays.asList(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
            "fbclid", "gclid", "gclsrc", "mc_cid", "mc_eid", "igshid", "mkt_tok",
            "feature", "ref_url", "ref"));

    /** X / Twitter share & attribution (do not add {@code t}/{@code s} globally). */
    private static final Set<String> X_STRIP = new HashSet<>(Arrays.asList(
            "a", "s", "t", "twclid", "ref_src", "refsrc", "cn", "src", "iid", "ntref", "ft",
            "lang", "vertical", "f", "p", "rd", "rnd", "cxt", "bp"));

    private static final Set<String> YOUTUBE_STRIP = new HashSet<>(Arrays.asList(
            "si", "pp", "embed"));

    private static final Set<String> FACEBOOK_STRIP = new HashSet<>(Arrays.asList(
            "mibextid", "__cft__", "__tn__", "eid", "hc_ref", "refid"));

    private static final Set<String> INSTAGRAM_STRIP = new HashSet<>(Arrays.asList(
            "igsh", "igshid"));

    private static final Set<String> TIKTOK_STRIP = new HashSet<>(Arrays.asList(
            "is_from_webapp", "is_copy_url", "share_id", "sender_device", "sender_web_id",
            "_r", "_t", "_d"));

    private static final Set<String> LINKEDIN_STRIP = new HashSet<>(Arrays.asList(
            "trk", "trkinfo", "li_fat_id", "lipi", "originalsubdomain"));

    private static final Set<String> REDDIT_STRIP = new HashSet<>(Arrays.asList(
            "share_id", "rdt"));

    private UrlSanitizer() {
    }

    /**
     * Sanitized x.com / twitter.com status URL with host {@code fixupx.com}, or {@code null} if not applicable.
     */
    @Nullable
    public static String toFixupXContentUrl(@NonNull String input) {
        String cleaned = sanitize(input);
        URI uri = parseHttpUri(cleaned);
        if (uri == null || uri.getHost() == null) {
            return null;
        }
        String host = uri.getHost().toLowerCase(Locale.US);
        if (!isXContentHost(host)) {
            return null;
        }
        try {
            URI rebuilt = new URI(
                    uri.getScheme(),
                    "fixupx.com",
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());
            return rebuilt.toString();
        } catch (URISyntaxException e) {
            return null;
        }
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
            if (shouldStrip(name, stripNames)) {
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

    /** Also strips keys prefixed with {@code utm_} or {@code __} (session blobs). */
    static boolean shouldStrip(@NonNull String name, @NonNull Set<String> stripNames) {
        if (stripNames.contains(name)) {
            return true;
        }
        if (name.startsWith("utm_")) {
            return true;
        }
        return name.startsWith("__");
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
    static Set<String> paramsToStrip(@NonNull String host) {
        Set<String> names = new HashSet<>(GLOBAL_STRIP);
        if (isYouTubeHost(host)) {
            names.addAll(YOUTUBE_STRIP);
        }
        if (isXHost(host)) {
            names.addAll(X_STRIP);
        }
        if (isFacebookHost(host)) {
            names.addAll(FACEBOOK_STRIP);
        }
        if (isInstagramHost(host)) {
            names.addAll(INSTAGRAM_STRIP);
        }
        if (isTikTokHost(host)) {
            names.addAll(TIKTOK_STRIP);
        }
        if (isLinkedInHost(host)) {
            names.addAll(LINKEDIN_STRIP);
        }
        if (isRedditHost(host)) {
            names.addAll(REDDIT_STRIP);
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
        return isXContentHost(host) || host.equals("t.co");
    }

    private static boolean isXContentHost(@NonNull String host) {
        return host.equals("x.com")
                || host.endsWith(".x.com")
                || host.equals("twitter.com")
                || host.endsWith(".twitter.com")
                || host.equals("mobile.twitter.com");
    }

    private static boolean isFacebookHost(@NonNull String host) {
        return host.equals("facebook.com")
                || host.endsWith(".facebook.com")
                || host.equals("fb.com")
                || host.endsWith(".fb.com")
                || host.equals("fb.watch");
    }

    private static boolean isInstagramHost(@NonNull String host) {
        return host.equals("instagram.com") || host.endsWith(".instagram.com");
    }

    private static boolean isTikTokHost(@NonNull String host) {
        return host.equals("tiktok.com")
                || host.endsWith(".tiktok.com")
                || host.equals("vm.tiktok.com");
    }

    private static boolean isLinkedInHost(@NonNull String host) {
        return host.equals("linkedin.com") || host.endsWith(".linkedin.com");
    }

    private static boolean isRedditHost(@NonNull String host) {
        return host.equals("reddit.com")
                || host.endsWith(".reddit.com")
                || host.equals("old.reddit.com")
                || host.equals("www.reddit.com");
    }
}