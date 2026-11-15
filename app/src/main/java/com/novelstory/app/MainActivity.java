package com.novelstory.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends BaseActivity {

    private LinearLayout chapterList;
    private TextView titleText;
    private TextView progressText;
    private TextView noteText;
    private TextView versionText;
    private TextView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chapterList = findViewById(R.id.chapterList);
        titleText = findViewById(R.id.titleText);
        progressText = findViewById(R.id.progressText);
        noteText = findViewById(R.id.noteText);
        versionText = findViewById(R.id.versionText);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        versionText.setText(getString(R.string.version_fmt, BuildConfig.VERSION_NAME));
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        String lang = Prefs.getLang(this);
        boolean builtin = Prefs.getActiveNovel(this).isEmpty();
        titleText.setText(builtin ? getString(R.string.app_title) : getString(R.string.imported_novel));

        int count = Content.chapterCount(this);
        int total = builtin ? Content.BUILTIN_TOTAL_CHAPTERS : count;

        if (builtin && count < Content.BUILTIN_TOTAL_CHAPTERS) {
            noteText.setVisibility(View.VISIBLE);
            noteText.setText(getString(R.string.novel_incomplete_fmt, count, Content.BUILTIN_TOTAL_CHAPTERS));
        } else {
            noteText.setVisibility(View.GONE);
        }

        chapterList.removeAllViews();
        int passed = 0;
        for (int n = 1; n <= count; n++) {
            boolean isPassed = Prefs.isPassed(this, n);
            if (isPassed) {
                passed++;
            }
            boolean unlocked = n == 1 || Prefs.isPassed(this, n - 1);
            chapterList.addView(buildCard(n, lang, isPassed, unlocked));
        }
        progressText.setText(getString(R.string.progress_fmt, passed, total));
    }

    private View buildCard(int chapter, String lang, boolean passed, boolean unlocked) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(18);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText(Content.chapterTitle(this, lang, chapter));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(themeColor(R.attr.appText));

        TextView status = new TextView(this);
        status.setTextSize(13);
        status.setPadding(0, dp(6), 0, 0);
        if (passed) {
            status.setText(R.string.status_passed);
            status.setTextColor(themeColor(R.attr.appSuccess));
        } else if (!unlocked) {
            status.setText(R.string.status_locked);
            status.setTextColor(themeColor(R.attr.appMuted));
        } else {
            status.setText(R.string.status_open);
            status.setTextColor(themeColor(R.attr.appPrimary));
        }

        card.addView(title);
        card.addView(status);

        if (unlocked) {
            card.setOnClickListener(v -> {
                Intent i = new Intent(this, ReaderActivity.class);
                i.putExtra("chapter", chapter);
                startActivity(i);
            });
        } else {
            card.setAlpha(0.45f);
        }
        return card;
    }
}