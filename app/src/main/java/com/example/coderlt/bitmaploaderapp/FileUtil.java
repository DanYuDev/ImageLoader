package com.example.coderlt.bitmaploaderapp;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by coderlt on 2018/2/3.
 */

public class FileUtil {
    public static final int IO_BUFFER_SIZE = 1024;

    private static final String TAG = "CacheUtil";

    public static File getDiskCacheDir(Context context, String fileName){
        // 首先查看外部存储状态
        boolean externalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if(externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        }else{
            // 如果外部存储不存在，那么就
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath+File.separator+fileName);
    }

    /**
     * 获取到 key 之后，就可以得到对应的 Editor 对象
     * @param url imageUrl
     * @return key for LruCache
     */
    public static String hashKeyFromUrl(String url){
        String  cacheKey;
        try{
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(url.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        }catch(NoSuchAlgorithmException e){
            // 备选方案
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    public static String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i<bytes.length; i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static boolean downloadUrlToStream(String urlString, OutputStream outputStream){
        HttpURLConnection connection =null;
        URL  url;
        BufferedInputStream in = null;
        BufferedOutputStream out=null;

        try{
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(),IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

            int b;
            while( (b=in.read())!=-1 ){
                out.write(b);
            }

            return true;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(connection!=null){
                connection.disconnect();
            }
            close(out);
            close(in);
        }

        return false;
    }

    /**
     * 避免了频繁地 try/catch,简化代码
     * @param closeable
     * @return
     */
    public static boolean close(Closeable closeable){
        if(closeable==null){
            return false;
        }
        try{
            closeable.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }
}
