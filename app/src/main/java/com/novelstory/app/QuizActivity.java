package com.novelstory.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends BaseActivity {

    private static final int MODE_CHECK = 0;
    private static final int MODE_NEXT = 1;
    private static final int MODE_FINISH = 2;
    private static final int MODE_REREAD = 3;

    private static final class Item {
        final String text;
        final String[] options;
        final int correct;

        Item(String text, String[] options, int correct) {
            this.text = text;
            this.options = options;
            this.correct = correct;
        }
    }

    private int chapter;
    private final List<Item> items = new ArrayList<>();
    private int index = 0;
    private int mode = MODE_CHECK;

    private TextView quizProgress;
    private TextView questionText;
    private TextView feedbackText;
    private RadioGroup optionGroup;
    private RadioButton[] radios;
    private Button btnAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        chapter = getIntent().getIntExtra("chapter", 1);
        quizProgress = findViewById(R.id.quizProgress);
        questionText = findViewById(R.id.questionText);
        feedbackText = findViewById(R.id.feedbackText);
        optionGroup = findViewById(R.id.optionGroup);
        btnAction = findViewById(R.id.btnAction);
        radios = new RadioButton[]{
                findViewById(R.id.opt0), findViewById(R.id.opt1),
                findViewById(R.id.opt2), findViewById(R.id.opt3)
        };

        buildItems();

        if (items.isEmpty()) {
            Prefs.setPassed(this, chapter);
            Toast.makeText(this, R.string.no_quiz, Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        showQuestion();
        btnAction.setOnClickListener(v -> onAction());
    }

    private void buildItems() {
        List<Content.Question> qs = Content.questions(this, Prefs.getLang(this), chapter);
        for (Content.Question q : qs) {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < q.options.length; i++) {
                order.add(i);
            }
            Collections.shuffle(order);
            String[] opts = new String[order.size()];
            int correct = -1;
            for (int i = 0; i < order.size(); i++) {
                opts[i] = q.options[order.get(i)];
                if (order.get(i) == q.answer) {
                    correct = i;
                }
            }
            items.add(new Item(q.text, opts, correct));
        }
        Collections.shuffle(items);
    }

    private void showQuestion() {
        Item it = items.get(index);
        quizProgress.setText(getString(R.string.quiz_progress_fmt, index + 1, items.size()));
        questionText.setText(it.text);
        for (int i = 0; i < radios.length; i++) {
            if (i < it.options.length) {
                radios[i].setText(it.options[i]);
                radios[i].setVisibility(View.VISIBLE);
                radios[i].setEnabled(true);
                radios[i].setBackgroundResource(R.drawable.bg_option);
            } else {
                radios[i].setVisibility(View.GONE);
            }
        }
        optionGroup.clearCheck();
        feedbackText.setText("");
        btnAction.setText(R.string.check_answer);
        mode = MODE_CHECK;
    }

    private int selectedIndex() {
        int id = optionGroup.getCheckedRadioButtonId();
        for (int i = 0; i < radios.length; i++) {
            if (radios[i].getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private void onAction() {
        if (mode == MODE_CHECK) {
            checkAnswer();
        } else if (mode == MODE_NEXT) {
            showQuestion();
        } else if (mode == MODE_FINISH) {
            finish();
        } else if (mode == MODE_REREAD) {
            setResult(RESULT_FIRST_USER);
            finish();
        }
    }

    private void checkAnswer() {
        int sel = selectedIndex();
        if (sel < 0) {
            feedbackText.setText(R.string.select_answer);
            feedbackText.setTextColor(themeColor(R.attr.appMuted));
            return;
        }
        RadioButton chosen = radios[sel];
        Item it = items.get(index);

        if (sel == it.correct) {
            chosen.setBackgroundResource(R.drawable.bg_option_correct);
            feedbackText.setText(R.string.correct);
            feedbackText.setTextColor(themeColor(R.attr.appSuccess));
            index++;
            if (index >= items.size()) {
                Prefs.setPassed(this, chapter);
                setResult(RESULT_OK);
                feedbackText.setText(R.string.all_correct);
                btnAction.setText(R.string.finish);
                mode = MODE_FINISH;
            } else {
                btnAction.setText(R.string.next);
                mode = MODE_NEXT;
            }
        } else {
            chosen.setBackgroundResource(R.drawable.bg_option_wrong);
            feedbackText.setText(R.string.not_correct);
            feedbackText.setTextColor(themeColor(R.attr.appDanger));
            btnAction.setText(R.string.reread);
            mode = MODE_REREAD;
        }
    }
}
