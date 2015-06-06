package com.game.globomb.local;

import android.util.Log;

import com.game.globomb.online.OnlineGameActivity;
import com.game.globomb.online.OnlinePlayer;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class InitializeListener implements BluetoothPacket {
    private final String TAG = "InitializeListener";
    private final LocalGameActivity activity;

    InitializeListener(LocalGameActivity activity){
        this.activity = activity;
    }

    public void onReceive(JSONObject data) {
        try {
            JSONObject player = data.getJSONObject("player");
            String playerid = player.getString("identifier");
            double longitude = player.getDouble("longitude");
            double latitude = player.getDouble("latitude");
            boolean bomb = player.getBoolean("bomb");
            String name = player.getString("name");

            LocalPlayer ply = new LocalPlayer(activity);
            activity.playerMap.put(playerid, ply);
            ply.identifier = playerid;
            ply.latitude = latitude;
            ply.longitude = longitude;
            ply.bomb = bomb;
            ply.name = name;
            ply.update();
            activity.selfPlayer = playerid;
        }
        catch (JSONException e) {
            Log.v(TAG, "Unable to parse: " + data);
        }
    }

    public void call(final Object... args) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, args[0].toString());
                JSONObject data = (JSONObject) args[0];
                try {
                    JSONObject player = data.getJSONObject("player");
                    String playerid = player.getString("identifier");
                    double longitude = player.getDouble("longitude");
                    double latitude = player.getDouble("latitude");
                    boolean bomb = player.getBoolean("bomb");
                    String name = player.getString("name");

                    /*OnlinePlayer ply = new OnlinePlayer(activity);
                    activity.playerMap.put(playerid, ply);
                    ply.identifier = playerid;
                    ply.latitude = latitude;
                    ply.longitude = longitude;
                    ply.bomb = bomb;
                    ply.name = name;
                    ply.update();
                    activity.selfPlayer = playerid;*/
                }
                catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }
            }
        });

    }
}
