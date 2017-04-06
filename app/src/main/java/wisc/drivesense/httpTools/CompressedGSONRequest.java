package wisc.drivesense.httpTools;

import com.android.volley.AuthFailureError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import wisc.drivesense.user.DriveSenseToken;
import wisc.drivesense.utility.GsonSingleton;

/**
 * Created by peter on 12/2/16.
 */

public abstract class CompressedGSONRequest<T> extends GsonRequest<T> {
    private final String TAG = "CompressedGSONRequest";
    public CompressedGSONRequest(int method, String url, Object body, Class<T> responseClass, DriveSenseToken dsToken) {
        super(method, url, body, responseClass);
        this.dsToken = dsToken;
    }

    public String getBodyContentType()
    {
        return "application/json";
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {

        HashMap<String, String> headers = new HashMap<>(super.getHeaders());
        headers.put("Content-Encoding", "gzip");
        return headers;
    }

    @Override
    public byte[] getBody() {
        String json = GsonSingleton.toJson(payload);
        byte[] jsonBytes = json.getBytes();

        ByteArrayOutputStream os = new ByteArrayOutputStream(jsonBytes.length);
        try {
            GZIPOutputStream gos = new GZIPOutputStream(os);
            gos.write(jsonBytes);
            gos.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] output = os.toByteArray();
        //Log.d(TAG, "Compressed payload from "+jsonBytes.length + " bytes to "+output.length+" bytes, a compression ratio of "+output.length/(float)jsonBytes.length);
        return output;
    }
}
