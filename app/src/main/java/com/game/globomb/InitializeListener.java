package com.game.globomb;

import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InitializeListener implements Emitter.Listener {
    private final String TAG = "MessageListener";
    private final GameActivity activity;

    InitializeListener(GameActivity activity){
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
                    JSONObject player = data.getJSONObject("player");
                    String playerid = player.getString("identifier");
                    double longitude = player.getDouble("longitude");
                    double latitude = player.getDouble("latitude");
                    boolean bomb = player.getBoolean("bomb");
                    String name = player.getString("name");

                    Player ply = new Player(activity);
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
                    return;
                }
            }
        });

    }
}
