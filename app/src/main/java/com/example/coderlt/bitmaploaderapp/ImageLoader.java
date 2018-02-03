package com.example.coderlt.bitmaploaderapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import com.jakewharton.disklrucache.DiskLruCache;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by coderlt on 2018/2/3.
 * 充分学习任主席的 ImageLoader 之后实现的
 * 参见 Android 开发艺术探索 Chapter 12 Bitmap的加载和 Cache
 */

public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static final long DISK_CACHE_SIZE = 1024*1024*50;
    private static final int DISK_CACHE_INDEX = 0;
    private static final int IO_BUFFER_SIZE = 1024;
    private boolean mIsDiskLruCacheCreated = false;
    private LruCache<String,Bitmap> mMemoryCache;
    private DiskLruCache mDiskCache;
    private Context mContext;

    private static ImageLoader instance = new ImageLoader(MyApplication.getContext());

    // 采用单例模式
    public static ImageLoader getInstance(){
        return instance;
    }

    private ImageLoader(Context context){
        mContext = context;
        // 当前可用空间 MB
        int maxMemory = (int)(Runtime.getRuntime().maxMemory()/1024);
        int cacheSize = maxMemory /8;

        mMemoryCache = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key,Bitmap bitmap){
                return bitmap.getRowBytes()*bitmap.getHeight()/1024;
            }
        };

        File diskCacheDir = FileUtil.getDiskCacheDir(mContext,"bitmap");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        if(getUsableSpace(diskCacheDir)>DISK_CACHE_SIZE){
            try{
                mDiskCache = DiskLruCache.open(diskCacheDir,1,1,
                        DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 加载图片（先后从内存、磁盘和网络顺序加载）
     * @param uri   图片的定位符
     * @param reqWidth  目标宽度
     * @param reqHeight 目标高度
     * @return
     */
    public Bitmap loadBitmap(String uri,int reqWidth,int reqHeight){
        Bitmap result = null;

        try {
            // 先从内存加载
            result = loadBitmapFromMemoryCache(uri);
            if (result != null) {
                return result;
            }

            result = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
            if (result != null) {
                return result;
            }

            result = loadBitmapFromHttp(uri,reqWidth,reqHeight);
            //  创建缓存失败 ，放弃缓存策略，并且从网络上下载该 Bitmap(不做缓存)
            if(result == null && !mIsDiskLruCacheCreated){
                result = downloadBitmapFromUrl(uri);
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        // result maybe null
        return result;
    }

    /**
     * 由于是内存加载，故不需要宽高参数，因为内存缓存是已经压缩的了
     * @param uri
     * @return
     */
    private Bitmap loadBitmapFromMemoryCache(String uri){
        Bitmap bitmap = mMemoryCache.get(FileUtil.hashKeyFromUrl(uri));
        return bitmap;
    }

    /**
     * 不建议在 UI Thread 中加载图片，属于耗时操作
     * 每次加载 Bitmap 都会往内存 MemoryCache 添加一份缓存(如果MemoryCache 中不存在的话)
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmapFromDiskCache(String uri,int reqWidth,int reqHeight)throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper() ) {
            Log.w(TAG,"load bitmap from UI Thread,that's not recommended!");
        }

        if(mDiskCache == null){
            return null;
        }

        Bitmap bitmap = null;
        String key = FileUtil.hashKeyFromUrl(uri);
        DiskLruCache.Snapshot snapshot = mDiskCache.get(key);

        if (snapshot!=null){
            FileInputStream fileInputStream = (FileInputStream)snapshot
                    .getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = BitmapUtil.decodeSampleBitmapFromFileDescriptor(fileDescriptor,reqWidth,reqHeight);

            if(bitmap!=null){
                addBitmapToMemoryCache(key,bitmap);
            }
        }

        return bitmap;
    }

    private void addBitmapToMemoryCache(String key,Bitmap bitmap){
        // 首先判断内存缓存中是否有这个对象
        if (mMemoryCache.get(key) == null){
            mMemoryCache.put(key,bitmap);
        }
    }

    /**
     * Cannot vist net in UI Thread
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmapFromHttp(String uri,int reqWidth,int reqHeight) throws IOException{
        if(Looper.myLooper()== Looper.getMainLooper()){
            throw new RuntimeException("Cannot vist network from UI Thread.");
        }
        if(mDiskCache==null){
            return null;
        }

        // 从网络拉取图片到 磁盘缓存中，而不需要内存缓存中，
        // 因为最后调用 loadFromDisk() 会间接调用 MemoryCache 的添加操作
        // 保证了MemoryCache 添加的唯一途径，程序逻辑很流畅
        String key = FileUtil.hashKeyFromUrl(uri);
        DiskLruCache.Editor editor = mDiskCache.edit(key);
        if(editor!=null){
            OutputStream os = editor.newOutputStream(DISK_CACHE_INDEX);
            if(FileUtil.downloadUrlToStream(uri,os)){
                editor.commit();
            }else{
                editor.abort();
            }
            // TODO 理解这个 flush.
            mDiskCache.flush();
        }
        return loadBitmapFromDiskCache(uri,reqWidth,reqHeight);
    }

    /**
     * 放弃缓存策略之后，独立从网络加载图片到内存
     * 不涉及到磁盘缓存和内存缓存
     * @param uri
     * @return
     */
    private Bitmap downloadBitmapFromUrl(String uri){
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;

        try{
            URL url = new URL(uri);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(),IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            if(urlConnection!=null){
                urlConnection.disconnect();
            }
            FileUtil.close(in);
        }

        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File path){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize()*(long)stats.getAvailableBlocks();
    }
}
