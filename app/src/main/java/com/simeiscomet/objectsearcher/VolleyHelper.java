package com.simeiscomet.objectsearcher;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.io.IOException;

/**
 * http://miblog.guruguruheadslab.com/archives/555
 */

public class VolleyHelper {
    private static final Object sLock = new Object();
    private static RequestQueue sRequestQueue;

    /*
     * RequestQueueのシングルトン生成
     */
    public static RequestQueue getRequestQueue( final Context context ) throws IOException {
        synchronized( sLock ) {
            if ( sRequestQueue == null ) {
                //sRequestQueue = Volley.newRequestQueue( context, new MultiPartStack() );
                sRequestQueue = Volley.newRequestQueue(context);
            }
            return sRequestQueue;
        }
    }
}
