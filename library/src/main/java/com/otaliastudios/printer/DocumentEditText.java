package com.otaliastudios.printer;


import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * An {@link EditText} implementation that works well when laid out inside
 * a {@link DocumentView}.
 *
 * @see DocumentTextView
 * @see DocumentHelper
 */
public class DocumentEditText extends AppCompatEditText implements Printable {

    public DocumentEditText(Context context) {
        super(context);
    }

    public DocumentEditText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        DocumentHelper.onLayout(this);
    }

    /**
     * Notifies that a print is going to happen.
     * This is the right moment to release / hide edit features
     * and artifacts.
     */
    @Override
    public void onPrePrint() {

    }

    /**
     * Notifies that the print process has ended.
     * Visual artifacts can be restored now.
     */
    @Override
    public void onPostPrint() {

    }
}
