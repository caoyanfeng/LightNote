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
 * <p>相对于Knife，新功能优化包括：</p>
 * <p>相对于Knife，做的功能优化包括：</p>
 * <p>1、{@link #contains(int)}</p>
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
    * */
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
    public void bold(boolean valid) {
        if (valid) {
            styleValid(Typeface.BOLD);
        } else {
            styleInValid(Typeface.BOLD);
        }
    }

    @Override
    public void italic(boolean valid) {
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
        styleInValid(style,getSelectionStart(),getSelectionEnd());

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
                if (mStart<start){
                    styleValid(style,mStart,start);
                }
                if (mEnd>end) {
                    styleValid(style,end,mEnd);
                }
            }
        }

    }

    /**
     * @return start和end的范围刚好在style类型的span的起点和终点内，即[start,end]∈[mStart,mEnd]，则返回true，否则返回false。
     * */
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
            for (StyleSpan span:spans){
                if (span.getStyle() == style){
                    int mStart = editable.getSpanStart(span);//span开始的位置
                    int mEnd = editable.getSpanEnd(span);//span结束的位置
                    if (start>=mStart&&end<=mEnd){
                        return true;
                    }
                }
            }
            return false;
        }

    }
    // UnderlineSpan ===================================================================================
    @Override
    public void underline(boolean valid) {
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
            if (mStart<start){
                underlineValid(mStart,start);
            }
            if (mEnd>end) {
                underlineValid(end,mEnd);
            }
        }
    }

    /**
     * @return start和end的范围刚好在underline类型的span的起点和终点内，即[start,end]∈[mStart,mEnd]，则返回true，否则返回false。
     * */
    protected boolean containUnderline(int start, int end) {
        if (start > end) {
            return false;
        }
        Editable editable = getEditableText();
        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            } else {
                UnderlineSpan[] before = editable.getSpans(start - 1, start, UnderlineSpan.class);
                UnderlineSpan[] after = editable.getSpans(start, start + 1, UnderlineSpan.class);
                return before.length > 0 && after.length > 0;
            }
        } else {
            UnderlineSpan[] spans = editable.getSpans(start, end, UnderlineSpan.class);
            for (UnderlineSpan span:spans){
                    int mStart = editable.getSpanStart(span);//span开始的位置
                    int mEnd = editable.getSpanEnd(span);//span结束的位置
                    if (start>=mStart&&end<=mEnd){
                        return true;
                }
            }
            return false;
        }
    }
    // StrikethroughSpan ===================================================================================
    @Override
    public void strikethrough(boolean valid) {
        if (valid) {
            strikethroughValid();
        } else {
            strikethroughInValid();
        }
    }

    public void strikethroughValid() {
        strikethroughValid(getSelectionStart(),getSelectionEnd());

    }

    @Override
    public void strikethroughValid(int start, int end) {
        if (start >= end) {
            return;
        }

        getEditableText().setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void strikethroughInValid() {
        strikethroughInValid(getSelectionStart(),getSelectionEnd());
    }

    @Override
    public void strikethroughInValid(int start, int end) {
        if (start >= end) {
            return;
        }
        Editable editable=getEditableText();
        StrikethroughSpan[] spans = editable.getSpans(start, end, StrikethroughSpan.class);
        for (StrikethroughSpan span : spans) {
            int mStart = editable.getSpanStart(span);//span开始的位置
            int mEnd = editable.getSpanEnd(span);//span结束的位置
            editable.removeSpan(span);
            if (mStart<start) {
               strikethroughValid(mStart,start);
            }
            if (mEnd>end){
                strikethroughValid(end,mEnd);
            }
        }

    }
    /**
     * @return start和end的范围刚好在strikethrough类型的span的起点和终点内，即[start,end]∈[mStart,mEnd]，则返回true，否则返回false。
     * */
    private boolean containStrikethrough(int start, int end) {
        if (start > end) {
            return false;
        }
        Editable editable=getEditableText();
        if (start == end) {
            if (start - 1 < 0 || start + 1 > editable.length()) {
                return false;
            } else {
                StrikethroughSpan[] before = editable.getSpans(start - 1, start, StrikethroughSpan.class);
                StrikethroughSpan[] after = editable.getSpans(start, start + 1, StrikethroughSpan.class);
                return before.length > 0 && after.length > 0;
            }
        } else {
            StrikethroughSpan[] spans = editable.getSpans(start, end, StrikethroughSpan.class);
            for (StrikethroughSpan span:spans){
                int mStart = editable.getSpanStart(span);//span开始的位置
                int mEnd = editable.getSpanEnd(span);//span结束的位置
                if (start>=mStart&&end<=mEnd){
                    return true;
                }
            }
            return false;

        }
    }
    // BulletSpan ===================================================================================
    @Override
    public void bullet(boolean valid) {
        if (valid) {
            bulletValid();
        } else {
            bulletInvalid();
        }
    }

    protected void bulletValid(){
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        for (int i=0;i<lines.length;i++){
            if (containBullet(i)){//第i行已经包含了，就continue
                continue;
            }
            int lineStart=0;
            for (int j=0;j<i;j++){
                lineStart+=lines[j].length()+1;
            }
            int lineEnd=lineStart+lines[i].length();
            if (lineStart>=lineEnd)continue;
            // Find selection area inside
            int bulletStart = 0;
            int bulletEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            }

            if (bulletStart < bulletEnd) {
                getEditableText().setSpan(new BulletSpan(), bulletStart, bulletEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        }
    }

    protected void bulletInvalid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (!containBullet(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            int bulletStart = 0;
            int bulletEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            }

            if (bulletStart < bulletEnd) {
                BulletSpan[] spans = getEditableText().getSpans(bulletStart, bulletEnd, BulletSpan.class);
                for (BulletSpan span : spans) {
                    getEditableText().removeSpan(span);
                }
            }
        }
    }


    protected boolean containBullet() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart+=lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                list.add(i);
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                list.add(i);
            }
        }

        for (Integer i : list) {
            if (!containBullet(i)) {
                return false;
            }
        }
        return true;
    }
    /**
     * 计算第index行是否有Bullet
     * @param index 选中的区域里面，第一行为0行，这是第index行
     * @return 已经包含Bullet则返回true,否则返回false
     * */
    protected boolean containBullet(int index) {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        if (index < 0 || index >= lines.length) {
            return false;
        }

        int start = 0;
        for (int i = 0; i < index; i++) {
            start = start + lines[i].length() + 1;
        }

        int end = start + lines[index].length();
        if (start >= end) {
            return false;
        }

        BulletSpan[] spans = getEditableText().getSpans(start, end, BulletSpan.class);
        return spans.length > 0;
    }
    // QuoteSpan ===================================================================================
    @Override
    public void quote(boolean valid) {
        if (valid) {
            quoteValid();
        } else {
            quoteInvalid();
        }
    }

    protected void quoteValid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (containQuote(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1; // \n
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            int quoteStart = 0;
            int quoteEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            }

            if (quoteStart < quoteEnd) {
                getEditableText().setSpan(new QuoteSpan(), quoteStart, quoteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    protected void quoteInvalid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (!containQuote(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            int quoteStart = 0;
            int quoteEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            }

            if (quoteStart < quoteEnd) {
                QuoteSpan[] spans = getEditableText().getSpans(quoteStart, quoteEnd, QuoteSpan.class);
                for (QuoteSpan span : spans) {
                    getEditableText().removeSpan(span);
                }
            }
        }
    }

    protected boolean containQuote() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                list.add(i);
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                list.add(i);
            }
        }

        for (Integer i : list) {
            if (!containQuote(i)) {
                return false;
            }
        }

        return true;
    }

    protected boolean containQuote(int index) {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        if (index < 0 || index >= lines.length) {
            return false;
        }

        int start = 0;
        for (int i = 0; i < index; i++) {
            start = start + lines[i].length() + 1;
        }

        int end = start + lines[index].length();
        if (start >= end) {
            return false;
        }

        QuoteSpan[] spans = getEditableText().getSpans(start, end, QuoteSpan.class);
        return spans.length > 0;
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
                return containUnderline(getSelectionStart(), getSelectionEnd());
            case FORMAT_STRIKETHROUGH:
                return containStrikethrough(getSelectionStart(), getSelectionEnd());
            case FORMAT_BULLET:
                return containBullet();
            case FORMAT_QUOTE:
                return containQuote();
            case FORMAT_LINK:
                return containLink(getSelectionStart(), getSelectionEnd());
            default:
                return false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (!historyEnable||historyWorking){
            return;
        }
        inputBefore=new SpannableStringBuilder(s);

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
        if (historyList.size()>=historySize){//超过100次的保存限额
            historyList.remove(0);
        }
        historyList.add(inputBefore);
        historyCursor=historyList.size();

    }
    // Redo/Undo ===================================================================================
    public void redo(){
        if (!redoValid()){
            return;
        }
        historyWorking=true;
        if (historyCursor>=historyList.size()-1){
            historyCursor=historyList.size();
            setText(inputLast);
        }else {
            historyCursor++;
            setText(historyList.get(historyCursor));
        }
        setSelection(getEditableText().length());
        historyWorking=false;
    }
    public void undo(){
        if (!undoValid())return;
        historyWorking=true;
        historyCursor--;
        setText(historyList.get(historyCursor));
        setSelection(getEditableText().length());
        historyWorking=false;
    }
    public boolean redoValid(){
        if (!historyEnable||historyWorking){
            return false;
        }
        return historyCursor<historyList.size()-1||historyCursor>=historyList.size()-1&&inputLast!=null;
    }

    public boolean undoValid(){
        if (!historyEnable||historyWorking){
            return false;
        }
        if (historyList.size()<=0||historyCursor<=0){
            return false;
        }
        return true;
    }
    public void clearHistory(){
        if (historyList!=null){
            historyList.clear();
        }
    }

    public void hideSoftInput(){
        clearFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void showSoftInput() {
        requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public String toHtml() {
        return Parser.toHtml(getEditableText());
    }

    @Override
    public void fromHtml(String source) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(Parser.fromHtml(source));
        switchToKnifeStyle(builder, 0, builder.length());
        setText(builder);
    }

    protected void switchToKnifeStyle(Editable editable, int start, int end) {
        BulletSpan[] bulletSpans = editable.getSpans(start, end, BulletSpan.class);
        for (BulletSpan span : bulletSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            spanEnd = 0 < spanEnd && spanEnd < editable.length() && editable.charAt(spanEnd) == '\n' ? spanEnd - 1 : spanEnd;
            editable.removeSpan(span);
            editable.setSpan(new BulletSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        QuoteSpan[] quoteSpans = editable.getSpans(start, end, QuoteSpan.class);
        for (QuoteSpan span : quoteSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            spanEnd = 0 < spanEnd && spanEnd < editable.length() && editable.charAt(spanEnd) == '\n' ? spanEnd - 1 : spanEnd;
            editable.removeSpan(span);
            editable.setSpan(new QuoteSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        URLSpan[] urlSpans = editable.getSpans(start, end, URLSpan.class);
        for (URLSpan span : urlSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            editable.removeSpan(span);
            editable.setSpan(new URLSpan(span.getURL()), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
