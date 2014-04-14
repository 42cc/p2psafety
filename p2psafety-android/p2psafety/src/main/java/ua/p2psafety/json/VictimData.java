package ua.p2psafety.json;

import android.location.Location;

/**
 * Created by Taras Melon on 27.03.14.
 */
public class VictimData {

    private String supporterUrl;
    private Location location;
    private String name;
    private String lastComment;
    private Long radius;

    public VictimData() {}

    public VictimData(String supporterUrl, Location location, String name, String lastComment, Long radius) {
        this.supporterUrl = supporterUrl;
        this.location = location;
        this.name = name;
        this.lastComment = lastComment;
        this.radius = radius;
    }

    public String getSupporterUrl() {
        return supporterUrl;
    }

    public void setSupporterUrl(String supporterUrl) {
        this.supporterUrl = supporterUrl;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastComment() {
        return lastComment;
    }

    public void setLastComment(String lastComment) {
        this.lastComment = lastComment;
    }

    public Long getRadius() {
        return radius;
    }

    public void setRadius(Long radius) {
        this.radius = radius;
    }
}
