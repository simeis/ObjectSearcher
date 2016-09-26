package com.simeiscomet.objectsearcher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

/**
 * Created by NKJ on 2016/09/21.
 */
public abstract class ImageProcessingAsync<Result> extends AsyncTask<Void, Void, Result> implements DialogInterface.OnCancelListener {
    private Context _context;
    private ProgressDialog _dialog = null;

    private String[] _message;
    private int _progress = 0;
    private int _maxProgress = 0;

    private Exception exception;

    abstract protected Result doTask() throws Exception;
    abstract protected void onSuccess(Result result);
    abstract protected void onFailure(Exception exception);

    public ImageProcessingAsync( Context context0, String[] message0, int nowProgress )
    {
        _context = context0;
        _maxProgress = message0.length-1;
        _progress = nowProgress;
        _message = message0;
    }

    public ImageProcessingAsync( Context context0 )
    {
        _context = context0;
        _maxProgress = -1;
        _progress = -1;
        _message = null;
    }

    @Override
    protected void onPreExecute()
    {
        if( _message == null )
            return;

        //ダイアログを出す
        _dialog = new ProgressDialog(_context);
        _dialog.setTitle("おまちください");
        _dialog.setMessage(_message[_progress]);
        //_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//くるくる
        //_dialog.setIndeterminate(true);//進捗不確定モード
        _dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//プログレスバー
        _dialog.setMax( _maxProgress );
        _dialog.setProgress( _progress );

        //ダイアログをキャンセルできるようにする
        _dialog.setCancelable(true);
        _dialog.setOnCancelListener(this);

        _dialog.show();
    }

    @Override
    protected Result doInBackground( Void... params )
    {
        try {
            return doTask();
        } catch (Exception e) {
            exception = e;
            cancel(true);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate( Void... params )
    {
        _progress++;
        _dialog.setMessage( _message[_progress] );
        _dialog.setProgress( _progress );
    }

    @Override
    protected void onPostExecute( Result result )
    {
        onSuccess(result);

        //ダイアログが出ていたら消す
        if( _dialog != null && _dialog.isShowing() ){
            _dialog.dismiss();
        }
    }

    @Override
    protected void onCancelled()
    {
        //if (exception != null) {
            onFailure(exception);
        //}
        exception = null;

        //ダイアログが出ていたら消す
        if( _dialog != null && _dialog.isShowing() ){
            _dialog.dismiss();
        }
    }

    //「implements OnCancelListener」したことで実装
    //ダイアログをキャンセルしたときに呼ばれる
    @Override
    public void onCancel(DialogInterface dialog) {
        //AsyncTaskをキャンセルする
        cancel(true);
    }}
