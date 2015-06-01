package com.game.globomb.online;

import android.util.Log;
import android.widget.Toast;

import com.game.globomb.local.LocalGameActivity;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tdgunes on 12/05/15.
 */
public class MessageListener implements Emitter.Listener {
    private final String TAG = "MessageListener";
    private final OnlineGameActivity activity;

    MessageListener(OnlineGameActivity activity){
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
                    String username = data.getString("username");
                    String message = data.getString("message");
                    Toast.makeText(activity.getApplicationContext(), username+" says "+message, Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    Log.v(TAG, "Unable to parse: " + data);
                    return;
                }


            }
        });

    }



}
