package com.game.globomb.local;

import android.util.Log;
import android.widget.Toast;

import com.game.globomb.online.OnlineGameActivity;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class ExplodeListener implements BluetoothPacket {
    private final String TAG = "ExplodeListener";
    private final LocalGameActivity activity;

    ExplodeListener(LocalGameActivity activity){
        this.activity = activity;
    }

    public void onReceive(JSONObject data) {
        try {
            String playerid = data.getString("identifier");
            LocalPlayer player = activity.playerMap.get(playerid);
            if (player != null) {
                Toast.makeText(activity, player.name + " exploded!", Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException e) {
            Log.v(TAG, "Unable to parse: " + data);
            return;
        }
    }
}
