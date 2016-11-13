package name.free.lithtnote;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
 * <P>3、{@link #paragraphSymbol(Class, int, int)},项目符号和引用符合并存的问题</P>
 * <P>4、{@link #paragraphSymbol(Class, int, int)},项目符号和引用符合有空行的问题</P>
 * <p>相对于Knife，功能优化包括：</p>
 * <p>1、{@link #containStyle(Class, int, int, int)}</p>
 * <p>2、{@link #bullet()}</p>
 * <p>3、{@link #quote()}}</p>
 * <p>相对于Knife，新功能包括：</p>
 * <p>1、插入图片</p>
 */
public class LightNoteImp extends EditText implements LightNote {
    private  ImageClickListener mImageClickListener;
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
    private Context mContext;

    public LightNoteImp(Context context) {
        super(context);
    }

    public LightNoteImp(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        mContext=context;
    }

    public LightNoteImp(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
        mContext=context;
    }

    public LightNoteImp(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
        mContext=context;
    }

    /**
     * 从R.styleable.LightNoteImp中导入XML属性
     */
    private void init(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.LightNoteImp);
        bulletColor = array.getColor(R.styleable.LightNoteImp_bulletColor, 0);//项目符号的颜色
        bulletRadius = array.getDimensionPixelSize(R.styleable.LightNoteImp_bulletRadius, 0);//项目符号的半径，这个是private类型
        bulletGapWidth = array.getDimensionPixelSize(R.styleable.LightNoteImp_bulletGapWidth, 0);//项目符合和文本之间的间隙
        historyEnable = array.getBoolean(R.styleable.LightNoteImp_historyEnable, true);//是否支持历史
        historySize = array.getInt(R.styleable.LightNoteImp_historySize, 100);//支持的历史长度，默认100
        linkColor = array.getColor(R.styleable.LightNoteImp_linkColor, 0);//链接颜色
        linkUnderline = array.getBoolean(R.styleable.LightNoteImp_linkUnderline, true);
        quoteColor = array.getColor(R.styleable.LightNoteImp_quoteColor, 0);//引用颜色
        quoteStripeWidth = array.getDimensionPixelSize(R.styleable.LightNoteImp_quoteStripeWidth, 0);//private类型
        quoteGapWidth = array.getDimensionPixelSize(R.styleable.LightNoteImp_quoteGapWidth, 0);//private类型
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
        boolean valid = !containStyle(StyleSpan.class, Typeface.BOLD, getSelectionStart(), getSelectionEnd());
        if (valid) {
            styleValid(new StyleSpan(FORMAT_BOLD), getSelectionStart(), getSelectionEnd());
        } else {
            styleInValid(StyleSpan.class, Typeface.BOLD, getSelectionStart(), getSelectionEnd());
        }
    }

    @Override
    public void italic() {
        boolean valid = !containStyle(StyleSpan.class, Typeface.ITALIC, getSelectionStart(), getSelectionEnd());
        if (valid) {
            styleValid(new StyleSpan(FORMAT_ITALIC), getSelectionStart(), getSelectionEnd());
        } else {
            styleInValid(StyleSpan.class, Typeface.ITALIC, getSelectionStart(), getSelectionEnd());
        }
    }

    private void styleValid(Object span, int start, int end) {
        if (start >= end) return;
        getEditableText().setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    //调试
    public <T> void styleInValid(Class<T> t, int style, int start, int end) {
        if (start >= end) {
            return;
        }
        Editable editable = getEditableText();
        T[] spans = editable.getSpans(start, end, t);
        for (T span : spans) {
            //bold和italic都是StyleSpan类型，需要再保证style
            if (span.getClass() == StyleSpan.class && ((StyleSpan) span).getStyle() != style) {
                continue;
            }
            int mStart = editable.getSpanStart(span);//span开始的位置
            int mEnd = editable.getSpanEnd(span);//span结束的位置
            editable.removeSpan(span);
            if (style == FORMAT_LINK) {//链接不再把[start,end]外的部分重新加上
                continue;
            }
            if (mStart < start) {
                styleValid(style, mStart, start);
            }
            if (mEnd > end) {
                styleValid(style, end, mEnd);
            }
        }

    }

    // UnderlineSpan ===================================================================================
    @Override
    public void underline() {
        boolean valid = !containStyle(UnderlineSpan.class, FORMAT_UNDERLINED, getSelectionStart(), getSelectionEnd());
        if (valid) {
            styleValid(new UnderlineSpan(), getSelectionStart(), getSelectionEnd());
        } else {
            styleInValid(UnderlineSpan.class, FORMAT_UNDERLINED, getSelectionStart(), getSelectionEnd());
        }
    }

    /**
     * @return start和end的范围刚好在underline类型的span的起点和终点内，即[start,end]∈[mStart,mEnd]，则返回true，否则返回false。
     */
    protected <T> boolean containStyle(Class<T> t, int style, int start, int end) {
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
                if (span.getClass() == StyleSpan.class && ((StyleSpan) span).getStyle() != style) {//如果是bold或italic
                    continue;
                }
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
        boolean valid = !containStyle(StrikethroughSpan.class, FORMAT_STRIKETHROUGH, getSelectionStart(), getSelectionEnd());
        if (valid) {
            styleValid(new StrikethroughSpan(), getSelectionStart(), getSelectionEnd());
        } else {
            styleInValid(StrikethroughSpan.class, FORMAT_STRIKETHROUGH, getSelectionStart(), getSelectionEnd());
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
        for (int i = 0; i <lines.length; i++) {
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

    /**
     * 通过反射机制来实例化BulletSpan、QuoteSpan.
     * 注意：如果某一行既有BulletSpan，也有QuoteSpan，则QuoteSpan必须在BulletSpan之前
     * */
    private <T> void paragraphSymbol(Class<T> t, int start, int end) {
        if (start==end){//表明选中区域有空行，则通过插入空白符来使得改行可以有BulletSpan或QuoteSpan
            getText().insert(start," ");
            end++;
        }
        try {
            Object what=null;
            if (t==BulletSpan.class){
                Constructor cons=t.getConstructor(int.class,int.class);
                what=cons.newInstance(bulletGapWidth,bulletColor);
            }else if (t==QuoteSpan.class){
                BulletSpan[] spans = getEditableText().getSpans(start, end, BulletSpan.class);
                if (spans.length!=0){
                    for (BulletSpan span:spans){
                        getText().removeSpan(span);
                    }
                }
                Constructor cons=t.getConstructor(int.class);
                what=cons.newInstance(quoteColor);
                getEditableText().setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (spans.length!=0){
                   for (BulletSpan span:spans){
                       getEditableText().setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                   }
                }
                return;
            }
            if (what==null)what=t.newInstance();
            getEditableText().setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // URLSpan =====================================================================================
    @Override
    public void link(Activity context) {
        link(context, getSelectionStart(), getSelectionEnd());
    }

    /**
     * 插入本地图片，图片太大时不能加载。这里最大的长宽根据横竖屏来粗略判断，长度是宽度的两倍。
     * @param context 插入图片的上下文.
     * @param fileUri 图片Uri.
     * */
    @Override
    public void imageSpan(Activity context, Uri fileUri,LoadImageListener loadImageListener) {
        if (fileUri!=null){
            Configuration mConfiguration = context.getResources().getConfiguration();
            BitMapAsyncTask bitMapAsyncTask=null;
            if (mConfiguration.orientation==mConfiguration.ORIENTATION_PORTRAIT){//竖屏的时候最大宽度是getWidth()
               bitMapAsyncTask=new BitMapAsyncTask(context,getWidth(),getWidth()*2,loadImageListener);
            }else if (mConfiguration.orientation==mConfiguration.ORIENTATION_LANDSCAPE){//横屏的时候最大宽度是getWidth()/2
                bitMapAsyncTask=new BitMapAsyncTask(context,getWidth()/2,getWidth(),loadImageListener);
            }
            bitMapAsyncTask.execute(fileUri);
        }

    }

    public void link(final Activity context, final int start, final int end) {
        boolean valid = !containStyle(URLSpan.class, FORMAT_LINK, getSelectionStart(), getSelectionEnd());
        if (valid) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(false);
            final EditText editText = new EditText(context);
            builder.setView(editText);
            builder.setTitle(R.string.dialog_title);
            builder.setNegativeButton(R.string.dialog_button_cancel, null);
            builder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // DO NOTHING HERE
                    final String link = editText.getText().toString();
                    if (link.length() > 0) {
                        styleValid(new URLSpan(link){
                            @Override
                            public void onClick(View widget) {
                                super.onClick(widget);
                                Uri uri = Uri.parse(link);
                                Intent it = new Intent(Intent.ACTION_VIEW,uri);
                                it.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
                                context.startActivity(it);
                            }
                        }, start, end);
                        setMovementMethod(LinkMovementMethod.getInstance());
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.create().show();

        } else {
            styleInValid(URLSpan.class, FORMAT_LINK, start, end);
        }
    }

    @Override
    public void clearFormats() {
        setText(getEditableText().toString());
        setSelection(getEditableText().length());
    }

    @Override
    public void setImageClickListener(ImageClickListener imageClickListener) {
        mImageClickListener=imageClickListener;
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

    /**
     * 异步加载图片.
     * */
    private class BitMapAsyncTask extends AsyncTask<Uri,Void,Bitmap>{
        private Activity mContext;
        private Uri imageUri;
        private int mMaxWidth;
        private int mMaxHeight;
        private LoadImageListener mLoadImageListener;

        public BitMapAsyncTask(Activity context,int maxWidth,int maxHeight){
            mContext=context;
            mMaxHeight=maxHeight;
            mMaxWidth=maxWidth;
        }
        public BitMapAsyncTask(Activity context,int maxWidth,int maxHeight,LoadImageListener loadImageListener){
           this(context,maxWidth,maxHeight);
            mLoadImageListener=loadImageListener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mLoadImageListener!=null){
                mLoadImageListener.onStart();
            }

        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            imageUri=params[0];
            if (mLoadImageListener!=null){
                mLoadImageListener.onResume();
            }
            if (mMaxWidth>mMaxHeight){//如果
                return BitMapUtil.getScaledBitMap(imageUri,mMaxHeight,mMaxWidth);
            }else {
                return BitMapUtil.getScaledBitMap(imageUri,mMaxWidth,mMaxHeight);
            }

        }

        /**
         * 参考：http://blog.csdn.net/u010132993/article/details/51260539
         * 设置ImageSpan的同时，也设置了一个对应的ClickableSpan.
         * */
        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageSpan imageSpan=new ImageSpan(mContext,bitmap,ImageSpan.ALIGN_BASELINE);
            String image=imageUri.toString();
            SpannableString spannableString = new SpannableString(image);
            spannableString.setSpan(imageSpan, 0, image.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ClickableSpan clickSpan=new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (mImageClickListener!=null){
                        mImageClickListener.onClick(imageUri,bitmap);
                    }
                }
            };
            spannableString.setSpan(clickSpan, 0, image.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            int index = getSelectionStart();
            Editable editText = getEditableText();
            if (index < 0 || index >= editText.length()) {
                editText.append(spannableString);
            } else {
                editText.insert(index, spannableString);
            }
            editText.insert(index+spannableString.length(),"\n");
            setMovementMethod(LinkMovementMethod.getInstance());
            if (mLoadImageListener!=null){
                mLoadImageListener.onFinish();
            }
        }
    }

}
