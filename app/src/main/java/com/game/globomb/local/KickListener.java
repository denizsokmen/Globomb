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
        try {
            String playerid = data.getString("identifier");

            LocalPlayer ply = activity.playerMap.get(playerid);
            if (ply != null) {
                if (ply.identifier.equals(activity.selfPlayer)) {
                    activity.client.cancel();
                    activity.finish();
                }
                else {
                    activity.playerMap.remove(playerid);
                }
            }
        }
        catch (JSONException e) {
            Log.v(TAG, "Unable to parse: " + data);
            return;
        }
    }
}
