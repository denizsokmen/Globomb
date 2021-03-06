package com.game.globomb.online;

import android.util.Log;
import android.widget.Toast;

import com.game.globomb.local.LocalGameActivity;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class ExplodeListener implements Emitter.Listener {
    private final String TAG = "ExplodeListener";
    private final OnlineGameActivity activity;

    ExplodeListener(OnlineGameActivity activity){
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
                    String explodedUserName = activity.playerMap.get(playerid).name;
                    if (explodedUserName.equalsIgnoreCase(activity.playerName)){
                        Toast.makeText(activity.getApplicationContext(), "You got exploded!", Toast.LENGTH_LONG).show();
                        activity.timerHandler.postDelayed(activity.timerExecutor, 4 * 1000);
                    }
                    else {
                        Toast.makeText(activity.getApplicationContext(), explodedUserName+" got exploded!", Toast.LENGTH_LONG).show();
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
