package wisc.drivesense.user;

import android.util.Base64;

import com.google.gson.Gson;

/**
 * Created by peter on 10/28/16.
 */


public class DriveSenseToken {
    public String firstname;
    public String lastname;
    public String email;
    public Integer userid;
    public String jwt;

    public static DriveSenseToken InstantiateFromJWT(String jwtString) {
        String body = new String(Base64.decode(jwtString.split("\\.")[1],Base64.DEFAULT));
        Gson parser = new Gson();
        DriveSenseToken tokenObj = parser.fromJson(body, DriveSenseToken.class);
        tokenObj.jwt = jwtString;
        return tokenObj;
    }
}