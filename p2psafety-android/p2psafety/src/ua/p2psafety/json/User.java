package ua.p2psafety.json;

public class User {
    String mId;
    String mUri;
    String mFullName;

    public void setId(String id) {
        mId = id;
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    public void setFullName(String full_name) {
        mFullName = full_name;
    }

    public String getId() {
        return mId;
    }

    public String getUri() {
        return mUri;
    }

    public String getUsername() {
        return mFullName;
    }
}
