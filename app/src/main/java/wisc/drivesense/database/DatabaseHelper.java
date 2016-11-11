package wisc.drivesense.database;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import wisc.drivesense.user.UserObject;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.GsonSingleton;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.TraceMessage;
import wisc.drivesense.utility.Trip;


public class DatabaseHelper {

    // Logcat tag
    private static final String TAG = "DatabaseHelper";

    private SQLiteDatabase meta_ = null;
    private SQLiteDatabase db_ = null;


    // Database Version
    private static final String DATABASE_NAME = "summary.db";
    private static final String TABLE_META = "meta";
    private static final String CREATE_TABLE_META = "CREATE TABLE IF NOT EXISTS "
            + TABLE_META + "(starttime INTEGER, endtime INTEGER, distance REAL, score REAL, deleted INTEGER, uploaded INTEGER, email TEXT);";
    private static final String TABLE_USER = "user";
    private static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS "
            + TABLE_USER + "(email TEXT, firstname TEXT, lastname TEXT, loginstatus INTEGER);";


    // Table Names
    private static final String TABLE_TRACE = "trace";


    // Table Create Statements

    private static final String CREATE_TABLE_TRACE= "CREATE TABLE IF NOT EXISTS "
            + TABLE_TRACE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, value TEXT, synced INTEGER"
            + ");";



    private boolean opened = false;
    // public interfaces
    public DatabaseHelper() {
        this.opened = true;
        //this.context = cont;
        //openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.CREATE_IF_NECESSARY, null);
        //File dir = this.context.getFilesDir();
        meta_ = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + DATABASE_NAME, null, null);
        meta_.execSQL(CREATE_TABLE_META);
        //we never close meta_ explicitly, maybe

        //create user table
        meta_.execSQL(CREATE_TABLE_USER);

    }


    //open and close for each trip
    public void createDatabase(long t) {
        this.opened = true;
        db_ = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + String.valueOf(t).concat(".db"), null, null);
        db_.execSQL(CREATE_TABLE_TRACE);
    }


    public void closeDatabase() {
        this.opened = false;
        if(meta_ != null && meta_.isOpen()) {
            meta_.close();
        }
        if(db_ != null && db_.isOpen()) {
            db_.close();
        }
    }
    public boolean isOpen() {
        return this.opened;
    }

    public void insertTrip(Trip trip) {
        Gson gson = new Gson();
        Log.d(TAG, "insertTrip" + gson.toJson(trip));
        ContentValues values = new ContentValues();
        values.put("starttime", trip.getStartTime());
        values.put("endtime", trip.getEndTime());
        values.put("distance", trip.getDistance());
        values.put("score", trip.getScore());
        values.put("deleted", 0);
        values.put("uploaded", 0);
        //assign to current user
        UserObject user = this.getCurrentUser();
        if(user != null) {
            values.put("email", user.email_);
        } else {
            //return;
            values.put("email", "");
        }
        meta_.insert(TABLE_META, null, values);
    }

    public void insertSensorData(Trace trace) {
        ContentValues values = new ContentValues();
        TraceMessage tm = new TraceMessage(trace);
        values.put("synced", false);
        values.put("value", GsonSingleton.toJson(tm));
        values.put("type", tm.type);
        db_.insert(TABLE_TRACE, null, values);
    }



    /**
     * @brief get the gps points of a trip, which is identified by the start time (the name of the database)
     * @param time the start time of a trip (also the name of the database)
     * @return a list of trace, or gps points
     */
    public List<Trace.Trip> getGPSPoints(long time) {
        SQLiteDatabase tmpdb = SQLiteDatabase.openDatabase(Constants.kDBFolder + String.valueOf(time).concat(".db"), null, SQLiteDatabase.OPEN_READONLY);
        List<Trace.Trip> res = new ArrayList<Trace.Trip>();
        String selectQuery = "SELECT  * FROM " + TABLE_TRACE + " WHERE type = '" + Trace.Trip.class.getSimpleName() + "'";
        Cursor cursor = tmpdb.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            String value = cursor.getString(cursor.getColumnIndex("value"));
            TraceMessage tm = GsonSingleton.fromJson(value, TraceMessage.class);
            if(tm.value instanceof Trace.Trip)
                res.add((Trace.Trip) tm.value);
        } while (cursor.moveToNext());
        tmpdb.close();
        return res;
    }

    private Trip constructTripByCursor(Cursor cursor, boolean withgps) {
        long stime = cursor.getLong(0);
        long etime = cursor.getLong(1);
        double dist = cursor.getDouble(2);
        double score = cursor.getDouble(3);
        int deleted = cursor.getInt(4);
        Trip trip = new Trip(stime);
        trip.setScore(score);
        trip.setStatus(deleted == 1? 0 : 1);
        trip.setEndTime(etime);
        trip.setDistance(dist);
        if(withgps) {
            trip.setGPSPoints(this.getGPSPoints(stime));
        }

        return trip;
    }

    public Trip getTrip(long time) {
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE starttime = " + time + ";";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        Trip trip = null;
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            trip = constructTripByCursor(cursor, true);
        } while (cursor.moveToNext());
        return trip;
    }

    /**
     * @brief remove the record of the table, so that the user cannot see it
     * but the file is still in the database
     * @param time
     */
    public void removeTrip(long time) {
        ContentValues data = new ContentValues();
        data.put("deleted", 1);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        meta_.update(TABLE_META, data, where, whereArgs);
    }

    /**
     * @  only if the trip is imcomplete
     * @param time
     */

    public void deleteTrip(long time) {
        Log.d(TAG, "deleteTrip:" + time);
        SQLiteDatabase.deleteDatabase(new File(Constants.kDBFolder + String.valueOf(time).concat(".db")));
    }


    public List<Trip> loadTrips() {
        UserObject user = this.getCurrentUser();
        List<Trip> trips = new ArrayList<Trip>();
        String selectQuery = "";
        if(user == null) {
            selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE email = '"+"' order by starttime desc;";
        } else {
            selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE email = '" + user.email_ + "' or email = '"+"' order by starttime desc;";
        }
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return trips;
        }
        do {
            int deleted = cursor.getInt(4);
            if(deleted >= 1) {
                continue;
            }
            Trip trip = constructTripByCursor(cursor, false);
            trips.add(trip);
            if(trips.size() >= Constants.kNumberOfTripsDisplay) {
                break;
            }
        } while (cursor.moveToNext());
        return trips;
    }


    public long[] tripsToSynchronize (String useremail) {
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE uploaded = 1 and deleted = 1 and " + " email = '" + useremail + "';";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return null;
        }
        long [] stime = new long[cursor.getCount()];
        int i = 0;
        do {
            stime[i++] = cursor.getLong(0);
        } while (cursor.moveToNext());
        return stime;
    }
    public int tripSynchronizeDone(long time) {
        ContentValues data = new ContentValues();
        data.put("deleted", 2);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        return meta_.update(TABLE_META, data, where, whereArgs);
    }
    /**
     * all about uploading
     */
    public long nextTripToUpload(String useremail) {
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE uploaded = 0 and " + " email = '" + useremail + "';";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        long stime = -1;
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            stime = cursor.getLong(0);
            break;
        } while (cursor.moveToNext());
        return stime;
    }


    public void tripRemoveSensorData(long time) {
        String [] tables = {TABLE_TRACE};
        SQLiteDatabase tmpdb = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + String.valueOf(time).concat(".db"), null, null);
        for(int i = 0; i < tables.length; ++i) {
            String dropsql = "DROP TABLE IF EXISTS " + tables[i] + ";";
            tmpdb.execSQL(dropsql);
        }
        tmpdb.close();
    }
    /**
     * label the meta table that the trip has been uploaded, and remove all the sensor tables, leave gps table
     * @param time
     */
    public int tripUploadDone(long time) {
        Log.d(TAG, "tripUploadDone");

        //drop the sensor tables to avoid space waste
        tripRemoveSensorData(time);

        //update information in meta table
        ContentValues data = new ContentValues();
        data.put("uploaded", 1);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        return meta_.update(TABLE_META, data, where, whereArgs);
    }



    //email TEXT, firstname TEXT, lastname TEXT, loginstatus INTEGER
    /* ========================== UserObject Specific Database Operations =================================== */

    /**
     * Create a new user and log them in immediately
     * @param email UserObject's email address
     * @param firstname UserObject's first name
     * @param lastname UserObject's last name
     * @return Success or failure creating the user
     */
    public boolean newUser(String email, String firstname, String lastname) {
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("firstname", firstname);
        values.put("lastname", lastname);
        values.put("loginstatus", 1);
        meta_.insert(TABLE_USER, null, values);
        return true;
    }
    public UserObject getCurrentUser() {
        UserObject user = null;
        String selectQuery = "SELECT  email, firstname, lastname, loginstatus FROM " + TABLE_USER + " WHERE loginstatus = 1;";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            user = new UserObject();
            user.email_ = cursor.getString(0);
            user.firstname_ = cursor.getString(1);
            user.lastname_ = cursor.getString(2);
            user.loginstatus_ = cursor.getInt(3);
            break;
        } while (cursor.moveToNext());
        return user;
    }

    public boolean hasUser(String email) {
        String selectQuery = "SELECT  * FROM " + TABLE_USER + " WHERE email like '" + email + "'";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) return false;
        else return true;
    }

    public boolean userLogin(String email) {
        if(!hasUser(email)) {
            return false;
        }
        Log.d(TAG, "user login processing in database");
        ContentValues data = new ContentValues();
        data.put("loginstatus", 1);
        String where = "email = ? ";
        String[] whereArgs = {email};
        try {
            meta_.update(TABLE_USER, data, where, whereArgs);
            return true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }


    }

    public boolean userLogout() {
        Log.d(TAG, "user logout processing in database");
        ContentValues data = new ContentValues();
        data.put("loginstatus", 0);
        try {
            meta_.update(TABLE_USER, data, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
