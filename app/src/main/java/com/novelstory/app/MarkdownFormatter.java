package com.novelstory.app;

import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownFormatter {

    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|\\*([^*\\n]+)\\*");

    private MarkdownFormatter() {
    }

    public static CharSequence format(String md, int accentColor) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (String raw : md.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.matches("^(-{3,}|\\*\\s\\*\\s\\*)$")) {
                int start = sb.length();
                sb.append("·    ·    ·");
                sb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append("\n\n");
                continue;
            }
            if (line.startsWith("#")) {
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') {
                    level++;
                }
                String text = line.replaceFirst("^#+\\s*", "");
                float size = level <= 1 ? 1.6f : level == 2 ? 1.35f : 1.15f;
                int start = sb.length();
                sb.append(text);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(size), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(accentColor), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append("\n\n");
                continue;
            }
            appendInline(sb, line);
            sb.append("\n\n");
        }
        return sb;
    }

    private static void appendInline(SpannableStringBuilder sb, String text) {
        Matcher m = INLINE.matcher(text);
        int last = 0;
        while (m.find()) {
            sb.append(text.substring(last, m.start()));
            boolean bold = m.group(1) != null;
            String inner = bold ? m.group(1) : m.group(2);
            int start = sb.length();
            sb.append(inner);
            sb.setSpan(new StyleSpan(bold ? Typeface.BOLD : Typeface.ITALIC),
                    start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            last = m.end();
        }
        sb.append(text.substring(last));
    }
}
