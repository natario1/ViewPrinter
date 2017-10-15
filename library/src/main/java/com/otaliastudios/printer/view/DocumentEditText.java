package com.otaliastudios.printer.view;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.EditText;

import com.otaliastudios.printer.DocumentHelper;
import com.otaliastudios.printer.DocumentView;

/**
 * An {@link EditText} implementation that works well when laid out inside
 * a {@link DocumentView}.
 *
 * @see DocumentTextView
 * @see DocumentHelper
 */
@SuppressLint("AppCompatCustomView")
public class DocumentEditText extends EditText {

    public DocumentEditText(Context context) {
        super(context);
    }

    public DocumentEditText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public DocumentEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        DocumentHelper.onLayout(this);
    }
}
