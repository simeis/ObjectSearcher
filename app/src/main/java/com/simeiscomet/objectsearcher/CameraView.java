package com.simeiscomet.objectsearcher;

/**
 * Created by NKJ on 2016/07/19.
 * Reference : http://boco.hp3200.com/beginner/camera01-4.html
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

public class CameraView extends SurfaceView implements Callback, Camera.PreviewCallback
{
    private Context _context;
    private SurfaceHolder _holder;

    private Camera _camera;
    private Display _display;
    private boolean _isTake = false;
    private boolean _isShot = false;

    private int _prevWidth = 1280;
    private int _prevHeight = 1280;

    private byte[] _frameBuffer;
    private int[] _rgb;
    private Bitmap _bitmap;

    public CameraView( Context context )
    {
        super( context );
        _context = context;
        _holder = getHolder();
        _holder.addCallback( this );
        //holder.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS );

        WindowManager winMan = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        _display = winMan.getDefaultDisplay();
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
        try {
            _camera = Camera.open();
            _camera.setPreviewDisplay( holder );
            _camera.setPreviewCallbackWithBuffer( this );
        } catch( IOException e ) {
        }
    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int f, int w, int h )
    {
        _camera.stopPreview();

        /*  端末の方向を調べ、それに合わせる  */
        int rotation = _display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        //Log.d("Rotation", "Rotation:"+ rotation );

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        int result;
        if ( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
            result = (info.orientation + degrees+90) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360+90) % 360;
        }

        Camera.Parameters parameters = _camera.getParameters();
        List<Size> previewSizes = parameters.getSupportedPreviewSizes();
        //parameters.setPreviewSize( previewSizes.get(0).width, previewSizes.get(0).height );
        parameters.setRotation( result );

        /*  プレビューサイズを最適化  */
        int tmpWidth = 0;
        _prevWidth = w;
        _prevHeight = h;

        //Log.d("Preview Width", "" + w );
        //Log.d("Preview Height", "" + h );

        // カメラに設定されているサポートされているサイズを一通りチェックする
        for (Size currSize : previewSizes) {
            //Log.d("Width", "" + currSize.width );
            //Log.d("Height", "" + currSize.height );

            // プレビューするサーフェイスサイズより大きいものは無視する
            if ((_prevWidth < currSize.width) || (_prevHeight < currSize.height)) {
                continue;
            }

            // プレビューサイズの中で一番大きいものを選ぶ
            if (tmpWidth < currSize.width) {
                tmpWidth = currSize.width;
                _prevWidth = currSize.width;
                _prevHeight = currSize.height;
            }
        }

        //Log.d("Selected Width", "" + _prevWidth );
        //Log.d("Selected Height", "" + _prevHeight);

        parameters.setPreviewSize(_prevWidth, _prevHeight);

        /*// 実際のプレビュー画面への拡大率を設定する
        float wScale = w / prevWidth;
        float hScale = h / prevHeight;

        // 画面内に収まらないといけないから拡大率は幅と高さで小さい方を採用する
        float prevScale = wScale < hScale ? wScale : hScale;

        // SurfaceViewのサイズをセットする
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.width = (int)(prevWidth * prevScale);
        layoutParams.height = (int)(prevHeight * prevScale);*/

        try {
            _camera.setParameters(parameters);
        } catch( Exception e ){

        }
        _camera.setDisplayOrientation(result);

        //parameters.setPreviewFormat( ImageFormat.FLEX_RGB_888 );
        parameters.setPreviewFormat( ImageFormat.NV21 );

        int size = _prevWidth * _prevHeight * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        _frameBuffer = new byte[size];

        _rgb = new int[ _prevWidth * _prevHeight ];

        _bitmap = Bitmap.createBitmap( _prevWidth, _prevHeight, Bitmap.Config.ARGB_8888 );

        _isShot = false;

        _camera.addCallbackBuffer( _frameBuffer );
        _camera.startPreview();
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        _camera.stopPreview();
        _camera.setPreviewCallback(null);
        _camera.release();
        _camera = null;
    }

    @Override
    public void onPreviewFrame( byte[] data, Camera camera )
    {
        /*// 画像変換処理
        decodeYUV420SP( _rgb, _frameBuffer, _prevWidth, _prevHeight);
        _bitmap.setPixels( _rgb, 0, _prevWidth, 0, 0, _prevWidth, _prevHeight);

        // 描画処理
        Canvas canvas = _holder.lockCanvas();
        if( canvas != null ){
            canvas.drawBitmap( _bitmap, 0, 0, null );
            _holder.unlockCanvasAndPost( canvas );
        }*/
        // バッファに追加することで、次のフレームを待ち受けます
        _camera.addCallbackBuffer( _frameBuffer );
    }

    public boolean shotImage( View v, MotionEvent event )
    {
        if( _isTake == true ){
            return true;
        }
        if( _isShot == true ){
            _camera.startPreview();
            _isShot = false;
            return true;
        }

        if( event.getAction() == MotionEvent.ACTION_DOWN ){
            // 撮影中の2度押し禁止用フラグ
            _isTake = true;

            _camera.startPreview();

            // オートフォーカス
            _camera.cancelAutoFocus();
            _camera.autoFocus( _mAutoFocusListener );
        }
        return true;
    }

    public boolean isShot()
    {
        return _isShot;
    }

    public int getPrevWidth() { return _prevWidth; }
    public int getPrevHeight() { return _prevHeight; }

    public int getRotate() { return _display.getRotation(); }

    public Bitmap getBitmap() {
        decodeYUV420SP(_rgb, _frameBuffer, _prevWidth, _prevHeight);
        _bitmap.setPixels(_rgb, 0, _prevWidth, 0, 0, _prevWidth, _prevHeight);

        return _bitmap;
    }

    static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }


    /**
     * オートフォーカス完了のコールバック
     */
    private Camera.AutoFocusCallback _mAutoFocusListener = new Camera.AutoFocusCallback()
    {
        public void onAutoFocus( boolean success, Camera camera ) {
            // 撮影
            //mCam.takePicture(null, null, mPicJpgListener);
            if( success == true ){
                _camera.stopPreview();
                _isShot = true;
                //Log.i( "shot", "complate" );
            }
            _isTake = false;
        }
    };
}
