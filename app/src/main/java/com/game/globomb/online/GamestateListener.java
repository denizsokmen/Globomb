package com.game.globomb.online;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class GamestateListener implements Emitter.Listener {
    private final String TAG = "GameStateListener";
    private final OnlineGameActivity activity;

    GamestateListener(OnlineGameActivity activity){
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
                    JSONArray players = data.getJSONArray("players");
                    for(int i = 0; i < players.length(); i++) {
                        JSONObject player = players.getJSONObject(i);
                        String playerid = player.getString("identifier");
                        double longitude = player.getDouble("longitude");
                        double latitude = player.getDouble("latitude");
                        boolean bomb = player.getBoolean("bomb");
                        String name = player.getString("name");

                        OnlinePlayer ply = activity.playerMap.get(playerid);
                        if (ply == null) {
                            ply = new OnlinePlayer(activity);
                            activity.playerMap.put(playerid, ply);

                        }
                        ply.identifier = playerid;
                        ply.latitude = latitude;
                        ply.longitude = longitude;
                        ply.bomb = bomb;
                        ply.name = name;
                        ply.update();


                    }
                    String time = ""+(60-data.getInt("time"));
                    activity.timeView.setText(time);
                }
                catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }
            }
        });

    }
}
