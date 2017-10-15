package com.otaliastudios.printer;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.otaliastudios.printer.view.DocumentEditText;
import com.otaliastudios.printer.view.DocumentTextView;

/**
 * Static utilities for views that might become smaller than they would like to,
 * without notifying the parent. In that case we want to move them to the next page, for example.
 *
 * It is recommended that these views call {@link #onLayout(View)} to let us determine if they would
 * like to be bigger, or {@link #onSpaceOver(View)} if they have already determined that.
 *
 */
public class DocumentHelper {

    private final static String TAG = DocumentHelper.class.getSimpleName();
    private final static PrinterLogger LOG = PrinterLogger.create(TAG);

    /**
     * To be called after the View has been laid out. If the view is presumably to small,
     * this will call {@link #onSpaceOver(View)} for you.
     *
     * @param view The view that has been laid out
     */
    public static void onLayout(View view) {
        if (view.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            // TODO: don't do this if getMaxLines is >= 0 (< MAX_VALUE). Same for getMaxheight.
            if (view.canScrollVertically(-1) || view.canScrollVertically(1)) {
                LOG.w("onLayout:", "We can scroll. Notifying the parent column.");
                onSpaceOver(view);
            }
        }
    }

    /**
     * Notifies the {@link DocumentView} that this view would like to be bigger than
     * it actually is. This might trigger a re-layout, for example moving the view to the
     * next page.
     *
     * @param view The view that would like to be bigger
     */
    public static void onSpaceOver(View view) {
        Container<?, ?> container = null;
        View current = view;
        while (true) {
            ViewParent parent = current.getParent();
            if (parent == null || !(parent instanceof ViewGroup)) {
                break;
            }

            if (parent instanceof Container) {
                container = (Container) parent;
                break;
            }
            current = (ViewGroup) parent;
        }

        if (container != null) {
            // View smallChild = this
            // View directChild = current
            container.onSpaceOver(null);
        }
    }
}
