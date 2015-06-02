package com.game.globomb.online;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class OnlinePlayer {

    public String identifier;

    public double longitude;
    public double latitude;
    public boolean bomb;

    public String name;
    public Marker marker;


    public OnlinePlayer(OnlineGameActivity game) {
        marker = game.map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("You are here!"));
    }

    public void update(String identifier, String name, double latitude,
                       double longitude, boolean bomb) {
        this.identifier = identifier;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bomb = bomb;
        this.name = name;

        this.update();
    }
    public void update() {
        marker.setPosition(new LatLng(latitude, longitude));
        marker.setTitle(name);
    }
}
