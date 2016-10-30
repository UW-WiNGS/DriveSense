package wisc.drivesense.uploader;

import android.nfc.Tag;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by peter on 10/27/16.
 */

public class GsonRequest<T> extends Request<T> {
    private final Gson mGson = new Gson();
    Object mBody;
    final String TAG = "GsonRequest";
    private final Response.Listener<T> listener;
    private final Class<T> responseClass;

    public GsonRequest(int method, String url, Object body, Class<T> responseClass, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mBody = body;
        this.listener = listener;
        this.responseClass = responseClass;
        this.setRetryPolicy(new DefaultRetryPolicy(5000, 0, 0));
    }

    public String getBodyContentType()
    {
        return "application/json";
    }

    @Override
    public byte[] getBody() {
        String json = mGson.toJson(mBody);

        return json.getBytes();
    }

    @Override
    protected void deliverResponse(T response) {
        Log.d(TAG, response.toString());
        this.listener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        Log.d(TAG, error.toString());
        super.deliverError(error);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        Log.d(TAG, "Got a response");
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(mGson.fromJson(json, responseClass),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }
}
