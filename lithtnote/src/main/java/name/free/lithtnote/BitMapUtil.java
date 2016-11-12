package name.free.lithtnote;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;


/**
 * Created by mi on 16-11-12.
 * <p>检测BitMap是否太大了，如果太大，则缩放到最大尺寸</p>
 */
public class BitMapUtil {
    /**
     *检查是否超过了最大的尺寸限制， 小米note的限制是max=4096x4096。
     * */
    public static Bitmap getScaledBitMap(Uri fileUri,int maxWith,int maxHeight){
        BitmapFactory.Options options=new BitmapFactory.Options();
        Bitmap bitmap=BitmapFactory.decodeFile(fileUri.getEncodedPath(),options);
        if (options.outWidth<maxWith&&options.outHeight<maxHeight){
            return bitmap;
        }
        if (options.outWidth>maxWith&&options.outHeight>maxHeight){//长宽都超过限制
            if (options.outWidth>options.outHeight){//宽度大于长度
                options.outHeight=maxWith*options.outHeight/options.outWidth;
                options.outWidth=maxWith;
            }else {
                options.outWidth=maxHeight*options.outWidth/options.outHeight;
                options.outHeight=maxHeight;
            }

        }else if (options.outWidth>maxWith){//宽度超过限制
            options.outHeight=maxWith*options.outHeight/options.outWidth;
            options.outWidth=maxWith;

        }else if (options.outHeight>maxHeight){//长度超过限制
            options.outWidth=maxHeight*options.outWidth/options.outHeight;
            options.outHeight=maxHeight;
        }
        return Bitmap.createScaledBitmap(bitmap,options.outWidth,options.outHeight,true);
    }

}
