package __PKG__;

import java.util.*;
import java.util.regex.*;

public class EdupageParse {
    static final int F = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    static final Pattern CANCEL = Pattern.compile("odwo[łl]an|zwolnien|odpada|nie ma|cancel", F);

    static String norm(String s) { return s.toLowerCase(Locale.ROOT).replaceAll("[\\s.\\-]", ""); }

    static String strip(String s) {
        return s.replaceAll("<[^>]*>", " ").replaceAll("<[^>]*$", " ").replace("&nbsp;", " ").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replaceAll("\\s+", " ").trim();
    }

    static String after(String s, int from) { return from < 0 ? "" : s.substring(s.indexOf('>', from) + 1); }

    /** Zwraca wiersze zastępstw dla klasy: {numer lekcji, tekst, "1" gdy odwołana} */
    static List<String[]> rows(String html, String cls) {
        List<String[]> out = new ArrayList<>();
        String want = norm(cls);
        String[] secs = html.split("class=\"section(?=[\\s\"])");
        for (int i = 1; i < secs.length; i++) {
            String[] parts = secs[i].split("class=\"row(?=[\\s\"])");
            String h = norm(strip(after(parts[0], 0)));
            if (!(h.equals(want) || h.startsWith(want + ","))) continue;
            for (int j = 1; j < parts.length; j++) {
                String r = parts[j];
                int q = r.indexOf('"');
                String attr = q < 0 ? "" : r.substring(0, q);
                int pi = r.indexOf("class=\"period"), ii = r.indexOf("class=\"info");
                String label = "", text;
                if (ii >= 0) {
                    int ie = r.lastIndexOf('<', ii);
                    if (pi >= 0 && pi < ii && ie > pi) label = strip(after(r.substring(0, ie), pi));
                    text = strip(after(r, ii));
                } else text = strip(after(r, 0));
                if (text.isEmpty()) continue;
                boolean canc = attr.contains("remove") || CANCEL.matcher(text).find();
                out.add(new String[]{label, text, canc ? "1" : "0"});
            }
        }
        return out;
    }

    static Set<String> groupsIn(String t) {
        Set<String> s = new HashSet<>();
        Matcher m = Pattern.compile("(\\d+)\\s*/\\s*\\d+").matcher(t);
        while (m.find()) s.add(m.group(1));
        t = t.replaceAll("\\d+\\s*/\\s*\\d+", " "); // "1/2" = grupa 1 z 2
        m = Pattern.compile("gr(?:up\\w*)?\\.?\\s*(\\d+(?:\\s*(?:,|i|oraz|&)\\s*\\d+)*)", F).matcher(t);
        while (m.find()) { Matcher d = Pattern.compile("\\d+").matcher(m.group(1)); while (d.find()) s.add(d.group()); }
        m = Pattern.compile("(\\d+)\\s*\\.?\\s*grup", F).matcher(t);
        while (m.find()) s.add(m.group(1));
        return s;
    }

    static boolean mentionsOtherGroup(String t, String g) {
        if (g == null || g.isEmpty()) return false;
        Set<String> s = groupsIn(t);
        return !s.isEmpty() && !s.contains(g);
    }
}
