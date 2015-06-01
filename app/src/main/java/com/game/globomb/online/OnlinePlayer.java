package com.game.globomb.online;

import com.game.globomb.local.LocalGameActivity;
import com.game.globomb.online.OnlineGameActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by deniz on 13/05/15.
 */
public class OnlinePlayer {

    public String identifier;
    public double longitude;
    public double latitude;
    public String name;
    public boolean bomb;
    public Marker marker;

    public OnlinePlayer(OnlineGameActivity game) {
        marker = game.mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("You are here!"));
    }

    public void update() {
        marker.setPosition(new LatLng(latitude, longitude));
        marker.setTitle(name);
    }
}
