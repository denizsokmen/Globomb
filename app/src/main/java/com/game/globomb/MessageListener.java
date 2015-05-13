package com.game.globomb;

import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tdgunes on 12/05/15.
 */
public class MessageListener implements Emitter.Listener {
    private final String TAG = "MessageListener";
    private final GameActivity activity;

    MessageListener(GameActivity activity){
        this.activity = activity;
    }

    @Override
    public void call(final Object... args) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, args[0].toString());
                JSONObject data = (JSONObject) args[0];
                String packet;
                try {
                    packet = data.getString("packet");
                    if (packet.equals("initialize")) {
                        int selfid = data.getInt("selfid");

                    }
                    else if (packet.equals("gamestate")) {
                        JSONArray players = data.getJSONArray("players");
                        for(int i = 0; i < players.length(); i++) {
                            JSONObject player = players.getJSONObject(i);
                            int playerid = player.getInt("identifier");
                            double longitude = player.getDouble("longitude");
                            double latitude = player.getDouble("latitude");
                            boolean bomb = player.getBoolean("bomb");
                            String name = player.getString("name");
                        }


                    }
                    else if(packet.equals("kick")) {
                        int playerid = data.getInt("playerid");

                    }
                }
                catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }
                //activity.showToast(username+" says '"+message+"'.", Toast.LENGTH_LONG);
            }
        });

    }




}
