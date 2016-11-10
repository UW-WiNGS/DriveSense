package wisc.drivesense.utility;

import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * Created by Vector on 11/10/2016.
 */

public class TraceMessage {

    @Expose
    public String type;
    @Expose
    public Trace value;

    public static TraceMessage FromIntent(Intent intent) {
        Gson gson = new Gson();
        return gson.fromJson(intent.getStringExtra("trace"), TraceMessage.class);
    }

    public TraceMessage(Trace trace) {
        this.type = trace.getClass().getSimpleName();
        this.value = trace;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TraceMessage) {
            return Serialize.toJson(this).equals(Serialize.toJson(obj));
        }
        return super.equals(obj);
    }
}
