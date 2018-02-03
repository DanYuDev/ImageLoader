package com.example.coderlt.bitmaploaderapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * Created by coderlt on 2018/2/2.
 */

public class BitmapUtil {
    private static final String TAG = "BitmapUtil";

    public static Bitmap decodeSampleBitmapFromResource(
            Resources resource, int resId, int reqWidth, int reqHeight)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();

        // 测试以得到合适的 inSampleSize
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resource,resId,options);
        options.inSampleSize = caculateInSampleSize(options,reqWidth,reqHeight);

        // 真实加载
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resource,resId,options);
    }

    public static int caculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
        int inSampleSize=2;

        int width = options.outWidth;
        int height = options.outHeight;

        Log.d(TAG,"Origin Bitmap width and height :"+width+","+height);

        float xRate = width/reqWidth;
        float yRate = height/reqHeight;

        float rate = Math.min(xRate,yRate);

        while(inSampleSize<rate){
            inSampleSize*=2;
        }

        Log.d(TAG,"inSampleSize is : "+inSampleSize/2);
        return inSampleSize/2;
    }

    public static Bitmap decodeSampleBitmapFromFileDescriptor(FileDescriptor fd,
                                                       int reqWidth,int reqHeight)
    {
        // first decode with inJsutDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // outPadding 参数是什么鬼？我也很绝望啊
        BitmapFactory.decodeFileDescriptor(fd,null,options);
        // caculate inSampleSize
        options.inSampleSize = caculateInSampleSize(options,reqWidth,reqHeight);
        // decode bitmap with inSampleSize setted.
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd,null,options);
    }
}
