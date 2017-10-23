package com.otaliastudios.printer;


import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * A {@link DocumentTextView} that implements the {@link AutoSplitView}
 * interface. This means that this view will split its content through a chain of
 * clone views, that might be moved to different columns or even pages.
 *
 * @see AutoSplitView
 * @see AutoSplitTextHelper
 */
public class AutoSplitTextView extends DocumentTextView implements AutoSplitView {

    private static final String TAG = AutoSplitTextView.class.getSimpleName();
    private static final PrinterLogger LOG = PrinterLogger.create(TAG);

    private AutoSplitTextHelper<AutoSplitTextView> mHelper;

    public AutoSplitTextView(Context context) {
        super(context); init(null);
    }

    public AutoSplitTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init(attrs);
    }

    public AutoSplitTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        mHelper = new AutoSplitTextHelper<>(this, new AutoSplitTextHelper.Provider<AutoSplitTextView>() {
            @Nullable
            @Override
            public AutoSplitTextHelper<AutoSplitTextView> getHelper(@Nullable AutoSplitTextView view) {
                return view == null ? null : view.mHelper;
            }
        });
    }

    @Override
    public Editable getText() {
        return (Editable) super.getText();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, BufferType.EDITABLE);
    }

    /**
     * Returns the whole text, included in this view and any other view of the
     * same chain. This might be an {@link Editable} but edits will not be reflected.
     * To do this you must use {@link #getText()} as always.
     *
     * @return the whole text of the chain
     */
    public CharSequence getChainText() {
        return mHelper.getChainText();
    }

    /**
     * Safely sets a new text for this chain. This is equal to fetching the root
     * of the chain with {@link #getFirst()}, and calling setText on it.
     *
     * @param text the new text.
     */
    public void setChainText(CharSequence text) {
        mHelper.setChainText(text);
    }

    /**
     * Returns the first view of the chain. It is safe to call
     * {@link #setText(int)} on it - changes will be dispatched to children if
     * necessary.
     *
     * @return the first view of the chain
     */
    public AutoSplitTextView getFirst() {
        return mHelper.getFirst();
    }

    @Override
    public int position() {
        return mHelper.position();
    }

    @Nullable
    @Override
    public AutoSplitTextView next() {
        return mHelper.mPost;
    }

    @Nullable
    @Override
    public AutoSplitTextView previous() {
        return mHelper.mPre;
    }

    @Override
    public void onAttach(int pageNumber, int columnNumber) {
        mHelper.onAttach(pageNumber, columnNumber);
    }

    @Override
    public void onPreDetach() {
        mHelper.onPreDetach();
    }

    @Override
    public void onDetach() {
        mHelper.onDetach();
    }

    @Override
    public int minimumSize() {
        return mHelper.minimumSize();
    }

    @Override
    public boolean releaseSpace(int space) {
        return mHelper.releaseSpace(space);
    }

    @Override
    public boolean acceptSpace(int space) {
        return mHelper.acceptSpace(space);
    }

    @NonNull
    @Override
    public AutoSplitTextView split() {
        AutoSplitTextView view = new AutoSplitTextView(getContext());
        view.onSplit(this);
        return view;
    }

    /**
     * We were split from source.
     * This is a good time to copy attributes (e.g. textSize) from that view
     * so we will have the same visual appearance.
     *
     * @param source Our source
     */
    @CallSuper
    protected void onSplit(AutoSplitTextView source) {
        mHelper.onSplit(source, this);
    }
}
