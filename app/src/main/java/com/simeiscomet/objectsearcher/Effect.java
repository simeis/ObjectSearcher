package com.simeiscomet.objectsearcher;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by NKJ on 2016/09/09.
 */
public class Effect extends SurfaceView implements SurfaceHolder.Callback
{
    private final int DELAY = 10;
    private final float LOOP = 100.0f;
    private final float SKIP = 4.0f;
    private final float COUNT = 3.0f;
    private final float SPAN = LOOP/COUNT;
    private final float PADDING = 0.0f;

    private SurfaceHolder _holder;

    private Display _display;

    private Handler _handler;

    private Rect _effectRect;

    private float _frame;

    private boolean _flag = true;

    public Effect( Context context )
    {
        super( context );
        _holder = getHolder();
        _holder.addCallback( this );
        //holder.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS );

        WindowManager winMan = ( WindowManager)context.getSystemService( Context.WINDOW_SERVICE );
        _display = winMan.getDefaultDisplay();

        Canvas canvas = _holder.lockCanvas();
        if( canvas != null ){
            _effectRect = new Rect( 0, 0, canvas.getWidth(), canvas.getWidth()*3/4 );
            _holder.unlockCanvasAndPost( canvas );
        } else {
            Rect winRect = new Rect();
            winMan.getDefaultDisplay().getRectSize( winRect );
            _effectRect = new Rect( 0, 0, winRect.width(), winRect.width()*3/4 );
        }
        _holder.setFormat( PixelFormat.TRANSLUCENT );

        setZOrderOnTop(true);
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
        // mHandlerを通じてUI Threadへ処理をキューイング
        _handler = new Handler();
        _handler.postDelayed( new Runnable() {
            public void run() {
                _doDraw();
                _frame = (++_frame)%LOOP;
                _handler.postDelayed( this, DELAY );
            }
        }, DELAY);
        /*try {
        } catch( IOException e ) {
        }*/
    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int f, int w, int h )
    {
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        _handler.removeCallbacksAndMessages( null );
    }

    private void _doDraw()
    {
        final MaskFilter blur = new BlurMaskFilter( 2, BlurMaskFilter.Blur.NORMAL );

        Canvas canvas = _holder.lockCanvas();
        if( canvas != null ){
            if( _flag ){
                final Paint paintGlow = new Paint();
                paintGlow.setARGB( 50, 0, 255, 150 );
                paintGlow.setStyle( Paint.Style.STROKE );
                paintGlow.setStrokeWidth( 8 );
                //paintGlow.setMaskFilter( blur );

                final Paint paint = new Paint();
                paint.setARGB( 170, 255, 255, 255 );
                paint.setStyle( Paint.Style.STROKE );
                paint.setStrokeWidth( 2 );

                canvas.drawColor( Color.argb( 200, 255, 255, 255 ), PorterDuff.Mode.DST_IN );

                DecelerateInterpolator intpr = new DecelerateInterpolator();
                for( int i=0; i<COUNT; ++i ){
                    float f = (_frame+SPAN*i)%LOOP/LOOP;
                    float padding = _effectRect.width() * intpr.getInterpolation( f ) * 3.0f / 16.0f + PADDING;
                    canvas.drawRect( padding, padding-(_effectRect.height()-canvas.getHeight()), _effectRect.width() - padding, _effectRect.height() - padding, paintGlow );
                    canvas.drawRect( padding, padding-(_effectRect.height()-canvas.getHeight()), _effectRect.width() - padding, _effectRect.height() - padding, paint );
                }
            }

            _holder.unlockCanvasAndPost( canvas );
        }
    }

    private void _clearCanvas()
    {
        Canvas canvas = _holder.lockCanvas();
        canvas.drawColor( Color.TRANSPARENT, PorterDuff.Mode.CLEAR );
        // 描画処理...
        _holder.unlockCanvasAndPost( canvas );
    }

    public void toggle()
    {
        _flag = !_flag;
    }
}
