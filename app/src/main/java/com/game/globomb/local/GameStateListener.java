package com.game.globomb.local;

import android.util.Log;
import android.widget.Toast;

import com.game.globomb.online.OnlineGameActivity;
import com.game.globomb.online.OnlinePlayer;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tdgunes on 02/06/15.
 */
public class GameStateListener implements BluetoothPacket {
    private final String TAG = "GameStateListener";
    private final LocalGameActivity activity;

    GameStateListener(LocalGameActivity activity){
        this.activity = activity;
    }

    public void onReceive(JSONObject data) {
        try {
            boolean selfbomb = activity.playerMap.get(activity.selfPlayer).bomb;
            JSONArray players = data.getJSONArray("players");
            for(int i = 0; i < players.length(); i++) {
                JSONObject playerJSON = players.getJSONObject(i);
                String identifier = playerJSON.getString("identifier");
                double longitude = playerJSON.getDouble("longitude");
                double latitude = playerJSON.getDouble("latitude");
                boolean bomb = playerJSON.getBoolean("bomb");
                String name = playerJSON.getString("name");

                LocalPlayer player = activity.playerMap.get(identifier);
                if (player == null) {
                    player = new LocalPlayer(activity);
                    activity.playerMap.put(identifier, player);
                }
                player.identifier = identifier;
                player.bomb = bomb;
                player.name = name;
                player.update();
            }
            String playerCount = ""+players.length();
            String time = ""+(60-data.getInt("time"));
            activity.timeText.setText(time);
            activity.updatePlayers();
            if (!selfbomb && activity.playerMap.get(activity.selfPlayer).bomb) {
                Toast.makeText(activity, "You got the bomb!", Toast.LENGTH_SHORT).show();
            }
        }
        catch (JSONException e) {
            Log.v(TAG, "Unable to parse: " + data);
        }

    }
}
