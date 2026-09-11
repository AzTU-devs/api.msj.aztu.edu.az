package az.edu.aztu.msj.common;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

/**
 * Scrubs plain-text fields — titles, names, keywords, affiliations — that must
 * never carry markup.
 *
 * <p>Tags are <em>removed</em> rather than escaped, so the stored value is inert
 * no matter which sink later renders it (JSX, an {@code innerHTML} assignment in
 * the editor, a PDF header, an e-mail body). Escaping alone would leave the
 * payload intact and merely defer the problem to the first renderer that
 * un-escapes it.
 *
 * <p>Cleaning runs to a fixed point because a single pass is defeated by
 * double-encoding: {@code &lt;script&gt;} survives pass one as literal text and
 * only becomes a tag once decoded. Two more passes take that decoded form and
 * strip it. Legitimate mathematical text ("a < b") reaches a stable state and is
 * preserved.
 */
public final class TextSanitizer {

    public static final int SHORT_MAX = 255;
    public static final int TITLE_MAX = 500;
    public static final int LONG_MAX = 20_000;

    private static final int MAX_PASSES = 3;

    /** Strips markup and control characters, collapses whitespace, truncates. */
    public static String text(String raw, int maxLength) {
        if (raw == null) return null;

        String current = raw;
        for (int i = 0; i < MAX_PASSES; i++) {
            String next = Parser.unescapeEntities(Jsoup.clean(current, Safelist.none()), false);
            if (next.equals(current)) break;
            current = next;
        }

        // Control and format characters (NUL, bidi overrides, zero-width joiners)
        // are spacing, not content — they hide payloads and spoof names.
        String cleaned = current.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** Plain text capped at {@link #SHORT_MAX} — names, countries, ORCIDs. */
    public static String shortText(String raw) {
        return text(raw, SHORT_MAX);
    }

    /** Plain text capped at {@link #TITLE_MAX} — article and section titles. */
    public static String title(String raw) {
        return text(raw, TITLE_MAX);
    }

    /** Plain text capped at {@link #LONG_MAX} — abstracts and other prose. */
    public static String longText(String raw) {
        return text(raw, LONG_MAX);
    }

    /**
     * An e-mail address, or null if it cannot be one. Kept separate from
     * {@link #shortText} so a scrubbed-but-malformed address is rejected outright
     * instead of being stored and later mailed to.
     */
    public static String email(String raw) {
        String cleaned = text(raw, SHORT_MAX);
        if (cleaned == null) return null;
        return cleaned.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}") ? cleaned.toLowerCase() : null;
    }

    /**
     * An {@code http(s)} URL, or null. Rejects {@code javascript:}, {@code data:}
     * and protocol-relative forms, all of which become script when placed in an
     * {@code href} or followed by a redirect.
     */
    public static String httpUrl(String raw) {
        String cleaned = text(raw, LONG_MAX);
        if (cleaned == null) return null;
        String lower = cleaned.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return null;
        if (cleaned.contains("\n") || cleaned.contains("\r")) return null;
        try {
            java.net.URI uri = java.net.URI.create(cleaned);
            return uri.getHost() == null ? null : cleaned;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TextSanitizer() {}
}
