package com.otaliastudios.printer;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

/**
 * An {@link EditText} implementation that works well when laid out inside
 * a {@link DocumentView}.
 *
 * @see DocumentTextView
 */
@SuppressLint("AppCompatCustomView")
public class DocumentEditText extends EditText {

    private final static String TAG = DocumentEditText.class.getSimpleName();
    private final static PrinterLogger LOG = PrinterLogger.create(TAG);

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
        if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            LOG.i("onLayout:", "We are wrap content. Looking if we can scroll.");
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
