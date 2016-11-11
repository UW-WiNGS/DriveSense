package wisc.drivesense.utility;

import java.io.Serializable;

import wisc.drivesense.triprecorder.RealTimeTiltCalculation;

/**
 * Created by lkang on 4/20/16.
 */
public class Rating implements Serializable {
    private RealTimeTiltCalculation tiltCalc;
    private int counter_;
    private Trace.GPS lastTrace_;
    private double lastSpeed_;
    private double score_ = 10.0;

    private static String TAG = "Rating";

    public Rating(RealTimeTiltCalculation tiltCalc) {
        lastSpeed_ = -1.0;
        lastTrace_ = null;
        counter_ = 0;
        this.tiltCalc = tiltCalc;
    }

    public Trace.Trip getRating(Trace.GPS trace) {
        int brake = this.calculateBraking(trace);
        tiltCalc.processTrace(trace);
        //create a new trace for GPS, since we use GPS to capture driving behaviors
        Trace.Trip ntrace = trace.copyTrace(Trace.Trip.class);
        ntrace.time = trace.time;
        ntrace.score = (float)score_;
        ntrace.brake = (float)brake;
        ntrace.tilt = (float)tiltCalc.getTilt();
        return ntrace;
    }

    private int calculateBraking(Trace.GPS trace) {
        if(lastTrace_ == null) {
            lastTrace_ = trace;
            return 0;
        }
        double time = trace.time - lastTrace_.time;
        double curSpeed = Trip.distance(lastTrace_, trace)/(time/1000.0);
        if(lastSpeed_ == -1.0) {
            lastSpeed_ = curSpeed;
            return 0;
        } else if(curSpeed == 0.0) {
            return 0;
        } else {
            counter_++;
        }
        double a = (curSpeed - lastSpeed_)/(time/1000.0);

        lastSpeed_ = curSpeed;
        lastTrace_ = trace;
        if(a < -2.5) {
            double curscore = 3.0 - Math.min(3.0, Math.abs(a));
            score_ = (score_ * (counter_ - 1) + curscore * 10.0)/counter_;
            return -1;
        } else {
            score_ = (score_ * (counter_ - 1) + 10.0)/counter_;
            return 0;
        }
    }



}
