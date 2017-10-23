package com.otaliastudios.printer;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * A {@link DocumentEditText} that implements the {@link AutoSplitView}
 * interface. This means that this view will split its content through a chain of
 * clone views, that might be moved to different columns or even pages.
 *
 * @see AutoSplitView
 * @see AutoSplitTextHelper
 */
public class AutoSplitEditText extends DocumentEditText implements AutoSplitView {

    private static final String TAG = AutoSplitEditText.class.getSimpleName();
    private static final PrinterLogger LOG = PrinterLogger.create(TAG);

    private final static int BACKGROUND_WHEN_FOCUSED = 0;
    private final static int BACKGROUND_ALWAYS = 1;

    private int mBackground;
    private Drawable mBackgroundDrawable;

    private AutoSplitTextHelper<AutoSplitEditText> mHelper;

    public AutoSplitEditText(Context context) {
        super(context); init(null);
    }

    public AutoSplitEditText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init(attrs);
    }

    public AutoSplitEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutoSplitEditText);
        mBackground = a.getInteger(R.styleable.AutoSplitEditText_chainBackground, BACKGROUND_ALWAYS);
        a.recycle();
        mHelper = new AutoSplitTextHelper<>(this, new AutoSplitTextHelper.Provider<AutoSplitEditText>() {
            @Nullable
            @Override
            public AutoSplitTextHelper<AutoSplitEditText> getHelper(@Nullable AutoSplitEditText view) {
                return view == null ? null : view.mHelper;
            }
        });
    }

    //region Background when focused

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        mBackgroundDrawable = background;
        checkBackground(hasFocus());
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        checkBackground(focused);
    }

    private void checkBackground(boolean hasFocus) {
        if (mBackground == BACKGROUND_ALWAYS) {
            super.setBackground(mBackgroundDrawable);
        } else if (mBackground == BACKGROUND_WHEN_FOCUSED) {
            super.setBackground(hasFocus ? mBackgroundDrawable : null);
        }
    }

    //endregion

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
    public AutoSplitEditText getFirst() {
        return mHelper.getFirst();
    }

    @Override
    public int position() {
        return mHelper.position();
    }

    @Nullable
    @Override
    public AutoSplitEditText next() {
        return mHelper.mPost;
    }

    @Nullable
    @Override
    public AutoSplitEditText previous() {
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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return mHelper.createInputConnection(super.onCreateInputConnection(outAttrs));
    }

    @NonNull
    @Override
    public AutoSplitEditText split() {
        AutoSplitEditText view = new AutoSplitEditText(getContext());
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
    protected void onSplit(AutoSplitEditText source) {
        mHelper.onSplit(source, this);
    }
}
