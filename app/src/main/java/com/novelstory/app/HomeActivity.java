package com.novelstory.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends BaseActivity {

    private static final int REQ_IMPORT = 10;
    private static final String BUILTIN = "";

    private LinearLayout novelList;
    private TextView themeToggle;
    private TextView btnLangTl;
    private TextView btnLangEn;
    private TextView btnImport;
    private TextView importHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        novelList = findViewById(R.id.novelList);
        themeToggle = findViewById(R.id.themeToggle);
        btnLangTl = findViewById(R.id.btnLangTl);
        btnLangEn = findViewById(R.id.btnLangEn);
        btnImport = findViewById(R.id.btnImport);
        importHint = findViewById(R.id.importHint);

        themeToggle.setOnClickListener(v -> {
            Prefs.setDark(this, !Prefs.isDark(this));
            recreate();
        });
        btnLangTl.setOnClickListener(v -> changeLang("tl"));
        btnLangEn.setOnClickListener(v -> changeLang("en"));
        btnImport.setOnClickListener(v -> openPicker());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void changeLang(String lang) {
        if (!lang.equals(Prefs.getLang(this))) {
            Prefs.setLang(this, lang);
            recreate();
        }
    }

    private void render() {
        themeToggle.setText(Prefs.isDark(this) ? R.string.theme_to_light : R.string.theme_to_dark);
        styleChip(btnLangTl, "tl".equals(Prefs.getLang(this)));
        styleChip(btnLangEn, "en".equals(Prefs.getLang(this)));

        novelList.removeAllViews();
        novelList.addView(buildCard(BUILTIN, getString(R.string.app_title), true));

        String imported = Prefs.getImportedNovel(this);
        if (!imported.isEmpty() && Content.isAvailable(this, imported)) {
            novelList.addView(buildCard(imported, getString(R.string.imported_novel), false));
        }
    }

    private void styleChip(TextView tv, boolean selected) {
        tv.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        tv.setTextColor(selected ? themeColor(R.attr.appOnPrimary) : themeColor(R.attr.appText));
    }

    private View buildCard(String novelId, String title, boolean builtin) {
        int count = Content.chapterCount(this, novelId);
        int total = builtin ? Content.BUILTIN_TOTAL_CHAPTERS : count;
        int passed = 0;
        for (int n = 1; n <= count; n++) {
            if (Prefs.isPassedIn(this, novelId, n)) {
                passed++;
            }
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(20);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(themeColor(R.attr.appText));

        TextView progress = new TextView(this);
        progress.setText(getString(R.string.progress_fmt, passed, total));
        progress.setTextSize(14);
        progress.setTextColor(themeColor(R.attr.appPrimary));
        progress.setPadding(0, dp(8), 0, 0);

        card.addView(titleView);
        card.addView(progress);

        if (builtin && count < Content.BUILTIN_TOTAL_CHAPTERS) {
            TextView note = new TextView(this);
            note.setText(getString(R.string.novel_incomplete_fmt, count, Content.BUILTIN_TOTAL_CHAPTERS));
            note.setTextSize(13);
            note.setTextColor(themeColor(R.attr.appDanger));
            note.setPadding(0, dp(6), 0, 0);
            card.addView(note);
        }

        card.setOnClickListener(v -> {
            Prefs.setActiveNovel(this, novelId);
            startActivity(new Intent(this, MainActivity.class));
        });
        return card;
    }

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed"});
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        int result = NovelImporter.importZip(this, data.getData());
        if (result == NovelImporter.OK) {
            Toast.makeText(this, R.string.import_ok, Toast.LENGTH_SHORT).show();
        } else if (result == NovelImporter.ERR_INVALID) {
            Toast.makeText(this, R.string.import_invalid, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.import_read_error, Toast.LENGTH_LONG).show();
        }
        render();
    }
}
