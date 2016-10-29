package name.free.lithtnote;


import android.text.TextWatcher;

/**
 * Created by mi on 16-10-22.
 * LightNote接口
 */
public interface LightNote extends TextWatcher{
    int FORMAT_BOLD = 0x01;
    int FORMAT_ITALIC = 0x02;
    int FORMAT_UNDERLINED = 0x03;
    int FORMAT_STRIKETHROUGH = 0x04;
    int FORMAT_BULLET = 0x05;
    int FORMAT_QUOTE = 0x06;
    int FORMAT_LINK = 0x07;
    /**
     * 字体加粗或相反
     * */
    void bold();
    /**
     * 斜体或相反
     * */
    void italic();
    /**
     * 下划线或相反
     * */
    void underline();
    /**
     * 贯穿线或去除
     *  @param valid,是否有效
     * */
    void strikethrough();
    /**
     * 项目符号或去除
     * */
    void bullet();
    /**
     * 段首竖线，引用符号
     * */
    void quote();
    /**
     * 超链接，或去除
     *  @param link,超链接
     * */
    void link(String link);
    /**
     * 应用富文本格式
     * @param style 富文本格式
     * @param start 起点
     * @param end 终点
     * */
    void styleValid(int style, int start, int end);
    /**
     * 取消富文本格式
     * @param style 富文本格式
     * @param start 起点
     * @param end 终点
     * */
    void styleInValid(int style,int start,int end);
    /**
     * 应用下划线
     * @param start 起点
     * @param end 终点
     * */
    void underlineValid(int start, int end);
    /**
     * 取消下划线
     * @param start 起点
     * @param end 终点
     * */
    void underlineInValid(int start, int end);
    /**
     * 应用贯穿线
     * @param start 起点
     * @param end 终点
     * */
    void strikethroughValid(int start, int end);
    /**
     * 取消贯穿线
     * @param start 起点
     * @param end 终点
     * */
    void strikethroughInValid(int start, int end);
    /**
     * 去除所有富文本格式
     * */
    void clearFormats();
}
