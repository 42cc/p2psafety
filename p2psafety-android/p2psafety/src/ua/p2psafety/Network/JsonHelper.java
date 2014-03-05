package ua.p2psafety.Network;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ua.p2psafety.Event;
import ua.p2psafety.User;
import ua.p2psafety.roles.Role;

public class JsonHelper {
    public static List<Event> jsonResponseToEvents(String json) {
        List<Event> events = new ArrayList<Event>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(json, Map.class);
            List<Map> objects = (ArrayList) data.get("objects");

            // deserializing json response
            for (Map jsonMap : objects) {
                events.add(JsonHelper.jsonToEvent(jsonMap));
            }

            data.clear();
            objects.clear();
        } catch (Exception e) {
            // ignore
            NetworkManager.LOGS.error("Can't get object from data", e);
        }

        return events;
    }

     public static Event jsonToEvent(Map data) {
        String TAG = "jsonToEvent";
        Event event = null;

        try {
            event = new Event();
            event.setId       (String.valueOf( data.get("id")));
            event.setKey      (String.valueOf( data.get("key")));
            event.setUri      (String.valueOf( data.get("resource_uri")));
            event.setStatus   (String.valueOf( data.get("status")));
            event.setType     (String.valueOf( data.get("type")));

            // parse user
            Map user_data = (Map) data.get("user");
            event.setUser(jsonToUser(user_data));
            // parse location
            Map loc_data = (Map) data.get("location");
            Location loc = new Location("");
            String lat = String.valueOf(loc_data.get("latitude"));
            String lon = String.valueOf(loc_data.get("longitude"));
            Log.i(TAG, "lat: " + lat + "  lon: " + lon);
            loc.setLatitude(Double.valueOf(lat));
            loc.setLongitude(Double.valueOf(lon));
            event.setLocation(loc);

            Log.i(TAG, "id: "       + event.getId());
            Log.i(TAG, "key: "      + event.getKey());
            Log.i(TAG, "uri: "      + event.getUri());
            Log.i(TAG, "status: "   + event.getStatus());
            Log.i(TAG, "type: "     + event.getType());
            Log.i(TAG, "location: " + event.getLocation());
        } catch (Exception e) {
            NetworkManager.LOGS.error("Can't get object from data", e);
        }

        return event;
    }

    //{"id": 14, "latest_location": null,
//        "latest_update": null, "resource_uri": "/api/v1/events/14/", "status": "P",
//        "user": {"full_name": "Oleg Ovcharenko", "id": 3, "resource_uri": ""}}

    public static User jsonToUser(Map data) {
        String TAG = "jsonToUser";
        User user = null;

        try {
            user = new User();
            user.setId          (String.valueOf( data.get("id")));
            user.setFullName    (String.valueOf( data.get("full_name")));
            user.setUri         (String.valueOf( data.get("resource_uri")));

            Log.i(TAG, "id: "           + user.getId());
            Log.i(TAG, "fullName: "     + user.getUsername());
            Log.i(TAG, "uri: "          + user.getUri());

        } catch (Exception e) {
            NetworkManager.LOGS.error("Can't get object from data", e);
        }

        return user;
    }

    public static List<Role> jsonResponseToRoles(String json) {
        List<Role> roles = new ArrayList<Role>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(json, Map.class);
            List<Map> objects = (ArrayList) data.get("objects");

            // deserializing json response
            for (Map jsonMap : objects) {
                roles.add(JsonHelper.jsonToRole(jsonMap));
            }

            data.clear();
            objects.clear();
        } catch (Exception e) {
            // ignore
            NetworkManager.LOGS.error("Can't get object from data", e);
        }

        return roles;
    }

    public static Role jsonToRole(Map data) {
        String TAG = "jsonToRole";
        Role role = null;

        try {
            role = new Role();
            role.id = String.valueOf( data.get("id"));
            role.name = (String.valueOf(data.get("name")));

            Log.i(TAG, "id: "           + role.id);
            Log.i(TAG, "fullName: "     + role.name);

        } catch (Exception e) {
            NetworkManager.LOGS.error("Can't get object from data", e);
        }

        return role;
    }


}
