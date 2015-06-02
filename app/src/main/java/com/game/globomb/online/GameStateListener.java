package com.game.globomb.online;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tdgunes on 02/06/15.
 */
public class GameStateListener implements Emitter.Listener {
    private final String TAG = "GameStateListener";
    private final OnlineGameActivity activity;

    GameStateListener(OnlineGameActivity activity){
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
                        JSONObject playerJSON = players.getJSONObject(i);
                        String identifier = playerJSON.getString("identifier");
                        double longitude = playerJSON.getDouble("longitude");
                        double latitude = playerJSON.getDouble("latitude");
                        boolean bomb = playerJSON.getBoolean("bomb");
                        String name = playerJSON.getString("name");

                        OnlinePlayer player = activity.playerMap.get(identifier);
                        if (player == null) {
                            player = new OnlinePlayer(activity);
                            activity.playerMap.put(identifier, player);

                        }
                        player.update(identifier, name, latitude, longitude, bomb);


                    }
                    String playerCount = ""+players.length();
                    String time = ""+(60-data.getInt("time"));
                    activity.playerView.setText(playerCount);
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
