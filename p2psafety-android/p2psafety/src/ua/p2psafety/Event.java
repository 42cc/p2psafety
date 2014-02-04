package ua.p2psafety;

public class Event {
    public static String STATUS_PASSIVE  = "P";
    public static String STATUS_ACTIVE   = "A";
    public static String STATUS_FINISHED = "F";

    String mId;
    String mKey;
    String mUri;
    String mStatus;
    User mUser;

    public void setId(String id) {
        mId = id;
    }

    public void setKey(String key) {
        mKey = key;
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public void setUser(User user) {
        mUser = user;
    }

    public String getId() {
        return mId;
    }

    public String getKey() {
        return mKey;
    }

    public String getUri() {
        return mUri;
    }

    public String getStatus() {
        return mStatus;
    }

    public User getUser() {
        return mUser;
    }
}
