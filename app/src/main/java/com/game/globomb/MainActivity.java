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
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import java.net.URISyntaxException;


public class MainActivity extends ActionBarActivity {
    private final String TAG = "MainActivity";

    private Socket socket;
    {
        try {
            socket = IO.socket("http://localhost:8080");
        } catch (URISyntaxException e) {
            Log.e(TAG,"Unable to connect to host!");
        }
    }
    private final MessageListener messageListener = new MessageListener(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        socket.connect();
        socket.on("message", messageListener);

        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.enterbutton);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        socket.disconnect();
        socket.on("message", messageListener);
    }

}
