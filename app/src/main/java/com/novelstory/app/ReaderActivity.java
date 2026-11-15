package com.novelstory.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ReaderActivity extends BaseActivity {

    private static final int REQ_QUIZ = 1;
    private static final int WORDS_PER_MINUTE = 250;

    private int chapter;
    private String lang;
    private int minSeconds;
    private long elapsedMs = 0;
    private long lastTick = 0;
    private boolean resumed = false;

    private ScrollView scroll;
    private TextView titleText;
    private TextView contentText;
    private TextView hintText;
    private Button btnQuiz;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.elapsedRealtime();
            if (resumed) {
                elapsedMs += now - lastTick;
            }
            lastTick = now;
            updateGate();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        chapter = getIntent().getIntExtra("chapter", 1);
        lang = Prefs.getLang(this);

        scroll = findViewById(R.id.scroll);
        titleText = findViewById(R.id.readerTitle);
        contentText = findViewById(R.id.contentText);
        hintText = findViewById(R.id.hintText);
        btnQuiz = findViewById(R.id.btnQuiz);
        View btnBack = findViewById(R.id.btnBack);

        String md = Content.loadChapter(this, lang, chapter);
        minSeconds = Math.max(60, Content.wordCount(md) * 60 / WORDS_PER_MINUTE);

        titleText.setText(Content.chapterTitle(this, lang, chapter));
        contentText.setText(MarkdownFormatter.format(md, themeColor(R.attr.appPrimary)));

        scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> updateGate());
        btnBack.setOnClickListener(v -> finish());
        btnQuiz.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra("chapter", chapter);
            startActivityForResult(i, REQ_QUIZ);
        });

        scroll.post(this::updateGate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        lastTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        if (resumed) {
            elapsedMs += SystemClock.elapsedRealtime() - lastTick;
        }
        resumed = false;
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private boolean isAtBottom() {
        View child = scroll.getChildAt(0);
        if (child == null) {
            return false;
        }
        int hidden = child.getBottom() - (scroll.getScrollY() + scroll.getHeight());
        return hidden <= dp(8);
    }

    private void updateGate() {
        boolean atBottom = isAtBottom();
        long remaining = Math.max(0, minSeconds - elapsedMs / 1000);
        boolean ready = atBottom && remaining == 0;

        btnQuiz.setEnabled(ready);
        btnQuiz.setBackgroundResource(ready ? R.drawable.bg_btn : R.drawable.bg_btn_disabled);

        if (ready) {
            hintText.setText(R.string.ready_quiz);
        } else if (!atBottom) {
            hintText.setText(R.string.scroll_to_end);
        } else {
            hintText.setText(getString(R.string.keep_reading_fmt, formatTime(remaining)));
        }
    }

    private String formatTime(long seconds) {
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void restartReading() {
        elapsedMs = 0;
        lastTick = SystemClock.elapsedRealtime();
        scroll.scrollTo(0, 0);
        Toast.makeText(this, R.string.reread_toast, Toast.LENGTH_LONG).show();
        scroll.post(this::updateGate);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_QUIZ) {
            return;
        }
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        } else if (resultCode == RESULT_FIRST_USER) {
            restartReading();
        }
    }
}
