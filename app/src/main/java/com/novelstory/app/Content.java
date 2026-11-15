package com.novelstory.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Content {

    public static final int BUILTIN_TOTAL_CHAPTERS = 75;

    private Content() {
    }

    public static final class Question {
        public final String text;
        public final String[] options;
        public final int answer;

        Question(String text, String[] options, int answer) {
            this.text = text;
            this.options = options;
            this.answer = answer;
        }
    }

    static File importedRoot(Context c, String novelId) {
        return new File(new File(c.getFilesDir(), "novels"), novelId);
    }

    private static InputStream open(Context c, String novelId, String rel) throws IOException {
        if (novelId.isEmpty()) {
            return c.getAssets().open("novel/" + rel);
        }
        return new FileInputStream(new File(importedRoot(c, novelId), rel));
    }

    private static String readText(Context c, String novelId, String rel) {
        try (InputStream in = open(c, novelId, rel);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            return out.toString("UTF-8");
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean exists(Context c, String novelId, String rel) {
        try (InputStream in = open(c, novelId, rel)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String chapterRel(String lang, int chapter) {
        return lang + "/chapter" + String.format(Locale.US, "%02d", chapter) + ".md";
    }

    private static String other(String lang) {
        return "tl".equals(lang) ? "en" : "tl";
    }

    public static int chapterCount(Context c, String novelId) {
        int max = 0;
        for (String lang : new String[]{"tl", "en"}) {
            int n = 1;
            while (exists(c, novelId, chapterRel(lang, n))) {
                n++;
            }
            max = Math.max(max, n - 1);
        }
        return max;
    }

    public static boolean isAvailable(Context c, String novelId) {
        return chapterCount(c, novelId) > 0;
    }

    public static int chapterCount(Context c) {
        return chapterCount(c, Prefs.getActiveNovel(c));
    }

    private static String resolveLang(Context c, String novelId, String lang, int chapter) {
        if (exists(c, novelId, chapterRel(lang, chapter))) {
            return lang;
        }
        if (exists(c, novelId, chapterRel(other(lang), chapter))) {
            return other(lang);
        }
        return lang;
    }

    public static String loadChapter(Context c, String lang, int chapter) {
        String novel = Prefs.getActiveNovel(c);
        String text = readText(c, novel, chapterRel(resolveLang(c, novel, lang, chapter), chapter));
        return text == null ? "" : text;
    }

    public static String chapterTitle(Context c, String lang, int chapter) {
        String md = loadChapter(c, lang, chapter);
        for (String line : md.split("\n")) {
            String t = line.trim();
            if (t.startsWith("#")) {
                String h = t.replaceFirst("^#+\\s*", "");
                if (h.contains("Kabanata") || h.contains("Chapter")) {
                    return h;
                }
            }
        }
        return "Chapter " + chapter;
    }

    public static int wordCount(String md) {
        String s = md.trim();
        return s.isEmpty() ? 0 : s.split("\\s+").length;
    }

    public static List<Question> questions(Context c, String lang, int chapter) {
        String novel = Prefs.getActiveNovel(c);
        List<Question> out = new ArrayList<>();
        String raw = readText(c, novel, lang + "/quiz.json");
        if (raw == null) {
            raw = readText(c, novel, other(lang) + "/quiz.json");
        }
        if (raw == null) {
            return out;
        }
        try {
            JSONArray all = new JSONArray(raw);
            for (int i = 0; i < all.length(); i++) {
                JSONObject block = all.getJSONObject(i);
                if (block.getInt("chapter") != chapter) {
                    continue;
                }
                JSONArray qs = block.getJSONArray("questions");
                for (int j = 0; j < qs.length(); j++) {
                    JSONObject q = qs.getJSONObject(j);
                    JSONArray opts = q.getJSONArray("options");
                    String[] arr = new String[opts.length()];
                    for (int k = 0; k < arr.length; k++) {
                        arr[k] = opts.getString(k);
                    }
                    out.add(new Question(q.getString("q"), arr, q.getInt("answer")));
                }
            }
        } catch (JSONException e) {
            out.clear();
        }
        return out;
    }
}
