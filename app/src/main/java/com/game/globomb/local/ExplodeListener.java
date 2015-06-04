package com.game.globomb.local;

import android.util.Log;

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

    }

    public void call(final Object... args) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, args[0].toString());
                JSONObject data = (JSONObject) args[0];
                try {
                    String playerid = data.getString("identifier");
                }
                catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }
            }
        });

    }
}
