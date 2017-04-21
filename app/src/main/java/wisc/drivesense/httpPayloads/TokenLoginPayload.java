package wisc.drivesense.httpPayloads;

/**
 * Created by peter on 10/28/16.
 */

public class TokenLoginPayload {
    //from google
    public String id_token;
    //from facebook
    public String access_token;

    //Response: jwt token from drivesense server
    public String token;
}