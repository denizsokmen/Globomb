package com.game.globomb.local;

import android.util.Log;
import android.widget.Toast;

import com.game.globomb.online.OnlineGameActivity;
import com.game.globomb.online.OnlinePlayer;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class KickListener implements BluetoothPacket {
    private final String TAG = "KickListener";
    private final LocalGameActivity activity;

    KickListener(LocalGameActivity activity){
        this.activity = activity;
    }


    public void onReceive(JSONObject data) {

    }


    public void call(final Object... args) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, args[0].toString());
                JSONObject data = (JSONObject) args[0];
                try {
                    String playerid = data.getString("identifier");

                    /*OnlinePlayer ply = activity.playerMap.get(playerid);
                    if (ply != null) {
                        activity.playerMap.remove(playerid);
                        ply.marker.remove();
                    }
                    Toast.makeText(activity, ply.name+" is disconnected", Toast.LENGTH_SHORT).show();*/
                }
                catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }
            }
        });

    }
}
