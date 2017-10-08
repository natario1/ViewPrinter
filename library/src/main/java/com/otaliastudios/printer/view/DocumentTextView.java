package com.otaliastudios.printer.view;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import com.otaliastudios.printer.DocumentTextHelper;
import com.otaliastudios.printer.DocumentView;

/**
 * A {@link TextView} implementation that works well when laid out inside
 * a {@link DocumentView}.
 *
 * @see DocumentEditText
 * @see DocumentTextHelper
 */
@SuppressLint("AppCompatCustomView")
public class DocumentTextView extends TextView {

    public DocumentTextView(Context context) {
        super(context);
    }

    public DocumentTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public DocumentTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        DocumentTextHelper.onLayout(this);
    }
}
