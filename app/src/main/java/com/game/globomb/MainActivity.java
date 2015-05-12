package com.game.globomb;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;


public class MainActivity extends ActionBarActivity {
    private final String TAG = "MainActivity";
    private final String SERVER_URL = "http://10.0.2.2:8080/"; //this is only for emulator


    private Socket gameSocket;


    private EditText editText;
    private MessageListener messageListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Log.v(TAG,"Connecting to: "+SERVER_URL);
            gameSocket = IO.socket(SERVER_URL);
        } catch (URISyntaxException e) {
            Log.v(TAG,"Unable to connect to host! \n"+e);
        }

        if (gameSocket != null ) {
            startConnection();
        }
        else {
            showToast("Unable to connect to server!", Toast.LENGTH_LONG);
        }
    }

    private void startConnection() {
        messageListener = new MessageListener(this);
        editText = (EditText) findViewById(R.id.editText);

        Log.v(TAG,"Starting...");


        gameSocket.connect();
        gameSocket.on("message", messageListener);


        Button btn = (Button) findViewById(R.id.enterbutton);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject object = new JSONObject();
                try {
                    object.put("name", editText.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.v(TAG, "Sending...: "+ object);
                gameSocket.emit("acknowledge", object);

                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });
    }


    public void showToast(String text, int duration){
        Context context = getApplicationContext();
        Toast.makeText(context, text, duration).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestroy()");
        gameSocket.disconnect();
        gameSocket.off("message", messageListener);
    }

}
