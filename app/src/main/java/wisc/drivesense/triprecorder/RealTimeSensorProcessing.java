package wisc.drivesense.triprecorder;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.OldTrace;
import wisc.drivesense.utility.Trace;


public class RealTimeSensorProcessing {



	private static final String TAG = "RealTimeSensorProcessing";

	public RealTimeSensorProcessing() {
		
	}
	
	private List<OldTrace> window_accelerometer = new LinkedList<OldTrace>();
	private List<OldTrace> window_gyroscope = new LinkedList<OldTrace>();
	private List<OldTrace> window_rotation_matrix = new LinkedList<OldTrace>();
	private OldTrace curSmoothedAccelerometer = null;
	private OldTrace curSmoothedGyroscope = null;
	private int kWindowSize = 15;


    /**
     * the only input point
     * @param trace
     */
    public void processTrace(Trace input) {

        OldTrace trace = OldTrace.fromTrace(input);
		if(trace == null || trace.type == null) {
			return;
		}

        String type = trace.type;
        if(type.equals(OldTrace.ACCELEROMETER)) {
            onAccelerometerChanged(trace);
        } else if (type.equals(OldTrace.GYROSCOPE)) {
            onGyroscopeChanged(trace);
        } else if(type.equals(OldTrace.ROTATION_MATRIX)) {
            window_rotation_matrix.add(trace);
            if(window_rotation_matrix.size() > kWindowSize) {
                window_rotation_matrix.remove(0);
            }
        } else if(type.equals(OldTrace.GPS)) {
            onGPSChanged(trace);
        } else if(type.equals(OldTrace.MAGNETOMETER)){
            onMagnetometerChanged(trace);
        } else {
            Log.d("Uncaptured trace type", trace.toString());
        }
    }


    public double curTilt = 0.0;
	private void calculateTilt() {
		double x = curSmoothedAccelerometer.values[0];
		double z = curSmoothedAccelerometer.values[2];
		double angle = 0.0;
		if(z == 0.0) {
			angle = x > 0.0 ? -1.57 : 1.57;
		} else {
			angle = Math.atan(-x/z);
		}
		this.curTilt = Math.toDegrees(angle);
	}
	
	private OldTrace curProjectedGyroscope = null;
	private void steeringMonitoring() {
		if(this.initRM_ == null) {
			return;
		}
		curProjectedGyroscope = RealTimeSensorProcessing.rotate(curSmoothedGyroscope, this.initRM_.values);

		extractSteeringEvent();
	}
	
	private double kTurnThreshold = 0.05;
	public List<Trace.EventTrace> events = new ArrayList<Trace.EventTrace>();
	private Trace.EventTrace event_turn = null;
	private OldTrace pastGyro = null;
	private int steering_counter = 0;

	private void extractSteeringEvent() {
		OldTrace trace = curProjectedGyroscope;
		double value = trace.values[2];
			
		if(window_gyroscope.size() < kWindowSize) {
			return;
		}
		OldTrace pre = RealTimeSensorProcessing.rotate(window_gyroscope.get(kWindowSize - 2), this.initRM_.values);
			
		if(Math.abs(value) >= kTurnThreshold) {
			steering_counter++;			
		}
		double pv = pastGyro.values[2];			
		if(Math.abs(pv) >= kTurnThreshold)	{
			steering_counter--;
		}
			
		boolean turning = false;
		if((double)steering_counter >= (double)kWindowSize * 0.6) {
			turning = true;
		}
			
		if(turning) {
			if(event_turn == null) {
				//the start of the event
				double turnsum = 0.0, turnabsolutesum = 0.0;
				for(int i = 0; i < window_gyroscope.size() - 1; ++i) {
					OldTrace cur = RealTimeSensorProcessing.rotate(window_gyroscope.get(i), this.initRM_.values);
					OldTrace next = RealTimeSensorProcessing.rotate(window_gyroscope.get(i + 1), this.initRM_.values);
					double steers = cur.values[2] * (next.time - cur.time)/1000.0;
					turnsum += steers;
					turnabsolutesum += Math.abs(steers);
				}
				
				event_turn = new Trace.EventTrace();
				event_turn.time = window_gyroscope.get(0).time;
				event_turn.steering_sum_ = turnsum;
				event_turn.steering_abs_sum_ = turnabsolutesum;
				
			} else {
				double steers = pre.values[2] * (trace.time - pre.time)/1000.0;
				event_turn.steering_sum_ += steers;
				event_turn.steering_abs_sum_  += Math.abs(steers); 				
			}
		} else {
			if(event_turn != null) {
				event_turn.endtime = trace.time;
				
				double degree = Math.abs(Math.toDegrees(event_turn.steering_sum_));
				//Log.d(TAG, degree, Math.toDegrees(event_turn.steering_abs_sum_));
				if(degree >= 60.0){
					event_turn.event_type = Trace.EventTrace.TURN;
					events.add(event_turn);
				} else if(degree >= 20) {
					event_turn.event_type = Trace.EventTrace.CURVING;
					events.add(event_turn);
				} else if(degree <= 10) {
					if(Math.toDegrees(event_turn.steering_abs_sum_) >= 20) {
						event_turn.event_type = Trace.EventTrace.LANECHANGE;
						events.add(event_turn);
					}
				} else {
					
				}				
			}
			event_turn = null;
		}
	
	}

	public List<List<OldTrace>> getTrainSet() {
		return trainset_;
	}
	public OldTrace getInitRM() {
		return initRM_;
	}
	public OldTrace getHorizontalRM() {
		return this.horizontalRM_;
	}
	
	public OldTrace getGyroDrift() {
		return this.gyrodrift_;
	}
	
	private boolean stopped_ = false;
	private List<List<OldTrace>> trainset_ = new ArrayList<List<OldTrace>>();
	private List<OldTrace> trainsample_ = null;
	
	private OldTrace initRM_ = null;
	private OldTrace horizontalRM_ = null;
	private OldTrace verticalRM_ = null;
	
	private double gravity_ = 0.0;
	
	private int train_len_ = 30;
	public void setTrainLength(int len) {
		this.train_len_ = len;
	}
	public int getTrainLength() {
		return this.train_len_;
	}
	
	private void onMagnetometerChanged(OldTrace magnetometer) {
		
	}
	
	private void onGPSChanged(OldTrace gps) {
		
	}
	
	
	private void onAccelerometerChanged(OldTrace accelerometer) {
		
		curSmoothedAccelerometer = lowpassFilter(curSmoothedAccelerometer, accelerometer);
		window_accelerometer.add(curSmoothedAccelerometer);
		
		detectOrientationChange(curSmoothedAccelerometer);
		monitorStability(curSmoothedAccelerometer);
		calculateTilt();
		
		if(window_accelerometer.size() >= kWindowSize) {
			stopped_ = stopped(window_accelerometer);
			
			
			if(straight_) {
				//train the initial rotation matrix
				//opportunity to train the oritentation matrix
				if(gravity_ <= 0.0 &&  stopped_ == true) {
					gravity_ = 0.0;
					OldTrace tmp = this.getAverage(window_accelerometer);
					for(int j = 0; j < tmp.dim; ++j) {
						gravity_ += tmp.values[j] * tmp.values[j];
					}
					gravity_ = Math.sqrt(gravity_);
					initRM_ = this.getAverage(window_rotation_matrix);
				}
					
				if(null == trainsample_) {
					trainsample_ = new ArrayList<OldTrace>();
					for(int i = 0; i < window_accelerometer.size(); ++i) {
						trainsample_.add(window_accelerometer.get(i));
					}
				} else {
					trainsample_.add(curSmoothedAccelerometer);
				}
			} else {
				//put current train sample into train set
				if(trainsample_ != null) {
					int trainlen = trainsample_.size();
					
					//we use small segment to train
					if(trainlen >= this.train_len_) {
						trainset_.add(trainsample_);		
					}
					
					if(trainlen >= this.train_len_) {
						//training opportunity
						//calculateHorizontalRM(trainsample_);
					}
					if (trainlen >= this.train_len_) {
						calibrateByAccelerometer(trainsample_);
					}
					
				}
				trainsample_ = null;
			}
			window_accelerometer.remove(0);
		}
	}
	
	public OldTrace calculateDirectionVector(List<OldTrace> trainsample) {
		//Log.d(TAG, "calculateDirectionVector");
		OldTrace direction = new OldTrace(3);
		List<OldTrace> window = new ArrayList<OldTrace>();
		List<Integer> pattern = new ArrayList<Integer>();		
		for(OldTrace trace: trainsample) {
			window.add(trace);
			if(window.size() >= kWindowSize) {
				boolean issteady = stopped(window);
				int label = issteady == true ? 1: -1;
				int sz = pattern.size();
				if(sz == 0) {
					pattern.add(label);
				} else {
					if(pattern.get(sz - 1)/label > 0) {
						Integer cur = pattern.get(sz - 1).intValue() + label;
						pattern.remove(sz - 1);
						pattern.add(cur);
					} else {
						pattern.add(label);
					}
				}
				window.remove(0);
			}
		}
		int pos = 0;
		for(Integer cur: pattern) {
			if(cur.intValue() < 0) {
				OldTrace sub = this.getAverage(trainsample.subList(pos, pos + Math.abs(cur.intValue())));
			}
			pos += Math.abs(cur);
		}
		return direction;
	}


	
	private void calibrateByAccelerometer(List<OldTrace> trainsample) {
		if(this.initRM_ == null || this.horizontalRM_ == null) {
			return;
		}
		List<OldTrace> aligned = new ArrayList<OldTrace>();
		for(OldTrace trace: trainsample) {
			OldTrace tmp = RealTimeSensorProcessing.rotate(trace, this.initRM_.values);
			OldTrace cur = RealTimeSensorProcessing.rotate(tmp, this.horizontalRM_.values);
			aligned.add(cur);
		}
		OldTrace avg = this.getAverage(aligned);
	}

    /*
	private double coeff1 = 0.0;
	private int coeffcounter = 0;
	private void calculateHorizontalRM(List<OldTrace> trainsample) {
		if(initRM_ == null) {
			return;
		}
		List<OldTrace> sample = new ArrayList<OldTrace>();
		for(OldTrace trace: trainsample) {
			OldTrace cur = RealTimeSensorProcessing.rotate(trace, initRM_.values);
			sample.add(cur);
		}
		double [] coeff = RealTimeSensorProcessing.curveFit(sample, 0, 1);
	
		coeff1 = (sample.size() * coeff[1] + coeff1 * coeffcounter) / (sample.size() + coeffcounter);
		coeffcounter += sample.size();
		
		
		OldTrace hdiff = new OldTrace(3);
		hdiff.setValues(1.0, coeff1, 0.0);
		OldTrace unit = RealTimeSensorProcessing.getUnitVector(hdiff);
		OldTrace yaxe = new OldTrace(3);
		yaxe.setValues(0.0, 1.0, 0.0);
		this.horizontalRM_ = RealTimeSensorProcessing.rotationMatrixBetweenHorizontalVectors(unit, yaxe);
	}
	*/
	
	private boolean straight_ = false;
	private OldTrace gyrodrift_ = new OldTrace(3);
	private void onGyroscopeChanged(OldTrace gyroscope) {
		curSmoothedGyroscope = lowpassFilter(curSmoothedGyroscope, gyroscope);
		window_gyroscope.add(curSmoothedGyroscope);
		if(window_gyroscope.size() > kWindowSize) {
			straight_ = isStraight(window_gyroscope);
			OldTrace past = window_gyroscope.remove(0);
			if(this.initRM_ != null) {
				pastGyro = RealTimeSensorProcessing.rotate(past, this.initRM_.values);
			} else {
				pastGyro = null;
			}
		} else {
			return;
		}
		steeringMonitoring();
	}
	
	/**
	 * check if the car is moving straight by gyroscope
	 * @param window
	 * @return
	 */
	public boolean isStraight(List<OldTrace> window) {
		//final double threshold = 0.005; 
		final double threshold = 0.004; //0.008;
		
		double[] devi = RealTimeSensorProcessing.standardDeviation(window);
		for(int i = 0; i < devi.length; ++i) {
			if(devi[i] >= threshold) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * check if the car is stopped by accelerometer
	 * @param window
	 * @return
	 */
	public boolean stopped(List<OldTrace> window) {
		final double threshold = 0.004;
		double variance = this.calculateClusterVariance(window);
		if(variance <= threshold) {
			return true;
		} else {
			return false;
		}
		
	}
	
	
	public OldTrace lowpassFilter(OldTrace last, OldTrace cur) {
		final double alpha = Constants.kExponentialMovingAverageAlpha;
		OldTrace res = new OldTrace(cur.dim);
		res.copyTrace(cur);
		if(last != null) {
			for(int j = 0; j < cur.dim; ++j) {
				res.values[j] = alpha * cur.values[j] + (1.0 - alpha) * last.values[j];
			}
		}
		return res;
	}
	
	
	public static double calculateClusterVariance(List<OldTrace> cluster) {
		double mean = 0.0, M2 = 0.0;
		OldTrace center = new OldTrace(3);
		center.copyTrace(cluster.get(0));
		int counter = 0;
		for(int i = 1; i < cluster.size(); ++i) {
			OldTrace cur = cluster.get(i);
			double dist = 0.0;
			dist = RealTimeSensorProcessing.euclideanDistance(center, cur);
			counter ++;
			for(int j = 0; j < center.dim; ++j) {
				center.values[j] += (cur.values[j] - center.values[j])/counter; 
			}
			double delta = dist - mean;
			mean += delta/counter;
			M2 += delta * (dist - mean);
		}
		return M2/counter;
	}
	
	private List<OldTrace> orientation_buffer_ = new ArrayList<OldTrace>();
	private boolean orientation_changing_ = false;
	private int number_of_orientation_changed = 0;
	
	public void detectOrientationChange(OldTrace accelerometer) {
		
		orientation_buffer_.add(accelerometer);
		if(orientation_buffer_.size() > 30) orientation_buffer_.remove(0);						
		
		double m2 = calculateClusterVariance(orientation_buffer_);
		
		if(m2 > Constants.kOrientationChangeVarianceThreshold) {
			if(orientation_changing_ == false) {
				//start changeing
				number_of_orientation_changed++;
			}
			orientation_changing_ = true;
		} else {
			if(orientation_changing_ == true) {
				orientation_changing_ = false;
				//chaning ends
			}
		}
	}
	
	private List<OldTrace> mv_buffer_ = new ArrayList<OldTrace>();
	private double avg_mv_ = 0.0;
	private int mv_counter_ = 0;
	public void monitorStability(OldTrace accelerometer) {
		
		if(this.orientation_changing_ == true) {
			mv_buffer_.clear();
		}
		
		mv_buffer_.add(accelerometer);
		if(mv_buffer_.size() > 600) mv_buffer_.remove(0);						
		
		double m2 = calculateClusterVariance(mv_buffer_);
		double sum = avg_mv_ * mv_counter_ + m2;
		avg_mv_ = sum / (++mv_counter_);
	}




	//////////////////////////////////////////////////////////////////////////////
	
	private OldTrace getAverage(List<OldTrace> input) {
		int sz = input.size();
		int d = input.get(sz - 1).dim;
		double [] sum = new double[d];
		for(int j = 0; j < d; ++j) sum[j] = 0.0;
		
		for(int i = 0; i < sz; ++i) {
			OldTrace temp = input.get(i);
			for(int j = 0; j < d; ++j) {
				sum[j] += temp.values[j];
			}
		}
		OldTrace trace = new OldTrace(d);
		for(int j = 0; j < d; ++j) {
			trace.values[j] = sum[j]/sz;
		}

		return trace;
	}
	

    /**
     * Calculate the euclidean distance between two traces
     * @param tr0
     * @param tr1
     * @return
     */
    public static double euclideanDistance(OldTrace tr0, OldTrace tr1) {
        double res = 0.0;
        double sum = 0.0;
        for(int i = 0; i < tr0.values.length; ++i) {
            sum += Math.pow(tr1.values[i] - tr0.values[i], 2.0);
        }
        res = Math.sqrt(sum);
        return res;
    }

    /*
     * For a given trace (preferably the raw accelerometer data, but apply to all)
     * return the standard deviation of the traces
     * */
    public static double[] standardDeviation(List<OldTrace> traces) {
        int sz = traces.size();
        int d = traces.get(sz - 1).dim;

        double[] average = new double[d];
        double[] res = new double [d];
        for(int j = 0; j < d; ++j) {
            average[j] = 0.0;
            res[j] = 0.0;
        }
        for(OldTrace trace: traces) {
            for(int j = 0; j < d; ++j) {
                average[j] += trace.values[j];
            }
        }
        for(int j = 0; j < d; ++j) {
            average[j] /= sz;
        }
        for(OldTrace trace: traces) {
            for(int j = 0; j < d; ++j) {
                res[j] += Math.pow((average[j] - trace.values[j]), 2.0);
            }
        }
        for(int j = 0; j < d; ++j) {
            res[j] = Math.sqrt(res[j]/sz);
        }

        return res;
    }


    public static OldTrace rotate(OldTrace raw_tr, double[] rM) {
    	OldTrace calculated_tr = new OldTrace();
        calculated_tr.time = raw_tr.time;
        double x, y, z;
        x = raw_tr.values[0];
        y = raw_tr.values[1];
        z = raw_tr.values[2];

        calculated_tr.values[0] = x * rM[0] + y * rM[1] + z * rM[2];
        calculated_tr.values[1] = x * rM[3] + y * rM[4] + z * rM[5];
        calculated_tr.values[2] = x * rM[6] + y * rM[7] + z * rM[8];

        return calculated_tr;
    }

    /*
    public static double[] curveFit(List<OldTrace> acce, int i, int j) {
        final WeightedObservedPoints obs = new WeightedObservedPoints();
        for(OldTrace trace: acce) {
            double x = trace.values[i];
            double y = trace.values[j];
            obs.add(x, y);
        }
        // Instantiate a third-degree polynomial fitter.
        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        // Retrieve fitted parameters (coefficients of the polynomial function).
        final double[] coeff = fitter.fit(obs.toList());
        return coeff;
    }
    */

    public static OldTrace getUnitVector(OldTrace input) {
    	OldTrace res = new OldTrace(3);
        double sum = Math.sqrt(Math.pow(input.values[0], 2.0) + Math.pow(input.values[1], 2.0));
        res.setValues(input.values[0]/sum, input.values[1]/sum, 0.0);
        return res;
    }

    public static OldTrace rotationMatrixBetweenHorizontalVectors(OldTrace v0, OldTrace v1) {
    	OldTrace res = new OldTrace(9);
        double cos_theta = v0.values[0] * v1.values[0] + v0.values[1] * v1.values[1];
        double sin_theta = v0.values[0] * v1.values[1] - v0.values[1] * v1.values[0];
        res.values[0] = cos_theta;
        res.values[1] = - sin_theta;
        res.values[2] = 0.0;
        res.values[3] = sin_theta;
        res.values[4] = cos_theta;
        for(int i = 5; i < 8; ++i) {
            res.values[i] = 0.0;
        }
        res.values[8] = 1.0;
        return res;
    }

	
	
}
