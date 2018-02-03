package com.example.coderlt.bitmaploaderapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private static final int SET_BITMAP=0x110;
    private Bitmap bitmap;
    private ImageView batIv;
    private int reqWidth,reqHeight;
    private MyHandler myHandler = new MyHandler(this);

    static class MyHandler extends Handler{
        private WeakReference<MainActivity> wr;
        public MyHandler(MainActivity activity){
            wr = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg){
            MainActivity act = wr.get();
            switch (msg.what){
                case SET_BITMAP:
                    act.batIv.setImageBitmap(act.bitmap);
                    break;
                default:
                    break;
            }
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        batIv = findViewById(R.id.test_iv);
        //batIv.setImageResource(R.drawable.batman);
        batIv.post(new Runnable() {
            @Override
            public void run() {
                reqWidth = batIv.getWidth();
                reqHeight = batIv.getHeight();
                Log.d(TAG,"ImageView width and height : "+reqWidth+","+reqHeight);
                bitmap = BitmapUtil.decodeSampleBitmapFromResource(getResources(),
                        R.drawable.test_3,reqWidth,reqHeight);

                Message msg = new Message();
                msg.what = SET_BITMAP;
                myHandler.sendMessage(msg);
            }
        });
    }
}
