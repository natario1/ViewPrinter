package com.otaliastudios.printer;


import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextWatcher;
import android.text.method.TransformationMethod;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An helper class that can help implementing the {@link AutoSplitView} interface
 * for {@link TextView} subclasses.
 *
 * Basically, the only thing needed is a text subclass that implements the desired interface,
 * and delegates all calls to the text helper. This will take care of chain management and other
 * nasty behaviors.
 *
 * @see AutoSplitView
 * @see AutoSplitTextView
 * @see AutoSplitEditText
 * @param <T> the auto split class
 */
public class AutoSplitTextHelper<T extends TextView & AutoSplitView> implements TextWatcher, TransformationMethod {

    interface Provider<T extends TextView & AutoSplitView> {
        @Nullable
        AutoSplitTextHelper<T> getHelper(@Nullable T view);
    }

    private static final String TAG = AutoSplitTextHelper.class.getSimpleName();
    private static final PrinterLogger LOG = PrinterLogger.create(TAG);

    private static final char NEWLINE = '\n';
    private static final char REPLACEMENT = '\uFEFF';

    private final T mView;
    private final Provider<T> mProvider;
    private int mPageNumber = -1;
    private int mColumnNumber = -1;
    private boolean mHasHiddenNewline = false;
    private boolean mActionInProgress = false;
    private Rect mTmp = new Rect();
    private CharSequence mDetachText;

    T mPre;
    T mPost;

    /**
     * Creates an helper to delegate calls. The provider is just an
     * interface that can let us have the helpers for any view in the chain.
     *
     * @param view the TextView that needs to implement AutoSplitView
     * @param provider an helper provider
     */
    public AutoSplitTextHelper(T view, Provider<T> provider) {
        mView = view;
        mProvider = provider;
        mView.addTextChangedListener(this);
        mView.setTransformationMethod(this);
    }

    public void onAttach(int pageNumber, int columnNumber) {
        mPageNumber = pageNumber;
        mColumnNumber = columnNumber;
    }

    // With this policy, onPreDetach and onDetach are called only for the first view of the chain.
    // The others are removed immediately, and this ensures consistency when collecting / re-adding
    // views. This works very well and makes things like DocumentColumn#collect() easy.
    public void onPreDetach() {
        mDetachText = null;
        String prefix = logPrefix();
        LOG.i(prefix, "onPreDetach:", "first:", isFirst());
        if (isFirst()) {
            mDetachText = getChainText();
            LOG.v(prefix, "onPreDetach:", "we are first! Storing text.", mDetachText.length(), "chars.");
            AutoSplitTextHelper<T> helper = next();
            while (helper != null) {
                LOG.v(prefix, "onPreDetach:", "removing", helper.logPrefix());
                removeFromChain(helper.mView);
                helper = next();
            }
            LOG.v(prefix, "onPreDetach:", "now we should be first & last!", isLast());
        }
    }

    // Again, this will be called just for the root view.
    public void onDetach() {
        mPageNumber = -1;
        mColumnNumber = -1;
        String prefix = logPrefix();
        LOG.i(prefix, "onDetach");
        if (mDetachText != null) {
            LOG.v(prefix, "onDetach:", "restoring text.", mDetachText.length(), "chars.");
            mView.setText(mDetachText);
        }
    }

    private String logPrefix() {
        return "[page:" + mPageNumber +
                " col:" + mColumnNumber +
                " view:" + Utils.mark(mView) +
                " num:" + position() + "]";
    }

    private AutoSplitTextHelper<T> previous() {
        return mProvider.getHelper(mPre);
    }

    private AutoSplitTextHelper<T> next() {
        return mProvider.getHelper(mPost);
    }

    private boolean isFirst() {
        return mPre == null;
    }

    private boolean isLast() {
        return mPost == null;
    }

    private boolean isFirst(T view) {
        return view.previous() == null;
    }

    private boolean isLast(T view) {
        return view.next() == null;
    }

    private void removeFromChain(@NonNull T child) {
        AutoSplitTextHelper<T> helper = mProvider.getHelper(child);
        LOG.v(helper.logPrefix(), "removeFromChain:", "being removed now.");
        if (helper.mPost != null) helper.next().mPre = helper.mPre;
        if (helper.mPre != null) helper.previous().mPost = helper.mPost;
        ((ViewGroup) child.getParent()).removeView(child);
    }

    public CharSequence getChainText() {
        if (!isFirst()) {
            return previous().getChainText();
        } else if (isLast()) {
            return mView.getText();
        } else {
            SpannableStringBuilder text = new SpannableStringBuilder("");
            T curr = mView;
            while (true) {
                text.append(curr.getText());
                if (isLast(curr)) break;
                curr = (T) curr.next();
            }
            return text;
        }
    }

    public void setChainText(CharSequence text) {
        if (!isFirst()) {
            previous().setChainText(text);
        } else {
            mView.setText(text);
        }
    }

    public T getFirst() {
        return isFirst() ? mView : previous().getFirst();
    }


    public int position() {
        return isFirst() ? 0 : previous().position() + 1;
    }

    private boolean isActionInProgress() {
        return isFirst() ? mActionInProgress : previous().isActionInProgress();
    }

    private void setActionInProgress(boolean progress) {
        if (isFirst()) {
            mActionInProgress = progress;
        } else {
            previous().setActionInProgress(progress);
        }
    }

    private T split() {
        T view = (T) mView.split();
        AutoSplitTextHelper<T> post = next();
        mProvider.getHelper(view).mPre = mView;
        mProvider.getHelper(view).mPost = mPost;
        if (post != null) post.mPre = view;
        mPost = view;
        LOG.i(logPrefix(), "split:", "Creating new view at position", next().logPrefix());
        return view;
    }

    public void onSplit(T source, T dest) {
        Utils.mark(dest, Utils.mark(source));
        dest.setTextSize(TypedValue.COMPLEX_UNIT_PX, source.getTextSize());
        dest.setTypeface(source.getTypeface());
        dest.setTextColor(source.getTextColors());
        dest.setTextAlignment(source.getTextAlignment());
        dest.setInputType(source.getInputType());
        if (Build.VERSION.SDK_INT >= 23) dest.setBreakStrategy(source.getBreakStrategy());
        if (Build.VERSION.SDK_INT >= 26) dest.setJustificationMode(source.getJustificationMode());
        dest.setContentDescription(source.getContentDescription());
        dest.setGravity(source.getGravity());
        if (Build.VERSION.SDK_INT >= 21) dest.setElevation(source.getElevation());
        dest.setRotationX(source.getRotationX());
        dest.setRotationY(source.getRotationY());
        dest.setRotation(source.getRotation());
        dest.setAlpha(source.getAlpha());
        dest.setScaleX(source.getScaleX());
        dest.setScaleY(source.getScaleY());
        dest.setLayoutParams(new ViewGroup.MarginLayoutParams(source.getLayoutParams()));
        dest.setSingleLine(source.getMaxLines() == 1);
        dest.setMaxLines(source.getMaxLines());
        dest.setMinLines(source.getMinLines());
        dest.setFocusable(source.isFocusable());
        dest.setFocusableInTouchMode(source.isFocusableInTouchMode());
        dest.setPadding(source.getPaddingLeft(), source.getPaddingTop(),
                source.getPaddingRight(), source.getPaddingBottom());
    }

    private Editable edit(T view) {
        return (Editable) view.getText();
    }

    // We want to know the height of a single line.
    // There surely are smarter ways of achieving this,
    // But I have no time now.
    public int minimumSize() {
        Layout layout;
        int minSize;
        if (mView.getLayout() != null) {
            layout = mView.getLayout();
        } else {
            LOG.i(logPrefix(), "minimumSize:", "layout absent, creating a new StaticLayout.");
            layout = new StaticLayout("line", mView.getPaint(),
                    Integer.MAX_VALUE, Layout.Alignment.ALIGN_NORMAL,
                    mView.getLineSpacingMultiplier(), mView.getLineSpacingExtra(),
                    mView.getIncludeFontPadding());
        }
        layout.getLineBounds(0, mTmp);
        minSize = mTmp.height();
        minSize += mView.getPaddingTop();
        minSize += mView.getPaddingBottom();
        LOG.v(logPrefix(), "minimumSize:", "returning", minSize);
        return minSize;
    }

    // Whitespaces policy:
    // - if ' ': layout keeps it on the previousView line, so we don't forward it. We keep it here.
    // - if '\n': layout keeps it on the following line (obvious), so it is passed to the nextView view.
    public boolean releaseSpace(int space) {
        // This can happen, I don't know, let's consume.
        if (!mView.isLaidOut()) return true;

        // Returning FALSE means that the pager will try to pass the isLast view from this column to the nextView.
        // This is unacceptable if we have a following mPost view. We can only return FALSE if we delete ourselves,
        // So the pager will act on another view.

        String logPrefix = logPrefix();
        LOG.w(logPrefix, "releaseSpace:", "asked to release", space);
        int length = mView.length();
        int height = mView.getLayout().getHeight();

        if (length == 0) {
            // This can only happen if we are the isFirst and only view, isFirst() && isLast().
            // If we are second, we are at the top of the page AND at the bottom.
            //                   There is no one in this page that can notify that it is too small.
            // If we are not isLast, we have a mPost. But we can't have a mPost while our length is 0.
            // Given these two, it makes sense to return false: the engine will move us to nextView page.
            if (!isFirst()) LOG.e(logPrefix, "releaseSpace:", "length=0 but we are not the first. THIS MAKES NO SENSE.");
            if (!isLast()) LOG.e(logPrefix, "releaseSpace:", "length=0 but we are not the last. THIS MAKES NO SENSE.");
            return false;
        }
        setActionInProgress(true);
        LOG.i(logPrefix, "releaseSpace:", "will we create another?", isLast());
        final T next = isLast() ? split() : mPost;
        // TODO use a better indicator than nextView.getParent in DocumentColumn
        int split = computeReleaseOffset(space);
        CharSequence keep = mView.getText().subSequence(0, split);
        CharSequence give = mView.getText().subSequence(split, length);
        boolean passFocus = mView.hasFocus() && mView.getSelectionEnd() >= split;
        LOG.i(logPrefix, "releaseSpace:", "splitOffset:", split, "selectionEnd:", mView.getSelectionEnd());
        edit(mView).replace(split, length, "");
        edit(next).insert(0, give);

        int consumed;
        if (mView.length() == 0) {
            // If we gave all, delete us.
            // The consumed count must include our padding and margins.
            consumed = height;
            consumed += mView.getPaddingTop();
            consumed += mView.getPaddingBottom();
            if (mView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margin = (ViewGroup.MarginLayoutParams) mView.getLayoutParams();
                consumed += margin.topMargin;
                consumed += margin.bottomMargin;
            }
            removeFromChain(mView);
        } else {
            // Use standard measuring. Not reliable when empty (layoutHeight > 0).
            consumed = height - mView.getLayout().getHeight();
        }

        if (passFocus) {
            next.post(new Runnable() {
                @Override
                public void run() {
                    LOG.v("requestingFocus:", next.getParent());
                    next.requestFocus();
                }
            });
        }

        setActionInProgress(false);
        LOG.i(logPrefix, "releaseSpace:", "returning", consumed >= space);
        return consumed >= space;
    }


    private int computeReleaseOffset(int space) {
        // This is simpler than it looks: we must pass whole lines.
        String logPrefix = logPrefix();
        Layout layout = mView.getLayout();
        int count = layout.getLineCount();
        int removed = 0;
        LOG.v(logPrefix, "computeReleaseOffset:", "space:", space, "lineCount:", count);
        int removeLine = 0;
        for (int i = count - 1; i >= 0; i--) {
            layout.getLineBounds(i, mTmp);
            removed += mTmp.height();
            if (removed >= space) {
                removeLine = i;
                break;
            }
        }
        // We have to remove line i and all subsequent lines.
        LOG.i(logPrefix, "computeReleaseOffset:", "removing line:", removeLine, "and subsequent.");
        return Math.max(layout.getOffsetForHorizontal(removeLine, 0) - 1, 0);
    }


    // Whitespaces policy:
    // We keep fetching words from the other view. These are split as follows:
    // - "Hey you my friend"     ->  "Hey "  "you "  "my "   "friend "
    // - "\nHey you my\nFriend"  ->  "\n"    "Hey "  "you "  "my"  "\n"  "Friend"
    // These are tried one after one. Each one causes an height increment that we might be
    // able to afford or not. We stop when we can't afford more.
    public boolean acceptSpace(int space) {
        // This can happen, I don't know, let's consume.
        if (!mView.isLaidOut()) return true;

        String logPrefix = logPrefix();
        // Returning FALSE means that the pager will try to:
        // 1. Pass the first view of this page to the previous page
        //    ^ FIXED THIS IN THE PAGER. It won't happen.
        // 2. Accept the first view of the nextView page at the end of this page
        //
        // So if we are not last(), we must ensure we never return FALSE.
        // That is, if !isLast(), return true. This is well handler here.

        int height = mView.getLayout().getHeight();
        int target = height + space;
        LOG.w(logPrefix, "acceptSpace:", "asked to accept:", space, "height:", height, "target:", target);
        if (isLast()) {
            // No one to accept from. We can safely return false. The pager will not try to move
            // us to the previous page, due to specific behavior for AutoSplitViews.
            LOG.w(logPrefix, "acceptSpace:", "quick end because we are the last. Returning false");
            return false;
        }

        if (mPost.length() == 0) {
            // Next view is empty. Remove it and try again with the following. Should be null though.
            LOG.w(logPrefix, "acceptSpace:", "quick end: nextView view empty, removing.");
            removeFromChain(mPost);
            return acceptSpace(space);
        }

        setActionInProgress(true);
        Editable source = edit(mPost);
        Editable dest = edit(mView);
        Pattern pattern = Pattern.compile("\\s"); // TODO: We only support ' ' and '\n', don't catch others
        CharSequence word = "";
        int wordCount = 0;
        while (mView.getLayout().getHeight() <= target) {
            if (source.length() == 0) break;
            Matcher matcher = pattern.matcher(source);
            int end;
            if (matcher.find()) {
                end = matcher.end();
                if (end > 1 && source.charAt(end - 1) == '\n') {
                    end--;
                }
            } else {
                end = source.length();
            }
            word = source.subSequence(0, end);
            LOG.v(logPrefix, "acceptSpace:", "Found!", "end:", end, "word:(" + word + ")");
            source.replace(0, end, "");
            dest.append(word);
            wordCount++;
        }

        // The while loops ends when the source is empty, or when we took to much.
        if (mView.getLayout().getHeight() > target) {
            LOG.i(logPrefix, "acceptSpace:", "Out of the loop. We took to much. Returning word:", word);
            dest.replace(dest.length() - word.length(), dest.length(), "");
            source.insert(0, word);
            wordCount--;
        }

        setActionInProgress(false);
        if (mPost.length() == 0) {
            LOG.w(logPrefix, "acceptSpace:", "We took everything from post view. Removing.", next().isLast());
            // View is empty. Remove it, and try again with the nextView mPost.
            // This is a bit flaky, I should think more about it.
            removeFromChain(mPost);
            int remaining = target - mView.getLayout().getHeight(); // >= 0
            return acceptSpace(remaining);
        } else {
            // Post is not empty, but if we take something else, we take too much.
            // I would say that we can return true here.
            LOG.i(logPrefix, "acceptSpace:", "ENDED.", "words:", wordCount, "finalHeight:", mView.getLayout().getHeight());
            return true;
        }
    }

    //region TextWatcher and newline transformations

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (isActionInProgress()) return;
        mHasHiddenNewline = mView.getText().length() > 0 && mView.getText().charAt(0) == NEWLINE;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isActionInProgress()) return;
        setActionInProgress(true);
        boolean hasHiddenNewLine = s.length() > 0 && s.charAt(0) == NEWLINE;
        if (mHasHiddenNewline && !hasHiddenNewLine && !isFirst()) {
            // An hidden newline was removed! Pass stuff back.
            // This might make us empty, and eventually, delete us.
            LOG.w(logPrefix(), "afterTextChanged:",
                    "we had a hidden newline, but not anymore.",
                    "Passing \"isFirst line\" back.");
            previous().acceptSpace(0);
        }
        setActionInProgress(false);
    }

    // This appears to be more reliable that the view onKeyEvent, which is not called always.
    // TextViews should not call this, only EditTexts.
    public InputConnection createInputConnection(InputConnection base) {
        return base == null ? null : new InputConnectionWrapper(base, true) {

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                // TODO: this could be improved by working even when we are not empty.
                // The behavior should be 'delete isLast character from mPre'.
                // In that case, we should check also that getSelectionStart() == 0.
                if (!isFirst() && mView.getText().length() == 0 &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    removeFromChain(mView);
                    return false;
                }
                return super.sendKeyEvent(event);
            }
        };
    }


    // Hides trailing newlines with an invisible character.
    // The newline is still visible when using getText() or similar,
    // this is just a rendering transformation.
    @Override
    public CharSequence getTransformation(final CharSequence source, View view) {
        return new CharSequence() {
            @Override
            public int length() {
                return source.length();
            }

            @Override
            public char charAt(int index) {
                if (!isFirst() && index == 0 && source.charAt(0) == NEWLINE) {
                    return REPLACEMENT;
                } else {
                    return source.charAt(index);
                }
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                if (!isFirst() && start == 0 && source.charAt(0) == NEWLINE) {
                    char[] buf = new char[end-start];
                    for (int i = 0; i < buf.length; i++) {
                        buf[i] = charAt(start + i);
                    }
                    return new String(buf);
                } else {
                    return source.subSequence(start, end);
                }
            }
        };
    }

    @Override
    public void onFocusChanged(
            View view, CharSequence sourceText,
            boolean focused, int direction,
            Rect previouslyFocusedRect) {
    }
}
