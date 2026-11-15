package com.novelstory.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;

public abstract class BaseActivity extends Activity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Prefs.isDark(this) ? R.style.Theme_NovelStory_Dark : R.style.Theme_NovelStory_Light);
        super.onCreate(savedInstanceState);
    }

    protected int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    protected int themeColor(int attrId) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attrId, tv, true);
        return tv.data;
    }
}
