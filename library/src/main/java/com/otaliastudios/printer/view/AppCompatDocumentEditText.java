package com.otaliastudios.printer.view;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import com.otaliastudios.printer.DocumentTextHelper;
import com.otaliastudios.printer.DocumentView;

/**
 * A {@link AppCompatDocumentEditText} implementation that works well when
 * laid out inside a {@link DocumentView}.
 * Don't use if you don't have appcompat in your classpath.
 *
 * @see AppCompatDocumentTextView
 * @see DocumentTextHelper
 */
public class AppCompatDocumentEditText extends AppCompatEditText {

    public AppCompatDocumentEditText(Context context) {
        super(context);
    }

    public AppCompatDocumentEditText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AppCompatDocumentEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        DocumentTextHelper.onLayout(this);
    }
}
