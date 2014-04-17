package ua.p2psafety.util;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Taras Melon on 03.04.14.
 */
public class MyLinkedHashMap extends LinkedHashMap<String, List<MarkerOptions>> {

    public void put(String key, MarkerOptions value, boolean isVictim) {
        List<MarkerOptions> list = super.get(key);
        if (list == null)
            list = new ArrayList<MarkerOptions>();
        if (!isVictim)
            value.icon(BitmapDescriptorFactory.defaultMarker(
                    BitmapDescriptorFactory.HUE_GREEN));
        boolean isInList = false;
        int i=0;
        while (!isInList && i<list.size()) {
            LatLng posInList = list.get(i).getPosition();
            LatLng newPos = value.getPosition();
            if (posInList.latitude == newPos.latitude && posInList.longitude == newPos.longitude)
                isInList = true;
            i++;
        }
        if (!isInList) {
            if (isVictim)
                for (MarkerOptions marker: list)
                    marker.alpha(0.5f);
            else
                list.clear();
            list.add(value);
        }

        super.put(key, list);
    }
}
