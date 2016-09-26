package com.simeiscomet.objectsearcher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by NKJ on 2016/09/20.
 */
public class HttpPostAsync extends AsyncTask<String, Integer, Boolean> implements DialogInterface.OnCancelListener {
    private final String _fileName = "objectsearcher_cashimage.png";
    private File _dir;
    private String _url;
    private Bitmap _image;

    private Context _context;
    private ProgressDialog _dialog;

    public HttpPostAsync( Context context0, String url0, Bitmap image0 )
    {
        _context = context0;
        _url = url0;
        _image = image0;
    }

    @Override
    protected void onPreExecute()
    {
        // cashフォルダを指定
        _dir = _context.getCacheDir();

        try {
            _tmpImageSave( _dir, _fileName, _image );
        } catch ( FileNotFoundException e1 ){
            Log.e("Error", "" + e1.toString());
            Toast.makeText( _context, "一時ファイルの作成に失敗しました", Toast.LENGTH_SHORT ).show();
            throw new RuntimeException( e1.toString() );
        } catch ( IOException e2 ){
            Log.e("Error", "" + e2.toString());
            Toast.makeText( _context, "一時ファイルの切り離しに失敗しました", Toast.LENGTH_SHORT ).show();
            throw new RuntimeException( e2.toString() );
        }

        //ダイアログを出す
        _dialog = new ProgressDialog(_context);
        _dialog.setTitle("おまちください");
        _dialog.setMessage("送信中");
        _dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//くるくる
        //dialog.setIndeterminate(true);//進捗不確定モード
        //dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//プログレスバー
        //_dialog.setMax(100);
        //_dialog.setProgress(0);

        //ダイアログをキャンセルできるようにする
        //_dialog.setCancelable(true);
        //_dialog.setOnCancelListener(this);

        _dialog.show();
    }

    @Override
    protected Boolean doInBackground( String... params )
    {
        HttpMultiPart httpPost = new HttpMultiPart();
        return httpPost.sendData( _url, _dir, _fileName );
    }

    @Override
    protected void onProgressUpdate( Integer... values )
    {

    }

    @Override
    protected void onPostExecute( Boolean result )
    {
        if( result ){
            Toast.makeText( _context, "画像の送信に成功しました", Toast.LENGTH_SHORT ).show();
        } else {
            Toast.makeText( _context, "画像の送信に失敗しました", Toast.LENGTH_SHORT ).show();
        }

        try {
            _tmpImageDelete( _dir, _fileName );
        } catch ( NullPointerException e1 ){
            Log.e("Error", "" + e1.toString() );
            Toast.makeText( _context, "一時ファイルの削除に失敗しました", Toast.LENGTH_SHORT ).show();
            throw new RuntimeException( e1.toString() );
        } catch ( SecurityException e2 ){
            Log.e("Error", "" + e2.toString());
            Toast.makeText( _context, "一時ファイルへのアクセスに失敗しました", Toast.LENGTH_SHORT ).show();
            throw new RuntimeException( e2.toString() );
        }

        if( _dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
        }
    }

    @Override
    protected void onCancelled()
    {
        //Thread.interrupt();
    }

    @Override
    public void onCancel( DialogInterface dialog )
    {
        cancel(true);
    }


    private void _tmpImageSave( File dir, String fileName, Bitmap image ) throws FileNotFoundException, IOException
    {
        // 保存処理開始
        FileOutputStream fos = null;
        fos = new FileOutputStream( new File( dir, fileName ) );

        // jpegで保存
        image.compress( Bitmap.CompressFormat.PNG, 100, fos );

        // 保存処理終了
        fos.close();
    }


    private void _tmpImageDelete( File dir, String fileName ) throws NullPointerException, SecurityException
    {
        // 削除処理開始
        File f = null;
        f = new File( dir, fileName );

        // 削除
        f.delete();
    }
}
