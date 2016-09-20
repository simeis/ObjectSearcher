package com.simeiscomet.objectsearcher;

import android.content.DialogInterface;
import android.os.AsyncTask;

/**
 * Created by NKJ on 2016/09/20.
 */
public class HttpPostAsync extends AsyncTask<String, Integer, String> implements DialogInterface.OnCancelListener {

    public HttpPostAsync()
    {

    }

    @Override
    protected void onPreExecute()
    {

    }

    @Override
    protected String doInBackground( String... params )
    {
        return params[0];
    }

    @Override
    protected void onProgressUpdate( Integer... values )
    {

    }

    @Override
    protected void onPostExecute( String result )
    {

    }

    @Override
    protected void onCancelled()
    {

    }

    @Override
    public void onCancel( DialogInterface dialog )
    {

    }
}
