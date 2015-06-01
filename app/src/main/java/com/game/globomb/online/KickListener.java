package com.game.globomb.online;

import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class KickListener implements Emitter.Listener {
    private final String TAG = "KickListener";
    private final OnlineGameActivity activity;

    KickListener(OnlineGameActivity activity){
        this.activity = activity;
    }

    @Override
    public void call(final Object... args) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, args[0].toString());
                JSONObject data = (JSONObject) args[0];
                try {
                    String playerid = data.getString("identifier");
                    Toast.makeText(activity, "OnlinePlayer disconnected", Toast.LENGTH_SHORT).show();
                    OnlinePlayer ply = activity.playerMap.get(playerid);
                    if (ply != null) {
                        activity.playerMap.remove(playerid);
                        ply.marker.remove();
                    }
                }
                catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }
            }
        });

    }
}
