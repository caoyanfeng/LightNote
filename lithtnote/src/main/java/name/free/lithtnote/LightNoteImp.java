package name.free.lithtnote;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mi on 16-10-22.
 * <p>LightNote实现类</p>
 * <p>参考：https://github.com/mthli/Knife</p>
 * <p>相对于Knife，修复的bug包括</p>
 * <p>1、{@link #bullet()}</p>
 * <p>2、{@link #quote()}}</p>
 * <p>相对于Knife，功能优化包括：</p>
 * <p>1、{@link #contains(int)}</p>
 * <p>2、{@link #bullet()}</p>
 * <p>3、{@link #quote()}}</p>
 */
public class LightNoteImp extends EditText implements LightNote {
    private int bulletColor = 0;//bullet的颜色
    private int bulletRadius = 0;//bullet的半径
    private int bulletGapWidth = 0;//
    private boolean historyEnable = true;
    private int historySize = 100;//最多返回100次前的操作？
    private int linkColor = 0;//链接的颜色
    private boolean linkUnderline = true;
    private int quoteColor = 0;//引用线的颜色
    private int quoteStripeWidth = 0;//引用线的宽度
    private int quoteGapWidth = 0;

    private List<Editable> historyList = new LinkedList<>();
    private boolean historyWorking = false;
    private int historyCursor = 0;

    private SpannableStringBuilder inputBefore;
    private Editable inputLast;


    public LightNoteImp(Context context) {
        super(context);
    }

    public LightNoteImp(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LightNoteImp(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public LightNoteImp(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    /**
     * 从R.styleable.LightNoteImp中导入XML属性
     */
    private void init(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.LightNoteImp);
        bulletColor = array.getColor(R.styleable.LightNoteImp_bulletColor, 0);//项目符号的颜色
        bulletRadius = array.getDimensionPixelSize(R.styleable.LightNoteImp_bulletRadius, 0);//项目符号的半径
        bulletGapWidth = array.getDimensionPixelSize(R.styleable.LightNoteImp_bulletGapWidth, 0);//项目符合和文本之间的间隙
        historyEnable = array.getBoolean(R.styleable.LightNoteImp_historyEnable, true);//是否支持历史
        historySize = array.getInt(R.styleable.LightNoteImp_historySize, 100);//支持的历史长度，默认100
        linkColor = array.getColor(R.styleable.LightNoteImp_linkColor, 0);//链接颜色
        linkUnderline = array.getBoolean(R.styleable.LightNoteImp_linkUnderline, true);
        quoteColor = array.getColor(R.styleable.LightNoteImp_quoteColor, 0);//引用颜色
        quoteStripeWidth = array.getDimensionPixelSize(R.styleable.LightNoteImp_quoteStripeWidth, 0);
        quoteGapWidth = array.getDimensionPixelSize(R.styleable.LightNoteImp_quoteGapWidth, 0);
        array.recycle();
        if (historyEnable && historySize <= 0) {
            throw new IllegalArgumentException("historySize must > 0");
        }

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        addTextChangedListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeTextChangedListener(this);
    }

    // BoldSpan & ItalicSpan ===================================================================================
    @Override
    public void bold() {
        boolean valid=!contains(LightNote.FORMAT_BOLD);
        if (valid) {
            styleValid(Typeface.BOLD);
        } else {
            styleInValid(Typeface.BOLD);
        }
    }

    @Override
    public void italic() {
        boolean valid=!contains(LightNote.FORMAT_ITALIC);
        if (valid) {
            styleValid(Typeface.ITALIC);
        } else {
            styleInValid(Typeface.ITALIC);
        }
    }

    private void styleValid(int style) {
        styleValid(style, getSelectionStart(), getSelectionEnd());
    }

    @Override
    public void styleValid(int style, int start, int end) {
        switch (style) {
            case Typeface.NORMAL:
            case Typeface.BOLD:
            case Typeface.ITALIC:
            case Typeface.BOLD_ITALIC:
                break;
            default:
                return;

        }
        if (start >= end) return;
        getEditableText().setSpan(new StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    public void styleInValid(int style) {
        styleInValid(style, getSelectionStart(), getSelectionEnd());

    }

    //调试
    @Override
    public void styleInValid(int style, int start, int end) {
        switch (style) {
            case Typeface.NORMAL:
            case Typeface.BOLD:
            case Typeface.ITALIC:
            case Typeface.BOLD_ITALIC:
                break;
            default:
                return;
        }

        if (start >= end) {
            return;
        }
        Editable editable = getEditableText();
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                int mStart = editable.getSpanStart(span);//span开始的位置
                int mEnd = editable.getSpanEnd(span);//span结束的位置
                editable.removeSpan(span);
                if (mStart < start) {
                    styleValid(style, mStart, start);
                }
                if (mEnd > end) {
                    styleValid(style, end, mEnd);
                }
            }
        }

    }

    /**
     * @return start和end的范围刚好在style类型的span的起点和终点内，即[start,end]∈[mStart,mEnd]，则返回true，否则返回false。
     */
    private boolean containStyle(int style, int start, int end) {
        switch (style) {
            case Typeface.NORMAL:
            case Typeface.BOLD:
            case Typeface.ITALIC:
            case Typeface.BOLD_ITALIC:
                break;
            default:
                return false;
        }
        if (start > end) {
            return false;
        }
        Editable editable = getEditableText();
        if (start == end) {
            if (start - 1 < 0 || start + 1 > editable.length()) {
                return false;
            } else {
                StyleSpan[] before = editable.getSpans(start - 1, start, StyleSpan.class);
                StyleSpan[] after = editable.getSpans(start, start + 1, StyleSpan.class);
                return before.length > 0 && after.length > 0 && before[0].getStyle() == style && after[0].getStyle() == style;
            }
        } else {
            StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    int mStart = editable.getSpanStart(span);//span开始的位置
                    int mEnd = editable.getSpanEnd(span);//span结束的位置
                    if (start >= mStart && end <= mEnd) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    // UnderlineSpan ===================================================================================
    @Override
    public void underline() {
        boolean valid=!contains(FORMAT_UNDERLINED);
        if (valid) {
            underlineValid();
        } else {
            underlineInValid();
        }
    }

    private void underlineValid() {
        underlineValid(getSelectionStart(), getSelectionEnd());
    }

    @Override
    public void underlineValid(int start, int end) {
        if (start >= end) {
            return;
        }
        getEditableText().setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    private void underlineInValid() {
        underlineInValid(getSelectionStart(), getSelectionEnd());

    }

    //调试
    @Override
    public void underlineInValid(int start, int end) {
        if (start >= end) {
            return;
        }
        UnderlineSpan[] spans = getEditableText().getSpans(start, end, UnderlineSpan.class);
        Editable editable = getEditableText();
        for (UnderlineSpan span : spans) {
            int mStart = editable.getSpanStart(span);//span开始的位置
            int mEnd = editable.getSpanEnd(span);//span结束的位置
            editable.removeSpan(span);
            if (mStart < start) {
                underlineValid(mStart, start);
            }
            if (mEnd > end) {
                underlineValid(end, mEnd);
            }
        }
    }

    /**
     * @return start和end的范围刚好在underline类型的span的起点和终点内，即[start,end]∈[mStart,mEnd]，则返回true，否则返回false。
     */
    protected <T> boolean containStyle(Class<T> t, int start, int end) {
        if (start > end) {
            return false;
        }
        Editable editable = getEditableText();
        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            } else {
                T[] before = editable.getSpans(start - 1, start, t);
                T[] after = editable.getSpans(start, start + 1, t);
                return before.length > 0 && after.length > 0;
            }
        } else {
            T[] spans = editable.getSpans(start, end, t);
            for (T span : spans) {
                int mStart = editable.getSpanStart(span);//span开始的位置
                int mEnd = editable.getSpanEnd(span);//span结束的位置
                if (start >= mStart && end <= mEnd) {
                    return true;
                }
            }
            return false;
        }
    }

    // StrikethroughSpan ===================================================================================
    @Override
    public void strikethrough() {
        boolean valid=!contains(FORMAT_STRIKETHROUGH);
        if (valid) {
            strikethroughValid();
        } else {
            strikethroughInValid();
        }
    }

    public void strikethroughValid() {
        strikethroughValid(getSelectionStart(), getSelectionEnd());

    }

    @Override
    public void strikethroughValid(int start, int end) {
        if (start >= end) {
            return;
        }

        getEditableText().setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void strikethroughInValid() {
        strikethroughInValid(getSelectionStart(), getSelectionEnd());
    }

    @Override
    public void strikethroughInValid(int start, int end) {
        if (start >= end) {
            return;
        }
        Editable editable = getEditableText();
        StrikethroughSpan[] spans = editable.getSpans(start, end, StrikethroughSpan.class);
        for (StrikethroughSpan span : spans) {
            int mStart = editable.getSpanStart(span);//span开始的位置
            int mEnd = editable.getSpanEnd(span);//span结束的位置
            editable.removeSpan(span);
            if (mStart < start) {
                strikethroughValid(mStart, start);
            }
            if (mEnd > end) {
                strikethroughValid(end, mEnd);
            }
        }

    }
    // BulletSpan ===================================================================================

    /**
     * <p>三种情况</p>
     * <p>1、选中区域没有bullet,则添加所有项目符号</p>
     * <p>2、选中区域每一段都有bullet，则去除所有项目符号</p>
     * <p>3、选中区域部分段落有bullet，则为没有bullet的段落添加bullet</p>
     * <p>这里的操作是，为每一段添加bullet，并记录所有span，然后判断是否是第2种情况，如果是，则删除所有bullet</p>
     */
    @Override
    public void bullet() {
        paragraphSymbol(BulletSpan.class);
    }

    // QuoteSpan ===================================================================================
    @Override
    public void quote() {
        paragraphSymbol(QuoteSpan.class);
    }

    /**
     * 泛型类，用于段前加上符号，例如quote和bullet.
     *
     * @param t {@link QuoteSpan},{@link BulletSpan}
     */
    public <T> void paragraphSymbol(Class<T> t) {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        int start = getSelectionStart();//选中位置的起点
        int end = getSelectionEnd();//选中位置的终点
        int paragraphStart = 0;//每一段开始位置
        int bulletSum = 0;//选中区域bullet的个数
        int lineSum = 0;//选中区域的非空行数
        List<T[]> spanList = new ArrayList<>();//选中区域所有的bullet
        for (int i = 0; i < lines.length; i++) {
            int paragraphEnd = paragraphStart + lines[i].length();//每一段的结尾位置
            if (lineSum > 0) {//已经遇到选中区域的起点
                if (lines[i].length() != 0) {
                    lineSum++;
                }
                T[] spans = getEditableText().getSpans(paragraphStart, paragraphEnd, t);
                if (spans.length == 0) {
                    paragraphSymbol(t, paragraphStart, paragraphEnd);
                    spans = getEditableText().getSpans(paragraphStart, paragraphEnd, t);
                } else {
                    bulletSum++;
                }
                spanList.add(spans);
                if (paragraphEnd >= end) {//选中的最后一行
                    break;
                }
            } else {//还没有遇到选中区域的起点
                if (paragraphEnd >= start) {//选中的第1行
                    if (lines[i].length() != 0) {
                        lineSum++;
                    }
                    T[] spans = getEditableText().getSpans(paragraphStart, paragraphEnd, t);
                    if (spans.length == 0) {
                        paragraphSymbol(t, paragraphStart, paragraphEnd);
                        spans = getEditableText().getSpans(paragraphStart, paragraphEnd, t);
                    } else {
                        bulletSum++;
                    }
                    spanList.add(spans);
                }
                if (paragraphEnd >= end) {//表明只选了i行
                    break;
                }
            }
            paragraphStart = paragraphEnd + 1;//下一个位置,加1是计入了\n
        }
        if (lineSum == bulletSum && spanList.size() > 0) {//表明选中的部分都是bullet,则要将其全部去除
            for (T[] bulletSpan : spanList) {
                for (T span : bulletSpan) {
                    getText().removeSpan(span);
                }
            }
        }
    }

    private <T> void paragraphSymbol(Class<T> t, int start, int end) {
        try {
            getEditableText().setSpan(t.newInstance(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // URLSpan =====================================================================================
    @Override
    public void link(String link) {
        link(link, getSelectionStart(), getSelectionEnd());
    }

    public void link(String link, int start, int end) {
        if (link != null && !TextUtils.isEmpty(link.trim())) {
            linkValid(link, start, end);
        } else {
            linkInvalid(start, end);
        }
    }

    protected void linkValid(String link, int start, int end) {
        if (start >= end) {
            return;
        }

        linkInvalid(start, end);
        getEditableText().setSpan(new URLSpan(link), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // Remove all span in selection, not like the boldInvalid()
    protected void linkInvalid(int start, int end) {
        if (start >= end) {
            return;
        }

        URLSpan[] spans = getEditableText().getSpans(start, end, URLSpan.class);
        for (URLSpan span : spans) {
            getEditableText().removeSpan(span);
        }
    }

    protected boolean containLink(int start, int end) {
        if (start > end) {
            return false;
        }

        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            } else {
                URLSpan[] before = getEditableText().getSpans(start - 1, start, URLSpan.class);
                URLSpan[] after = getEditableText().getSpans(start, start + 1, URLSpan.class);
                return before.length > 0 && after.length > 0;
            }
        } else {
            StringBuilder builder = new StringBuilder();

            for (int i = start; i < end; i++) {
                if (getEditableText().getSpans(i, i + 1, URLSpan.class).length > 0) {
                    builder.append(getEditableText().subSequence(i, i + 1).toString());
                }
            }

            return getEditableText().subSequence(start, end).toString().equals(builder.toString());
        }
    }

    @Override
    public void clearFormats() {
        setText(getEditableText().toString());
        setSelection(getEditableText().length());
    }

    public boolean contains(int format) {
        switch (format) {
            case FORMAT_BOLD:
                return containStyle(Typeface.BOLD, getSelectionStart(), getSelectionEnd());
            case FORMAT_ITALIC:
                return containStyle(Typeface.ITALIC, getSelectionStart(), getSelectionEnd());
            case FORMAT_UNDERLINED:
                return containStyle(UnderlineSpan.class, getSelectionStart(), getSelectionEnd());
            case FORMAT_STRIKETHROUGH:
                return containStyle(StrikethroughSpan.class, getSelectionStart(), getSelectionEnd());
            case FORMAT_LINK:
                return containLink(getSelectionStart(), getSelectionEnd());
            default:
                return false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (!historyEnable || historyWorking) {
            return;
        }
        inputBefore = new SpannableStringBuilder(s);

    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
        // DO NOTHING HERE
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (!historyEnable || historyWorking) {
            return;
        }

        inputLast = new SpannableStringBuilder(s);
        if (s != null && s.toString().equals(inputBefore.toString())) {
            return;
        }
        if (historyList.size() >= historySize) {//超过100次的保存限额
            historyList.remove(0);
        }
        historyList.add(inputBefore);
        historyCursor = historyList.size();

    }

    // Redo/Undo ===================================================================================
    public void redo() {
        if (!redoValid()) {
            return;
        }
        historyWorking = true;
        if (historyCursor >= historyList.size() - 1) {
            historyCursor = historyList.size();
            setText(inputLast);
        } else {
            historyCursor++;
            setText(historyList.get(historyCursor));
        }
        setSelection(getEditableText().length());
        historyWorking = false;
    }

    public void undo() {
        if (!undoValid()) return;
        historyWorking = true;
        historyCursor--;
        setText(historyList.get(historyCursor));
        setSelection(getEditableText().length());
        historyWorking = false;
    }

    public boolean redoValid() {
        if (!historyEnable || historyWorking) {
            return false;
        }
        return historyCursor < historyList.size() - 1 || historyCursor >= historyList.size() - 1 && inputLast != null;
    }

    public boolean undoValid() {
        if (!historyEnable || historyWorking) {
            return false;
        }
        if (historyList.size() <= 0 || historyCursor <= 0) {
            return false;
        }
        return true;
    }
}