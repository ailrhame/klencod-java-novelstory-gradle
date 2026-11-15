package com.novelstory.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static final String FILE = "novel_prefs";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getLang(Context c) {
        return sp(c).getString("lang", "tl");
    }

    public static void setLang(Context c, String lang) {
        sp(c).edit().putString("lang", lang).apply();
    }

    public static boolean isDark(Context c) {
        return sp(c).getBoolean("dark", false);
    }

    public static void setDark(Context c, boolean dark) {
        sp(c).edit().putBoolean("dark", dark).apply();
    }

    public static String getActiveNovel(Context c) {
        return sp(c).getString("novel", "");
    }

    public static void setActiveNovel(Context c, String novelId) {
        sp(c).edit().putString("novel", novelId).apply();
    }

    public static String getImportedNovel(Context c) {
        return sp(c).getString("imported", "");
    }

    public static void setImportedNovel(Context c, String novelId) {
        sp(c).edit().putString("imported", novelId).apply();
    }

    private static String progressKey(String novelId, int chapter) {
        return "passed_" + (novelId.isEmpty() ? "builtin" : novelId) + "_" + chapter;
    }

    public static boolean isPassed(Context c, int chapter) {
        return isPassedIn(c, getActiveNovel(c), chapter);
    }

    public static boolean isPassedIn(Context c, String novelId, int chapter) {
        return sp(c).getBoolean(progressKey(novelId, chapter), false);
    }

    public static void setPassed(Context c, int chapter) {
        sp(c).edit().putBoolean(progressKey(getActiveNovel(c), chapter), true).apply();
    }
}
