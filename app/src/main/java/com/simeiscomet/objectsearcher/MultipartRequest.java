package com.simeiscomet.objectsearcher;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonRequest;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import java.io.File;
import java.util.Map;

/**
 * http://qiita.com/tomoima525/items/8e77c4cfe51339974545
 */
public class MultipartRequest extends JsonRequest<String> {
    private final Response.Listener<String> mListener;
    private final Map<String, String> mStringParts;
    private final Map<String, File> mFileParts;
    private MultipartEntityBuilder mBuilder = MultipartEntityBuilder.create();

    public MultipartRequest(String url,
                            Map<String, String> stringParts,
                            Map<String, File> fileParts,
                            Response.Listener<String> listener,
                            Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, null, listener, errorListener);
        mListener = listener;
        mStringParts = stringParts;
        mFileParts = fileParts;
        buildMultipartEntity();
    }

    private void buildMultipartEntity() {
        mBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        mBuilder.setBoundary("_____"+Long.toString(System.currentTimeMillis())+"_____");
        mBuilder.setCharset(Consts.UTF_8);
        for ( Map.Entry<String, String> entry : mStringParts.entrySet() ) {
            mBuilder.addTextBody(entry.getKey(), entry.getValue());
        }
        for ( Map.Entry<String, File> entry : mFileParts.entrySet() ) {
            ContentType imageContentType = ContentType.create("image/png");
            //mBuilder.addBinaryBody("upfile", entry.getValue(), imageContentType, entry.getKey());
            mBuilder.addPart(entry.getKey(), new FileBody(entry.getValue(), imageContentType));
        }
    }

    @Override
    public String getBodyContentType() {
        return mBuilder.build().getContentType().getValue();
    }

/*  @Override
    public byte[] getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            mBuilder.build().writeTo(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }
*/

    public HttpEntity getEntity() {
        return mBuilder.build();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        return Response.success("Uploaded", getCacheEntry());
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}