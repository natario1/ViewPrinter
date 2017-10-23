package com.otaliastudios.printer;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.InputType;
import android.util.AttributeSet;

/**
 * A {@link android.widget.TextView} implementation that works well when laid out inside
 * a {@link DocumentView}.
 *
 * @see DocumentEditText
 * @see DocumentHelper
 */
public class DocumentTextView extends AppCompatTextView {

    public DocumentTextView(Context context) {
        super(context);
    }

    public DocumentTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        DocumentHelper.onLayout(this);
    }
}
