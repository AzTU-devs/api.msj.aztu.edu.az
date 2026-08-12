package az.edu.aztu.msj.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/** Sanitizes reviewer rich-text (HTML) to a safe subset — no scripts, no styles. */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.basic()
            .addTags("h3", "h4")
            .removeTags("img");   // reviewers write text, not embedded images

    public static String clean(String html) {
        if (html == null || html.isBlank()) return null;
        return Jsoup.clean(html, SAFELIST);
    }

    private HtmlSanitizer() {}
}
