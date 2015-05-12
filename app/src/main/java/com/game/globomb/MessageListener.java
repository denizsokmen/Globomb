package com.game.globomb;

import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tdgunes on 12/05/15.
 */
public class MessageListener implements Emitter.Listener {

    private final MainActivity activity;

    MessageListener(MainActivity activity){
        this.activity = activity;
    }

    @Override
    public void call(final Object... args) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                JSONObject data = (JSONObject) args[0];
                String username;
                String message;
                try {
                    username = data.getString("username");
                    message = data.getString("message");
                }
                catch (JSONException e) {
                    Log.v("MessageListener", "Unable to parse: " + data);
                    return;
                }
                activity.showToast(username+" says '"+message+"'.", Toast.LENGTH_SHORT);
            }
        });

    }




}
