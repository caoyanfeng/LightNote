package name.free.lithtnote;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextWatcher;
import android.view.View;

import java.io.File;

/**
 * Created by mi on 16-10-22.
 * LightNote接口
 */
public interface LightNote extends TextWatcher{
    /**
     * 图片加载的监听
     */
    interface LoadImageListener {
        void onStart();

        void onResume();

        void onFinish();
    }

    /**
     * 图片点击的监听
     */
    interface ImageClickListener {
        /**
         * @param fileUri 原图地址，可能失效
         * @param bitmap 显示的图片，大图片可能经过压缩
         * */
        void onClick(Uri fileUri,Bitmap bitmap);

    }

    int FORMAT_BOLD = Typeface.BOLD;//1
    int FORMAT_ITALIC = Typeface.ITALIC;//2
    int FORMAT_UNDERLINED = 0x03;
    int FORMAT_STRIKETHROUGH = 0x04;
    int FORMAT_BULLET = 0x05;
    int FORMAT_QUOTE = 0x06;
    int FORMAT_LINK = 0x07;
    int FORMAT_IMAGE = 0x08;

    /**
     * 字体加粗或相反
     */
    void bold();

    /**
     * 斜体或相反
     */
    void italic();

    /**
     * 下划线或相反
     */
    void underline();

    /**
     * 贯穿线或去除
     */
    void strikethrough();

    /**
     * 项目符号或去除
     */
    void bullet();

    /**
     * 段首竖线，引用符号
     */
    void quote();

    /**
     * 超链接，或去除
     *
     * @param context,用于生成对话框
     */
    void link(Activity context);

    /**
     * 插入图片
     *
     * @param fileUri,文件命，包含路径
     */
    void imageSpan(Activity context, Uri fileUri, LoadImageListener loadImageListener);

    /**
     * 去除所有富文本格式
     */
    void clearFormats();
    /**
     * 添加图片点击的监听
     * @param imageClickListener 监听
     */
    void setImageClickListener(ImageClickListener imageClickListener);
}
