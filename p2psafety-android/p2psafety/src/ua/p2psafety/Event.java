package ua.p2psafety;

public class Event {
    public static String STATUS_PASSIVE  = "P";
    public static String STATUS_ACTIVE   = "A";
    public static String STATUS_FINISHED = "F"; // currently not in use

    public static String TYPE_VICTIM = "victim";
    public static String TYPE_SUPPORT = "support";

    String mId;
    String mKey;
    String mUri;
    String mStatus;
    String mType;
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

    public void setType(String type) {
        mType = type;
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

    public String getType() {
        return mType;
    }

    public String getStatus() {
        return mStatus;
    }

    public User getUser() {
        return mUser;
    }
}
