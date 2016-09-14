package com.simeiscomet.objectsearcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
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
    private final float LOOP = 80.0f;
    private final float SKIP = 4.0f;
    private final float COUNT = 3.0f;
    private final float SPAN = LOOP/COUNT;
    private final float PADDING = 0.0f;

    private SurfaceHolder _holder;

    private Display _display;

    private Handler _handler;

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

        _holder.setFormat( PixelFormat.TRANSLUCENT );
        setZOrderOnTop( true );

        // mHandlerを通じてUI Threadへ処理をキューイング
        _handler = new Handler();
        _handler.postDelayed( new Runnable() {
            public void run() {
                _doDraw();
                _frame = (++_frame)%LOOP;
                _handler.postDelayed( this, DELAY );
            }
        }, DELAY);
    }

    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
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
        Canvas canvas = _holder.lockCanvas();
        if( canvas != null ){
            Paint paint = new Paint();
            paint.setColor( Color.argb( 200, 0, 255, 0 ) );
            paint.setStyle( Paint.Style.STROKE );
            canvas.drawColor( Color.argb( 150, 0, 255, 0 ), PorterDuff.Mode.SRC_IN );

            if( _flag ){
                DecelerateInterpolator intpr = new DecelerateInterpolator();
                for( int i=0; i<COUNT; ++i ){
                    float f = (_frame+SPAN*i)%LOOP/LOOP;
                    float padding = canvas.getWidth() * intpr.getInterpolation( f ) / 5.0f + PADDING;
                    canvas.drawRect(padding, padding, canvas.getWidth() - padding, canvas.getHeight() - padding, paint);
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
