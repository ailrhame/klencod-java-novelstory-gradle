package com.novelstory.app;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public final class LocaleHelper {

    private LocaleHelper() {
    }

    public static Context wrap(Context base) {
        Locale locale = new Locale(Prefs.getLang(base));
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }
}
