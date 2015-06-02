package com.game.globomb.online;

import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class OnlinePlayer {

    public String identifier;

    public double longitude;
    public double latitude;
    public boolean bomb;
    public boolean you = false;
    public String name;
    public Marker marker;
    private OnlineGameActivity game;

    private boolean firstTimeUpdate = true;
    private boolean mustShowInfoWindow = false;
    public OnlinePlayer(OnlineGameActivity game) {
        marker = game.map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("You are here!"));
        this.game = game;


    }

    public void update(String identifier, String name, double latitude,
                       double longitude, boolean bomb) {
        this.identifier = identifier;
        this.latitude = latitude;
        this.longitude = longitude;
        if (this.bomb == false && bomb) {
            mustShowInfoWindow = true;
        }

        this.bomb = bomb;
        this.name = name;

        this.update();
    }
    public void update() {

        if (firstTimeUpdate){
            if (name.equalsIgnoreCase(game.playerName)){
                you = true;
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                marker.setTitle("You");
            }
            else {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                marker.setTitle(name);
            }
            firstTimeUpdate = false;
        }

        marker.setPosition(new LatLng(latitude, longitude));

        if (bomb) {
            if (you) {
                this.game.player = this;
                this.game.gloBombImage.setAlpha(1.0f);
                this.game.bomb = bomb;
                marker.setSnippet("have the bomb!");

            }
            else {
                marker.setSnippet("has the bomb!");
            }

        }
        else {

            if (you) {
                this.game.player = this;
                this.game.bomb = bomb;
                this.game.gloBombImage.setAlpha(0.1f);
                marker.setSnippet("don't have the bomb!");
            }
            else {
                marker.setSnippet("doesn't have the bomb!");
            }

        }

        if (mustShowInfoWindow) {
            marker.showInfoWindow();
            mustShowInfoWindow = false;
        }
    }
}
