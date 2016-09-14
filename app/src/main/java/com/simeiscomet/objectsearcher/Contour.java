package com.simeiscomet.objectsearcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by NKJ on 2016/07/27.
 */
public class Contour extends SurfaceView implements SurfaceHolder.Callback
{
    private final int MAX_CLUSTER = 3;
    private final double MIN_DISTANCE = 10.0;
    private final double MAX_DISTANCE = 120.0;

    private enum Mode{
        WAITING,
        DRAGGING,
        FINISHED,
        SELECTED
    }

    Mode _mode = Mode.WAITING;

    private SurfaceHolder _holder;
    
    private Display _display;

    private CameraView _camView;
    
    private ArrayList<PointF> _points = new ArrayList<>();
    private boolean _isTouch = false;
    
    private PointF _center = new PointF();
    private double _angle = 0.0;
    private PointF[] _cornerPos = new PointF[4];
    
    private ArrayList<Cluster> _midPoints = new ArrayList<>();
    private PointF[] _weightPoints = new PointF[MAX_CLUSTER];
    private ArrayList<Point> _lineSegment = new ArrayList<>();

    private PointF _ratio = new PointF();

    private Bitmap _object = null;
    
    public Contour( Context context, CameraView camView )
    {
        super( context );
        _holder = getHolder();
        _holder.addCallback( this );
        //holder.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS );

        WindowManager winMan = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        _display = winMan.getDefaultDisplay();

        _holder.setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);

        _camView = camView;
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
        _points.clear();
        _clearCanvas();

        _mode = Mode.WAITING;
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
    }

    public boolean drawContour( View v, MotionEvent event )
    {
        if( event.getPointerCount() >= 2 ){
            _points.clear();
            _clearCanvas();
            _mode = Mode.WAITING;
            return false;
        }

        PointF eventPos = new PointF( (int)event.getX(), (int)event.getY() );

        PointF[] vec = new PointF[2];
        double innerProduct = 0.0;
        if( _points.size() >= 2 ){
            vec[0] = new PointF( _points.get( _points.size() - 1 ).x - _points.get( _points.size() - 2 ).x,
                                  _points.get( _points.size() - 1 ).y - _points.get( _points.size() - 2 ).y );
            vec[1] = new PointF( eventPos.x - _points.get( _points.size() - 1 ).x,
                                  eventPos.y - _points.get( _points.size() - 1 ).y );
            _vectorNormalization( vec[0] );
            _vectorNormalization( vec[1] );

            innerProduct = _innerProduct( vec[0], vec[1] );
            //Log.d("InnerProduct", ""+ innerProduct );
        }

        switch( event.getAction() ) {
        case MotionEvent.ACTION_DOWN:
            _points.clear();
            _clearCanvas();
            _points.add(eventPos);

            _mode = Mode.DRAGGING;
            break;

        case MotionEvent.ACTION_MOVE:
            if( _mode == Mode.WAITING ) {
                _points.add(eventPos);
            } else if( _mode == Mode.DRAGGING ){
                if( _points.size() < 1 ){
                    _points.add( eventPos );
                } else if( _points.size() < 2 ){
                    if( _calculateDistance( _points.get( _points.size() - 1 ), eventPos ) < ( MIN_DISTANCE + MAX_DISTANCE )/2.0 ){
                        break;
                    }
                    _points.add(eventPos);
                } else {
                    if( _calculateDistance( _points.get( _points.size() - 1 ), eventPos ) < MAX_DISTANCE * Math.max( ( innerProduct - 0.9 ) * 10.0, 0.0 ) + MIN_DISTANCE ){
                        break;
                    }

                    // 新しい線分が今までの線分と交差しているか
                    PointF newPoint = eventPos;
                    for( int i=1; i<_points.size(); ++i ){
                        //++iterator;
                        PointF a = _points.get( i );
                        PointF b = _points.get( i-1 );

                        // 交差している
                        if( _intersectionDetermination( a, b, _points.get(_points.size() - 1 ), eventPos ) ){
                            while( i != 0 ){
                                --i;
                                _points.remove(i);
                            }
                            PointF c = _points.get(1);
                            PointF d = _points.get(0);

                            newPoint = _intersectionCoordinate(c, d, _points.get(_points.size() - 1), eventPos);

                            // モード変更
                            _mode = Mode.FINISHED;

                            break;
                        }
                    }

                    // 座標追加
                    _points.add( newPoint );

                }
            }
            break;

        case MotionEvent.ACTION_UP:
            if( _mode == Mode.DRAGGING ){
                // 座標追加
                _points.add(eventPos);

                // 始点から離れすぎている
                double distance = _calculateDistance( _points.get(0), eventPos );
                if( distance > ( MIN_DISTANCE + MAX_DISTANCE ) ){
                    int pointNum = (int)( distance/( ( MIN_DISTANCE + MAX_DISTANCE )/2.0 ) );
                    for( int i=1; i<pointNum; ++i ){
                        double ratio = (double)i/(double)pointNum;
                        _points.add( new PointF( (float)( _points.get( 0 ).x * ratio + eventPos.x * ( 1.0 - ratio ) ), (float)( _points.get( 0 ).y * ratio + eventPos.y * ( 1.0 - ratio ) ) ) );
                    }
                }

                // 平面を作れる
                if( _points.size() >= 3 ){
                    // モード変更
                    _mode = Mode.FINISHED;
                }
                // 平面を作れない
                else{
                    // 座標初期化
                    _points.clear();

                    // モード変更
                    _mode = Mode.WAITING;
                }
                break;
            }
        }

       // Log.d("TouchEvent", "ID:" + event.getPointerCount() + ",X(0):" + event.getX(0) + ",Y(0):" + event.getY(0) );
        //Log.d("Pressure", "Pressure:"+ event.getPressure() );

        if( _mode == Mode.FINISHED ){
            _objectExtraction();
        }

        _doDraw();
        return true;
    }

    private void _doDraw()
    {
        Canvas canvas = _holder.lockCanvas();
        if( canvas != null ){
            Paint paint = new Paint();
            paint.setColor( Color.MAGENTA );
            canvas.drawColor( Color.TRANSPARENT, PorterDuff.Mode.CLEAR );
            if( _points.size() >= 2 ){
                for( int i=0; i<_points.size()-1; ++i ){
                    canvas.drawLine( _points.get( i ).x, _points.get( i ).y, _points.get( i + 1 ).x, _points.get( i + 1 ).y, paint );
                }
                if( _mode == Mode.FINISHED || _mode == Mode.SELECTED ){
                    canvas.drawLine( _points.get( _points.size() - 1 ).x, _points.get( _points.size() - 1 ).y, _points.get( 0 ).x, _points.get( 0 ).y, paint );
                }
            }

            for( final PointF pos : _points ) {
                canvas.drawCircle( pos.x, pos.y, 10, paint );
            }

            if( _mode == Mode.FINISHED || _mode == Mode.SELECTED ){
                paint.setColor( Color.WHITE );
                for( int i=0; i<3; ++i ){
                    canvas.drawLine( _cornerPos[i].x, _cornerPos[i].y, _cornerPos[i+1].x, _cornerPos[i+1].y, paint );
                }
                canvas.drawLine( _cornerPos[3].x, _cornerPos[3].y, _cornerPos[0].x, _cornerPos[0].y, paint );

                for( int i=0; i<_lineSegment.size(); ++i ){
                    canvas.drawLine( _weightPoints[_lineSegment.get(i).x].x, _weightPoints[_lineSegment.get(i).x].y, _weightPoints[_lineSegment.get(i).y].x, _weightPoints[_lineSegment.get(i).y].y, paint );
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


    private void _objectExtraction()
    {
        //Log.d("prev", "w"+ _camView.getPrevWidth());
        //Log.d("prev", "h"+ _camView.getPrevHeight());
        //Log.d("view", "w"+ _camView.getWidth());
        //Log.d("view", "h"+ _camView.getHeight());

        float prevLong = Math.max(_camView.getPrevWidth(), _camView.getPrevHeight());
        float prevShort = Math.min(_camView.getPrevWidth(), _camView.getPrevHeight());

        switch( _camView.getRotate() ){
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                _ratio = new PointF( prevShort / _camView.getWidth(),
                                       prevLong / _camView.getHeight() );
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                _ratio = new PointF( prevLong / _camView.getWidth(),
                                       prevShort / _camView.getHeight() );
                break;
        }

        PointF tmp;
        Matrix matrix = new Matrix();
        Bitmap bitmap;

        switch( _camView.getRotate() ){
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            for( final PointF pos : _points ){
                tmp = new PointF( pos.x, pos.y );
                pos.x = tmp.y * _ratio.y;
                pos.y = tmp.x * _ratio.x;
            }
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            for( final PointF pos : _points ){
                tmp = new PointF( pos.x, pos.y );
                pos.x = tmp.x * _ratio.x;
                pos.y = tmp.y * _ratio.y;
            }
            break;
        }

        switch( _camView.getRotate() ){
        case Surface.ROTATION_0:
            matrix.preScale( 1, -1 );
            break;
        case Surface.ROTATION_90:
            matrix.preScale( 1, 1 );
            break;
        case Surface.ROTATION_180:
            matrix.preScale( -1, 1 );
            break;
        case Surface.ROTATION_270:
            matrix.preScale( -1, -1 );
            break;
        }

        bitmap = Bitmap.createBitmap( _camView.getBitmap(), 0, 0, _camView.getPrevWidth(), _camView.getPrevHeight(), matrix, false );

        // 輪郭移動
        _circumscribedQuadrangle(0.1);
        _fitTheContour(bitmap);
        _circumscribedQuadrangle(0.1);

        try {
            _object = _projectiveTransformation(bitmap);
        } catch( Exception e ) {
            Log.e("P.T. Error", e.getMessage() );
        }

        switch( _camView.getRotate() ){
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            for( final PointF pos : _points ){
                tmp = new PointF( pos.x, pos.y );
                pos.x = tmp.y / _ratio.y;
                pos.y = tmp.x / _ratio.x;
            }
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            for( final PointF pos : _points ){
                tmp = new PointF( pos.x, pos.y );
                pos.x = tmp.x / _ratio.x;
                pos.y = tmp.y / _ratio.y;
            }
            break;
        }

        switch( _camView.getRotate() ){
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            for( int i=0; i<4; ++i ){
                tmp = new PointF( _cornerPos[i].x, _cornerPos[i].y );
                _cornerPos[i].x = tmp.y / _ratio.y;
                _cornerPos[i].y = tmp.x / _ratio.x;
            }
            for( int i=0; i<MAX_CLUSTER; ++i ){
                tmp = new PointF( _weightPoints[i].x, _weightPoints[i].y );
                _weightPoints[i].x = tmp.y / _ratio.y;
                _weightPoints[i].y = tmp.x / _ratio.x;
            }
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            for( int i=0; i<4; ++i ){
                tmp = new PointF( _cornerPos[i].x, _cornerPos[i].y );
                _cornerPos[i].x = tmp.x / _ratio.x;
                _cornerPos[i].y = tmp.y / _ratio.y;
            }
            for( int i=0; i<MAX_CLUSTER; ++i ){
                tmp = new PointF( _weightPoints[i].x, _weightPoints[i].y );
                _weightPoints[i].x = tmp.x / _ratio.x;
                _weightPoints[i].y = tmp.y / _ratio.y;
            }
            break;
        }

        _mode = Mode.SELECTED;
    }


    private Bitmap _projectiveTransformation( Bitmap image )
    {
        /*  変数宣言  */
        // 画像の幅
        Rect rect = new Rect( 0, 0, (int)_calculateDistance( _cornerPos[0], _cornerPos[1] ), (int)_calculateDistance( _cornerPos[0], _cornerPos[3] ) );

        if( rect.width() <= 0 || rect.height() <= 0 )
            throw new RuntimeException("Contour size is minimum.");

        /*  行列作成  */
        // 行列A
        double[][] matA = { { 0,             0,              1,  0,             0,              0,  0,                                0                                },
                             { 0,             0,              0,  0,             0,              1,  0,                                0                                },

                             { rect.width(),  0,              1,  0,             0,              0,  -rect.width()*_cornerPos[1].x,  0                                },
                             { 0,             0,              0,  rect.width(),  0,              1,  -rect.width()*_cornerPos[1].y,  0                                },

                             { rect.width(),  rect.height(),  1,  0,             0,              0,  -rect.width()*_cornerPos[2].x,  -rect.height()*_cornerPos[2].x },
                             { 0,             0,              0,  rect.width(),  rect.height(),  1,  -rect.width()*_cornerPos[2].y,  -rect.height()*_cornerPos[2].y },

                             { 0,             rect.height(),  1,  0,             0,              0,  0,                                -rect.height()*_cornerPos[3].x },
                             { 0,             0,              0,  0,             rect.height(),  1,  0,                                -rect.height()*_cornerPos[3].y } };

        // 列ベクトルB
        double[][] matB = { { _cornerPos[0].x }, { _cornerPos[0].y },
                             { _cornerPos[1].x }, { _cornerPos[1].y },
                             { _cornerPos[2].x }, { _cornerPos[2].y },
                             { _cornerPos[3].x }, { _cornerPos[3].y } };

        /*  計算開始  */
        // 逆行列
        Matrix2D invA = Matrix2D.inv( new Matrix2D( matA ) );

        // 積
        Matrix2D matX = Matrix2D.mult( invA, new Matrix2D( matB ) );

        double[][] dMatX = matX.getArrays();

        // 画像生成
        Bitmap object;
        object = Bitmap.createBitmap( rect.width(), rect.height(), Bitmap.Config.ARGB_8888 );//cvCreateImage( cvSize( rect.width, rect.height ), image.storedImage.depth, image.storedImage.nChannels );


        // 画像情報
        //int iStep = image.storedImage.widthStep;
        //int oStep = object.storedImage.widthStep;
        //int channel = object.storedImage.nChannels;

        // バイキュービック（http://www7a.biglobe.ne.jp/~fairytale/article/program/graphics.html）
        double a = -1.0;

        double parameter[] = { a + 2.0, a + 3.0, a * 5.0, a * 8.0, a * 4.0 };

        Rect preSize = new Rect( 0, 0, rect.width()+3, rect.height()+3 );
        Bitmap preImage = Bitmap.createBitmap(preSize.width(), preSize.height(), Bitmap.Config.ARGB_8888);

        // 変換画像上をラスタ走査
        for( int i=-1; i<rect.height()+2; ++i ) {
            for( int j=-1; j<rect.width()+2; ++j ) {
                // 変換前の座標を算出
                PointF pos = new PointF( (float)(((double)j*dMatX[0][0] + (double)i*dMatX[1][0] + dMatX[2][0] ) / ( (double)j*dMatX[3][0] + (double)i*dMatX[7][0] + 1.0 )),
                                         (float)(((double)j*dMatX[3][0] + (double)i*dMatX[4][0] + dMatX[5][0] ) / ( (double)j*dMatX[3][0] + (double)i*dMatX[7][0] + 1.0 )) );

                Point base = new Point( (int)pos.x, (int)pos.y );

                if( 1.0 <= pos.x && pos.x <= image.getWidth() - 2.0 && 1.0 <= pos.y && pos.y <= image.getHeight() - 2.0 ){
                    // RGB毎に変数を用意しておく
                    int insertR = 0;
                    int insertG = 0;
                    int insertB = 0;

                    // 基点の周辺16画素を取得して処理
                    for (int x=-1; x<=2; x++) {
                        for (int y=-1; y<=2; y++) {
                            // 実際に処理する画素を設定
                            Point current = new Point( base.x + x, base.y + y );

                            // 距離決定
                            PointF dist = new PointF( Math.abs(current.x - pos.x), Math.abs(current.y - pos.y) );

                            // 重み付け
                            double weight = 0.0;  // 重み変数

                            // まずはX座標の距離で重みを設定
                            // 1以下、2以下のとき、それぞれ重み付け
                            if( dist.x <= 1.0 ){
                                weight = parameter[0] * dist.x*dist.x*dist.x - parameter[1] * dist.x*dist.x + 1.0;
                            }
                            else if( dist.x <= 2.0 ){
                                weight = a*dist.x*dist.x*dist.x - parameter[2] * dist.x*dist.x + parameter[3] * dist.x - parameter[4];
                            }
                            else {
                                continue;  // 何も処理しないので、次のループへ
                            }

                            // Y座標の距離で重みを設定
                            if( dist.y <= 1.0 ){
                                weight *= parameter[0] * dist.y*dist.y*dist.y - parameter[1] * dist.y*dist.y + 1.0;
                            }
                            else if( dist.y <= 2.0 ){
                                weight *= a*dist.y*dist.y*dist.y - parameter[2] * dist.y*dist.y + parameter[3] * dist.y - parameter[4];
                            }
                            else {
                                continue;
                            }

                            // 実際に画素を取得
                            int colorProcess = Color.BLACK;
                            if( current.x >= 0 || current.y >= 0 || current.x <= rect.width()-1 || current.y <= rect.height()-1 ){
                                colorProcess = image.getPixel( current.x, current.y );
                            }

                            // 画素をRGB分割し、重みをかけて足し合わせる
                            insertR += ( Color.red( colorProcess ) * weight );
                            insertG += ( Color.green( colorProcess ) * weight );
                            insertB += ( Color.blue( colorProcess ) * weight );
                        }
                    }

                    if( insertR < 0 ) insertR = 0; if( insertR > 255 ) insertR = 255;
                    if( insertG < 0 ) insertG = 0; if( insertG > 255 ) insertG = 255;
                    if( insertB < 0 ) insertB = 0; if( insertB > 255 ) insertB = 255;

                    preImage.setPixel( j+1, i+1, Color.rgb( insertR, insertG, insertB ) );

                    continue;
                }

                // 格納
                preImage.setPixel( j+1, i+1, Color.BLACK );

            }
        }

        // 変換画像上をラスタ走査
        for( int i=0; i<rect.height(); ++i ) {
            for( int j=0; j<rect.width(); ++j ) {
                // 変換前の座標を算出
                PointF pos = new PointF( j+1, i+1 );

                Point base = new Point( (int)pos.x, (int)pos.y );

                if( 1.0 <= pos.x && pos.x <= preSize.width() - 2.0 && 1.0 <= pos.y && pos.y <= preSize.height() - 2.0 ){
                    // RGB毎に変数を用意しておく
                    int insertR = 0;
                    int insertG = 0;
                    int insertB = 0;

                    // 基点の周辺16画素を取得して処理
                    for (int x=-1; x<=2; x++) {
                        for (int y=-1; y<=2; y++) {
                            // 実際に処理する画素を設定
                            Point current = new Point( base.x + x, base.y + y );

                            // 距離決定
                            PointF dist = new PointF( Math.abs( current.x - pos.x ), Math.abs( current.y - pos.y ) );

                            // 重み付け
                            double weight = 0.0;  // 重み変数

                            // まずはX座標の距離で重みを設定
                            // 1以下、2以下のとき、それぞれ重み付け
                            if( dist.x <= 1.0 ){
                                weight = parameter[0] * dist.x*dist.x*dist.x - parameter[1] * dist.x*dist.x + 1.0;
                            }
                            else if( dist.x <= 2.0 ){
                                weight = a*dist.x*dist.x*dist.x - parameter[2] * dist.x*dist.x + parameter[3] * dist.x - parameter[4];
                            }
                            else {
                                continue;  // 何も処理しないので、次のループへ
                            }

                            // Y座標の距離で重みを設定
                            if( dist.y <= 1.0 ){
                                weight *= parameter[0] * dist.y*dist.y*dist.y - parameter[1] * dist.y*dist.y + 1.0;
                            }
                            else if( dist.y <= 2.0 ){
                                weight *= a*dist.y*dist.y*dist.y - parameter[2] * dist.y*dist.y + parameter[3] * dist.y - parameter[4];
                            }
                            else {
                                continue;
                            }

                            // 実際に画素を取得
                            int colorProcess = preImage.getPixel( current.x, current.y );

                            // 画素をRGB分割し、重みをかけて足し合わせる
                            insertR += ( Color.red( colorProcess ) * weight );
                            insertG += ( Color.green(colorProcess) * weight );
                            insertB += ( Color.blue(colorProcess) * weight );
                        }
                    }

                    if( insertR < 0 ) insertR = 0; if( insertR > 255 ) insertR = 255;
                    if( insertG < 0 ) insertG = 0; if( insertG > 255 ) insertG = 255;
                    if( insertB < 0 ) insertB = 0; if( insertB > 255 ) insertB = 255;

                    object.setPixel( j, i, Color.rgb( insertR, insertG, insertB ) );

                    continue;
                }

                // 格納
                object.setPixel(j, i, Color.BLACK );
            }
        }

        return object;
    }


    private void _fitTheContour( Bitmap image )
    {
        _midPoints.clear();

        int clusterNum;
        {
            double dist[] = {  _calculateDistance( _cornerPos[0], _cornerPos[1] ),
                    _calculateDistance( _cornerPos[0], _cornerPos[3] ) };
            double min = Math.min(dist[0], dist[1]);
            double max = Math.max(dist[0], dist[1]);

            clusterNum = MAX_CLUSTER;
            //if( min/max > 0.7 ) --clusterNum;		// 正方形に近ければクラスタ数を減らす
            //if( min*max < 30000 ) --clusterNum;		// 面積が小さければクラスタ数を減らす
            //std::cout << min/max << std::endl;
            //std::cout << min*max << std::endl;

        }

        // 片方の点
        {
            for( int i=0; i<_points.size(); ++i ){

                // もう片方の点
                for( int j=i+1; j<_points.size(); ++j ){
                    // 同じとき
                    /*if( i == j ){
                        ++j;
                        continue;
                    }*/

                    // 隣同士は無視
                    if( i == 0 ){
                        if( j == 1 || j == _points.size()-1 ){
                            continue;
                        }
                    } else if( i == _points.size()-1 ){
                        if( j == 0 || j == _points.size()-2 ){
                            continue;
                        }
                    } else {
                        if( j == i+1 || j == i-1 ){
                            continue;
                        }
                    }

                    // 別の輪郭と交差しているか確認
                    boolean determinate = false;

                    for( int k=0; k<_points.size(); ++k ){
                        if( k == i || k == j ){
                            continue;
                        }

                        PointF a = _points.get( i );
                        PointF b = _points.get( j );
                        PointF c = _points.get( k );
                        PointF d = _points.get( 0 );
                        if( k != _points.size()-1 ){
                            d = _points.get( k + 1 );
                        }

                        if( _intersectionDetermination( a, b, c, d ) ){
                            determinate = true;
                            break;
                        }
                    }

                    if( determinate ){
                        continue;
                    }

                    // 中点保存
                    Random rnd = new Random();
                    Cluster midPoint = new Cluster( new PointF( ( _points.get(i).x + _points.get(j).x )/2.0f+0.001f, ( _points.get(i).y + _points.get(j).y )/2.0f+0.001f ), rnd.nextInt(clusterNum) );

                    _midPoints.add( midPoint );
                }
            }
        }

        // ポリゴン外にある中点を削除
        {
            for( int i=0; i<_midPoints.size(); ++i ){
                int count = 0;

                PointF a = _midPoints.get( i ).point;
                PointF b = new PointF( _camView.getPrevWidth(), _midPoints.get( i ).point.y );

                for( int j=0; j<_points.size()-1; ++j ){
                    PointF c = _points.get( j );
                    PointF d = _points.get( j + 1 );
                    if( j == _points.size()-1 ){
                        d = _points.get( 0 );
                    }

                    if( _intersectionDetermination( a, b, c, d ) ){
                        ++count;
                    }
                }

                if( count%2 == 0 ){
                    _midPoints.remove( i-- );
                    continue;
                }

                _midPoints.get( i ).point.x -= 0.001f;
                _midPoints.get( i ).point.y -= 0.001f;
            }
        }


        _kMeans( _midPoints, _weightPoints, clusterNum );


        // 輪郭に内包される散在重心同士を結ぶ線分の組を抽出
        {
            _lineSegment.clear();

            // 全ての線分を探索
            for( int i=0; i<clusterNum; ++i ){
                for( int j=i+1; j<clusterNum; ++j ){
                    boolean determinate = false;

                    // 全ての輪郭線との交差判定
                    for( int k=0; k<_points.size(); ++k ){
                        PointF a = _weightPoints[i];
                        PointF b = _weightPoints[j];
                        PointF c = _points.get( k );
                        PointF d = _points.get( 0 );
                        if( k != _points.size()-1 ){
                            d = _points.get( k + 1 );
                        }

                        if( _intersectionDetermination( a, b, c, d ) ){
                            determinate = true;
                            break;
                        }
                    }

                    if( determinate ){
                        continue;
                    }

                    _lineSegment.add( new Point( i, j ) );
                }
            }
        }


        {
            ArrayList<Integer> background = new ArrayList<>();

            // 領域の各頂点に対して処理
            for( int k=0; k<_points.size(); ++k ){
                // 一番近い散在重心を求める
                double minDistance = _calculateDistance( _points.get(k), _weightPoints[0] );
                PointF nearestPoint = _weightPoints[0];

                for( int i=1; i<clusterNum; ++i ){
                    double distance = _calculateDistance( _points.get(k), _weightPoints[i] );
                    if( minDistance > distance ){
                        minDistance = distance;
                        nearestPoint = _weightPoints[i];
                    }
                }

                // 一番近い近似中心線を求める
                for( int j=0; j<_lineSegment.size(); ++j ){
                    double distance = _linePointDistance( _weightPoints[_lineSegment.get(j).x], _weightPoints[_lineSegment.get(j).y], _points.get(k) );
                    if( minDistance > distance ){
                        PointF pos = _perpendicular( _weightPoints[_lineSegment.get(j).x], _weightPoints[_lineSegment.get(j).y], _points.get(k) );
                        if( pos.x < 0.0 || pos.y < 0.0 ){
                            continue;
                        }
                        minDistance = distance;
                        nearestPoint = pos;
                    }
                }

                // 背景取得
                int loop = (int)Math.max( Math.abs( _points.get(k).x - nearestPoint.x ), Math.abs( _points.get(k).y - nearestPoint.y ) );
                //double maxEdge = 0.0;
                //PointF maxEdgePos;

                // 平均を取る
                Point pos;// = posInLine( nearestPoint, _points.get(k), loop );
                int r = 0, g = 0, b = 0;

                for( int i=loop; i>loop-10; --i ){
                    pos = posInLine( nearestPoint, _points.get(k), i );
                    //Log.d("GetPixPos", "x:"+ pos.x );
                    //Log.d("GetPixPos", "y:"+ pos.y );

                    int tmp = image.getPixel( pos.x, pos.y );

                    r += Color.red( tmp );
                    g += Color.green( tmp );
                    b += Color.blue( tmp );
                }

                r /= 10;
                g /= 10;
                b /= 10;

                // 背景情報追加
                background.add( Color.rgb( r, g, b ) );
            }


            // 更新されたかどうかを管理
            ArrayList<Boolean> updateFlags = new ArrayList<>();

            // 領域の各頂点に対して処理
            for( int k=0; k<_points.size(); ++k ){
                // 一番近い散在重心を求める
                double minDistance = _calculateDistance( _points.get( k ), _weightPoints[0] );
                PointF nearestPointF = _weightPoints[0];

                for( int i=1; i<clusterNum; ++i ){
                    double distance = _calculateDistance( _points.get( k ), _weightPoints[i] );
                    if( minDistance > distance ){
                        minDistance = distance;
                        nearestPointF = _weightPoints[i];
                    }
                }

                // 一番近い近似中心線を求める
                for( int j=0; j<_lineSegment.size(); ++j ){
                    double distance = _linePointDistance( _weightPoints[_lineSegment.get( j ).x], _weightPoints[_lineSegment.get( j ).y], _points.get( k ) );
                    if( minDistance > distance ){
                        PointF pos = _perpendicular( _weightPoints[_lineSegment.get( j ).x], _weightPoints[_lineSegment.get( j ).y], _points.get( k ) );
                        if( pos.x < 0.0 || pos.y < 0.0 ){
                            continue;
                        }
                        minDistance = distance;
                        nearestPointF = pos;
                    }
                }

                // 背景分離
                int loop = (int)Math.max(Math.abs( _points.get( k ).x - nearestPointF.x ), Math.abs( _points.get( k ).y - nearestPointF.y ) );
                //double maxEdge = 0.0;
                //PointF maxEdgePos;
                boolean update = false;

                for( int i=loop; i>1; --i ){
                    Point nowPos = posInLine( nearestPointF, _points.get( k ), i );
                    Point nextPos = posInLine( nearestPointF, _points.get( k ), i-1 );

                    int nowColor = image.getPixel( nowPos.x, nowPos.y );
                    int nextColor = image.getPixel( nextPos.x, nextPos.y );

                    // 背景候補と比較
                    int back = background.get( background.size()-1 );
                    int current = background.get( k );
                    int front = background.get( 0 );
                    if( k != 0 ){
                        back = background.get( k -1 );
                    }
                    if( k != background.size()-1 ){
                        front = background.get( k +1 );
                    }

                    boolean nowBackground = false;
                    boolean nextObject = false;

                    final double range = 80.0;

                    if( _colorDistance( nowColor, back ) < range ||
                        _colorDistance( nowColor, current ) < range ||
                        _colorDistance( nowColor, front ) < range ){
                        nowBackground = true;
                    }
                    if( _colorDistance( nextColor, back ) >= range ||
                        _colorDistance( nextColor, current ) >= range ||
                        _colorDistance( nextColor, front ) >= range ){
                        nextObject = true;
                    }

                    if( nowBackground && nextObject ){
                        _points.set( k, new PointF( nowPos ) );
                        update = true;
                        break;
                    }
                }

                updateFlags.add( update );
            }


            // 領域の各頂点に対して処理
            for( int i=0; i<_points.size(); ++i ){
                // 更新されなかった頂点を補正（左右の中点に移動）
                if( !updateFlags.get( i ) ){
                    int prev = i;
                    int next = i;
                    int prevMove = 0;
                    int nextMove = 0;

                    do {
                        if( prev > 0 ){
                            --prev;
                        } else {
                            prev = _points.size()-1;
                        }
                        ++prevMove;
                        if( prev == i ){
                            prevMove = 0;
                            break;
                        }
                    } while( !updateFlags.get( prev ) );

                    do {
                        if( next < _points.size()-1 ){
                            ++next;
                        } else {
                            next = 0;
                        }
                        ++nextMove;
                        if( next == i ){
                            nextMove = 0;
                            break;
                        }
                    } while( !updateFlags.get( next ) );

                    _points.set( i, new PointF( ( _points.get( prev ).x*(float)( nextMove ) + _points.get( next ).x*(float)( prevMove ) ) / (float)( prevMove + nextMove ),
                                                  ( _points.get( prev ).y*(float)( nextMove ) + _points.get( next ).y*(float)( prevMove ) ) / (float)( prevMove + nextMove ) ) );
                }
            }
        }
    }


    private Point posInLine( PointF start, PointF end, int x ){
        PointF d = new PointF();/* 二点間の距離 */
        d.x = ( end.x > start.x ) ? end.x - start.x : start.x - end.x;
        d.y = ( end.y > start.y ) ? end.y - start.y : start.y - end.y;

        PointF s = new PointF(); /* 二点の方向 */
        s.x = ( end.x > start.x ) ? 1.0f : -1.0f;
        s.y = ( end.y > start.y ) ? 1.0f : -1.0f;

        Point ret = new Point();

        if( d.x > d.y ){
            ret.x = (int)(start.x + s.x*(float)x);
            ret.y = (int)(start.y + s.y*d.y*((float)x/d.x) + 0.5);
        } else {
            ret.x = (int)(start.x + s.x*d.x*((double)x/d.y) + 0.5);
            ret.y = (int)(start.y + s.y*(double)x);
        }

        return ret;
    }


    private void _circumscribedQuadrangle( double fineness )
    {
        /*  変数宣言  */
        double minFerreDiameter = Math.sqrt( (double)( _camView.getPrevWidth()*_camView.getPrevWidth() + _camView.getPrevHeight()*_camView.getPrevHeight() ) );
        double minAngle = -1.0;
        double minUpperDistance = 0.0, minLowerDistance = 0.0;
        double maxUpperDistance, maxLowerDistance;

        /*  処理開始  */
        // 中点算出
        _center = new PointF( 0.0f, 0.0f );
    
        for( final PointF p : _points ){
            _center.x += p.x;
            _center.y += p.y;
        }
    
        _center.x /= (float)_points.size();
        _center.y /= (float)_points.size();
    
        // 0°~180°まで指定された角度の細かさでループ
        for( double angle=0.0; angle<180.0; angle += fineness ){
            // 変数宣言
            double upper = 0.0;
            double lower = 0.0;

            // 全ての点群に対して処理
            for( final PointF p : _points ){
                // 重心から点までの角度を求める
                PointF pos = new PointF( p.x, p.y );
                double at = Math.atan2((double) (pos.y - _center.y), (double) (pos.x - _center.x));
                at -= angle*Math.PI/180.0;
    
                // 距離
                double distance = _calculateDistance( pos, _center ) * Math.sin( at );
    
                // 上側
                if( distance >= 0.0 ){
                    if( upper < distance ){
                        upper = distance;
                    }
                }
                // 下側
                else{
                    if( lower > distance ){
                        lower = distance;
                    }
                }
            }
    
            // フェレ径を算出
            double ferreDiameter = upper - lower;
    
            // フェレ径が最小
            if( minFerreDiameter > ferreDiameter ){
                minFerreDiameter = ferreDiameter;
                minAngle = angle;
                minUpperDistance = upper;
                minLowerDistance = -lower;
            }
        }
    
        // 最小フェレ径に対応するフェレ径を算出
        {
            double upper = 0.0;
            double lower = 0.0;
    
            for( final PointF p : _points ){
                PointF pos = new PointF( p.x, p.y );
                double at = Math.atan2( (double)( pos.y - _center.y ), (double)( pos.x - _center.x ) );
                at -= (minAngle+90.0)*Math.PI/180.0;
    
                double distance = _calculateDistance( pos, _center ) * Math.sin(at);
    
                if( distance >= 0.0 ){
                    if( upper < distance ){
                        upper = distance;
                    }
                }
                else{
                    if( lower > distance ){
                        lower = distance;
                    }
                }
            }
    
            maxUpperDistance = upper;
            maxLowerDistance = -lower;
        }
    
        // 角度保存
        _angle = minAngle*Math.PI/180.0;
    
        // 外接四角形の角の座標を算出
        _cornerPos[0] = new PointF( _center.x + (float)( Math.cos( _angle + Math.PI / 2.0 )*minUpperDistance + Math.cos( _angle + Math.PI )*maxUpperDistance ),
                                      _center.y + (float)( Math.sin( _angle + Math.PI / 2.0 )*minUpperDistance + Math.sin( _angle + Math.PI )*maxUpperDistance ) );
        _cornerPos[1] = new PointF( _center.x + (float)( Math.cos( _angle - Math.PI / 2.0 )*minLowerDistance + Math.cos( _angle + Math.PI )*maxUpperDistance ),
                                      _center.y + (float)( Math.sin( _angle - Math.PI / 2.0 )*minLowerDistance + Math.sin( _angle + Math.PI )*maxUpperDistance ) );
        _cornerPos[2] = new PointF( _center.x + (float)( Math.cos( _angle - Math.PI / 2.0 )*minLowerDistance + Math.cos( _angle )*maxLowerDistance ),
                                      _center.y + (float)( Math.sin( _angle - Math.PI / 2.0 )*minLowerDistance + Math.sin( _angle )*maxLowerDistance ) );
        _cornerPos[3] = new PointF( _center.x + (float)( Math.cos( _angle + Math.PI / 2.0 )*minUpperDistance + Math.cos( _angle )*maxLowerDistance ),
                                      _center.y + (float)( Math.sin( _angle +Math. PI / 2.0 )*minUpperDistance + Math.sin( _angle )*maxLowerDistance ) );
    }


    private void _kMeans( ArrayList<Cluster> points, PointF[] _weightPoints, int k )
    {
        /*  変数宣言  */
        PointF[] weightHistory = new PointF[k];

        /*  初期化  */
        for( int i=0; i<k; ++i ) _weightPoints[i] = new PointF(0.0f, 0.0f);

        // とりあえず最大200回で打ち切る
        for( int l=0; l < 200; ++l ){
            // 現在の重心を保存
            System.arraycopy( _weightPoints, 0, weightHistory, 0, k );

            // 重心算出
            double[] count = new double[k];
            for( int i=0; i<k; ++i ) count[i] = 0.0;

            for( final Cluster c : points ){
                _weightPoints[c.cluster].x += c.point.x;
                _weightPoints[c.cluster].y += c.point.y;
                count[c.cluster]++;
            }

            for( int i=0; i<k; ++i ){
                if( count[i] > 0.0 ){
                    _weightPoints[i].x /= count[i];
                    _weightPoints[i].y /= count[i];
                } else {
                    _weightPoints[i].x = -1000.0f;
                    _weightPoints[i].y = -1000.0f;
                }
            }

            // 前回の重心と比較
            if( _pointsComparison( _weightPoints, weightHistory, k ) ){
                break;
            }

            // クラスター更新
            for( final Cluster c : points ){
                double minDistance = _calculateDistance( c.point, _weightPoints[0] );
                int minNum = 0;
                for (int i = 1; i < k; ++i) {
                    double dist = _calculateDistance( c.point, _weightPoints[i] );
                    if( minDistance > dist ) {
                        minDistance = dist;
                        minNum = i;
                    }
                }
                c.cluster = minNum;
            }
        }
    }


    private boolean _pointsComparison( PointF[] a, PointF[] b, int num )
    {
        for( int i=0; i<num; ++i ){
            if( ( a[i].x != b[i].x ) || ( a[i].y != b[i].y ) ){
                return false;
            }
        }

        return true;
    }

    
    private boolean _intersectionDetermination( PointF a, PointF b, PointF c, PointF d )
    {
        /*  変数宣言  */
        PointF ab = new PointF( b.x - a.x, b.y - a.y );
        PointF ac = new PointF( c.x - a.x, c.y - a.y );
        PointF ad = new PointF( d.x - a.x, d.y - a.y );
        PointF ca = new PointF( a.x - c.x, a.y - c.y );
        PointF cb = new PointF( b.x - c.x, b.y - c.y );
        PointF cd = new PointF( d.x - c.x, d.y - c.y );

        /*  判定  */
        if( _crossProduct( cd, ca )*_crossProduct( cd, cb ) < 0 && _crossProduct( ab, ac )*_crossProduct( ab, ad ) < 0 ){
            return true;
        }

        return false;
    }


    private PointF _intersectionCoordinate( PointF a, PointF b, PointF c, PointF d )
    {
        /*  変数宣言  */
        //PointF ab = new PointF( b.x - a.x, b.y - a.y );
        PointF ac = new PointF( c.x - a.x, c.y - a.y );
        //PointF ad = new PointF( d.x - a.x, d.y - a.y );
        //PointF ca = new PointF( a.x - c.x, a.y - c.y );
        PointF cb = new PointF( b.x - c.x, b.y - c.y );
        PointF cd = new PointF( d.x - c.x, d.y - c.y );

        /*  計算  */
        float k = _crossProduct( cd, ac ) / ( _crossProduct( cd, ac ) + _crossProduct( cd, cb ) );

        return new PointF( a.x + ( k*( b.x - a.x ) ), a.y + ( k*( b.y - a.y ) ) );
    }


    private float _innerProduct( PointF a, PointF b )
    {
        /*  計算  */
        return a.x * b.x + a.y * b.y;
    }


    private float _crossProduct( PointF a, PointF b )
    {
        /*  計算  */
        return a.x * b.y - a.y * b.x;
    }


    private void _vectorNormalization( PointF a )
    {
        /*  計算  */
        float magnitude = (float)Math.sqrt( a.x*a.x + a.y*a.y );

        a.x /= magnitude;
        a.y /= magnitude;
    }


    private float _calculateDistance( PointF a, PointF b )
    {
    /*  計算  */
        return (float)Math.sqrt(((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)));
    }


    private double _linePointDistance( PointF a, PointF b, PointF p )
    {
        /*  変数宣言  */
        PointF ab = new PointF( b.x - a.x, b.y - a.y );
        PointF ap = new PointF( p.x - a.x, b.y - p.y );

        /*  計算  */
        double area = _crossProduct( ab, ap );
        double distance = _calculateDistance( a, b );

        return area/distance;
    }


    private PointF _perpendicular( PointF a, PointF b, PointF p )
    {
        /*  変数宣言  */
        //PointF ab = new PointF( b.x - a.x, b.y - a.y );
        PointF ap = new PointF( p.x - a.x, p.y - a.y );

        PointF abN = new PointF( b.x - a.x, b.y - a.y );
        _vectorNormalization( abN );

        PointF ret = new PointF();

        /*  計算  */
        double distance = _innerProduct( abN, ap );

        if( distance < 0.0 || distance > _calculateDistance( a, b ) ){
            ret.x = -1.0f;
            ret.y = -1.0f;

            return ret;
        }

        ret.x += abN.x*distance;
        ret.y += abN.y*distance;

        return ret;
    }


    private double _colorDistance( int color0, int color1 )
    {
        int r0 = Color.red( color0 );
        int g0 = Color.green( color0 );
        int b0 = Color.blue( color0 );

        int r1 = Color.red( color1 );
        int g1 = Color.green( color1 );
        int b1 = Color.blue( color1 );

        return Math.sqrt( ( r0 - r1 ) * ( r0 - r1 )
                         + ( g0 - g1 ) * ( g0 - g1 )
                         + ( b0 - b1 ) * ( b0 - b1 ) );
    }
}