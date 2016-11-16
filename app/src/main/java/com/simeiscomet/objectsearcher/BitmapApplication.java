package com.simeiscomet.objectsearcher;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Created by NKJ on 2016/10/19.
 */
public class BitmapApplication extends Application {
    private final String TAG = "APPLICATION";
    private Bitmap obj;

    @Override
    public void onCreate()
    {
        //Application作成時
        Log.v(TAG,"--- onCreate() in ---");
    }

    @Override
    public void onTerminate()
    {
        //Application終了時
        Log.v(TAG, "--- onTerminate() in ---");
    }

    public void setObj(Bitmap bmp)
    {
        obj = bmp;
    }

    public Bitmap getObj()
    {
        return obj;
    }

    public void clearObj()
    {
        obj = null;
    }

}
