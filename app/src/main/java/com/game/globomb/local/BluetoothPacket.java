package com.game.globomb.local;

import org.json.JSONObject;

/**
 * Created by Deniz on 3.6.2015.
 */
public interface BluetoothPacket {

    public void onReceive(JSONObject data);
}
