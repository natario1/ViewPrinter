package com.otaliastudios.printer;


import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;

/**
 * An {@link EditText} implementation that works well when laid out inside
 * a {@link DocumentView}.
 *
 * @see DocumentTextView
 */
public class DocumentTextView extends TextView {

    private final static String TAG = DocumentTextView.class.getSimpleName();
    private final static PrinterLogger LOG = PrinterLogger.create(TAG);

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
        if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            LOG.i("onLayout:", "We are wrap content. Looking if we can scroll.");
            // TODO: don't do this if getMaxLines is >= 0 (< MAX_VALUE). Same for getMaxheight.
            if (canScrollVertically(-1) || canScrollVertically(1)) {
                LOG.w("onLayout:", "We can scroll. Notifying the parent column.");
                notifyColumn();
            }
        }
    }

    private void notifyColumn() {
        DocumentColumn column = null;
        View current = this;
        while (true) {
            ViewParent parent = current.getParent();
            if (parent == null || !(parent instanceof ViewGroup)) {
                break;
            }

            if (parent instanceof DocumentColumn) {
                column = (DocumentColumn) parent;
                break;
            }
            current = (ViewGroup) parent;
        }

        if (column != null) {
            column.notifyTooSmall(this, current);
        }
    }
}
