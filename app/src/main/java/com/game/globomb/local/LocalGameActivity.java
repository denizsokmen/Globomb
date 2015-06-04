package com.game.globomb.local;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.game.globomb.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.SupportMapFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

public class LocalGameActivity extends FragmentActivity {
    private BluetoothClient client;
    private BluetoothServer server;
    private boolean isHost;
    private boolean isLocal;

    public HashMap<String, BluetoothPacket> listeners = new HashMap<String, BluetoothPacket>();
    public HashMap<String, LocalPlayer> playerMap = new HashMap<String, LocalPlayer>();
    public String selfPlayer;
    public String playerName;

    private BluetoothDevice device;
    private Handler handler;

    private Runnable runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Bundle extras = getIntent().getExtras();
        device = null;
        if (extras != null) {
            playerName = extras.getString("name", "generic player");
            device = (BluetoothDevice) extras.get("device");
            isLocal = extras.getBoolean("local", false);
            isHost = extras.getBoolean("host", false);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        startConnection();
    }


    @Override
    public void onResume() {
        super.onResume();

    }

    private void hostGame() {
//        server = new BluetoothServer(this);
//        server.start();
//
//        OnlinePlayer ply = new OnlinePlayer(this);
//        playerMap.put("host", ply);
//        ply.identifier = "host";
//        ply.latitude = 0;
//        ply.longitude = 0;
//        ply.bomb = true;
//        ply.name = "asd";
//        ply.update();
//        selfPlayer = "host";
//
        handler = new Handler();
        runnable = new Runnable()
        {
            public void run()
            {
                sendGamestate();
                handler.postDelayed(this, 1000);
            }
        };
//
        runnable.run();
    }

    public void receivePacket(String packet, JSONObject data) {
        BluetoothPacket pkt = listeners.get(packet);
        if (pkt != null)
            pkt.onReceive(data);
        else
            Log.e("error", "Invalid packet: " + packet + " data: " + data.toString());
    }

    private void startConnection() {

        listeners.put("initialize", new InitializeListener(this));
        listeners.put("kick", new KickListener(this));
        listeners.put("explode", new ExplodeListener(this));
        listeners.put("gamestate", new GameStateListener(this));

        if (isLocal) {
            if (isHost) {
                hostGame();
            }
            else {
                client = new BluetoothClient(device, this);
                client.start();
            }
        }
    }

    public synchronized void sendPacket(String name, JSONObject packet) {
        if (isLocal && !isHost) {
            try {
                Log.e("packet send", packet.toString());
                packet.put("packet", name);
                DataOutputStream stream = new DataOutputStream(client.mmSocket.getOutputStream());
                byte[] bytes = packet.toString().getBytes(Charset.forName("UTF-8"));
                ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 2);
                buffer.putShort((short) bytes.length);
                buffer.put(bytes);
                stream.write(buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }







    @Override
    protected void onPause() {
        super.onPause();
    }


    public void showToast(String text, int duration){
//        Context context = getApplicationContext();
//        Toast.makeText(context, text, duration).show();
    }


    public void sendGamestate() {

        try {
            JSONObject gamestate = new JSONObject();
            JSONArray players = new JSONArray();
            /*for (HashMap.Entry<String, OnlinePlayer> entry : playerMap.entrySet()) {
                OnlinePlayer ply = entry.getValue();
                JSONObject playerObj = new JSONObject();

                playerObj.put("identifier", ply.identifier);
                playerObj.put("latitude", ply.latitude);
                playerObj.put("longitude", ply.longitude);
                playerObj.put("name", ply.name);
                playerObj.put("bomb", ply.bomb);
                players.put(playerObj);

            }*/

            gamestate.put("players", players);
            gamestate.put("time", 10);
            server.broadcast("gamestate", gamestate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
