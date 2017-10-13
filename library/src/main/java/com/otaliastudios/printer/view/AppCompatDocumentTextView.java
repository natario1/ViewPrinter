package com.otaliastudios.printer.view;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.otaliastudios.printer.DocumentHelper;
import com.otaliastudios.printer.DocumentView;

/**
 * A {@link android.support.v7.widget.AppCompatTextView} implementation that works well when
 * laid out inside a {@link DocumentView}.
 * Don't use if you don't have appcompat in your classpath.
 *
 * @see AppCompatDocumentEditText
 * @see DocumentHelper
 */
public class AppCompatDocumentTextView extends AppCompatTextView {

    public AppCompatDocumentTextView(Context context) {
        super(context);
    }

    public AppCompatDocumentTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AppCompatDocumentTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        DocumentHelper.onLayout(this);
    }
}
