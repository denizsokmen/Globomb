package com.game.globomb.local;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class LocalGameActivity extends FragmentActivity {
    public BluetoothClient client;
    private BluetoothServer server;
    private boolean isHost;
    private boolean isLocal;

    public HashMap<String, BluetoothPacket> listeners = new HashMap<String, BluetoothPacket>();
    public HashMap<String, LocalPlayer> playerMap = new HashMap<String, LocalPlayer>();
    public String selfPlayer;
    public String playerName;
    public Random random = new Random();

    public ArrayList<LocalPlayer> players = new ArrayList<LocalPlayer>();

    private BluetoothDevice device;
    private Handler handler;

    private Runnable runnable;

    public TextView timeText;
    public ListView playerList;

    public long time = 0;
    private ListAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        timeText = (TextView) findViewById(R.id.timetext);
        playerList = (ListView) findViewById(R.id.playerlist);

        adapter = new ListAdapter(this, R.layout.player_row, players);
        playerList.setAdapter(adapter);

        playerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocalPlayer entry = players.get(position);
                LocalPlayer self = playerMap.get(selfPlayer);

                if (!entry.equals(self) && self.bomb) {
                    entry.bomb = true;
                    self.bomb = false;
                    JSONObject bombjson = new JSONObject();
                    try {
                        Toast.makeText(LocalGameActivity.this, entry.identifier, Toast.LENGTH_SHORT).show();
                        Log.e("identify: ", entry.identifier);
                        bombjson.put("identifier", entry.identifier);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendPacket("bomb", bombjson);
                }
            }
        });

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
    protected void onStop() {
        super.onStop();
        if (isHost)
            handler.removeCallbacks(runnable);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private void hostGame() {
        server = new BluetoothServer(this);
        server.start();

        LocalPlayer ply = new LocalPlayer(this);
        playerMap.put("host", ply);
        ply.identifier = "host";
        ply.latitude = 0;
        ply.longitude = 0;
        ply.bomb = true;
        ply.name = playerName;
        ply.update();
        selfPlayer = "host";

        handler = new Handler();
        runnable = new Runnable()
        {
            public void run()
            {
                time++;
                sendGamestate();

                if (time == 0) {
                    JSONObject explodepacket = new JSONObject();
                    JSONObject kickpacket = new JSONObject();
                    for (HashMap.Entry<String, LocalPlayer> entry : playerMap.entrySet()) {
                        LocalPlayer ply = entry.getValue();
                        if (ply.bomb) {
                            try {
                                explodepacket.put("identifier", ply.identifier);
                                kickpacket.put("identifier", ply.identifier);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        server.sendPacket(ply.socket, "kick", kickpacket);
                    }
                    server.broadcast("explode", explodepacket);
                    randomBomb();
                }


                handler.postDelayed(this, 1000);
            }
        };
        runnable.run();
    }

    public void receivePacket(String packet, JSONObject data) {
        BluetoothPacket pkt = listeners.get(packet);
        if (pkt != null)
            pkt.onReceive(data);
        else
            Log.e("error", "Invalid packet: " + packet + " data: " + data.toString());
    }

    public synchronized void onConnectionSuccessful() {
        JSONObject object = new JSONObject();
        try {
            object.put("name", playerName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendPacket("acknowledge", object);
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
        if (isLocal) {
            if (!isHost) {
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
            else {
                //send to self
                receivePacket(name, packet);
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


    public void updatePlayers() {
        players.clear();
        players.addAll(playerMap.values());
        adapter.notifyDataSetChanged();
    }

    public void randomBomb() {
        for (HashMap.Entry<String, LocalPlayer> entry : playerMap.entrySet()) {
            entry.getValue().bomb = false;
        }

        LocalPlayer ply = players.get(random.nextInt(players.size()));
        ply.bomb = true;
        sendGamestate();
    }


    public void sendGamestate() {
        try {
            JSONObject gamestate = new JSONObject();
            JSONArray players = new JSONArray();
            for (HashMap.Entry<String, LocalPlayer> entry : playerMap.entrySet()) {
                LocalPlayer ply = entry.getValue();
                JSONObject playerObj = new JSONObject();
                playerObj.put("identifier", ply.identifier);
                playerObj.put("latitude", ply.latitude);
                playerObj.put("longitude", ply.longitude);
                playerObj.put("name", ply.name);
                playerObj.put("bomb", ply.bomb);
                players.put(playerObj);
            }
            time = time%60;
            gamestate.put("players", players);
            gamestate.put("time", time);

            timeText.setText(""+(60-time));
            updatePlayers();
            server.broadcast("gamestate", gamestate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class ListAdapter extends ArrayAdapter<LocalPlayer> {

        public ListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public ListAdapter(Context context, int resource, List<LocalPlayer> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.player_row, null);
            }

            LocalPlayer p = getItem(position);

            if (p != null) {
                TextView nam = (TextView) v.findViewById(R.id.titleText);
                if (nam != null) {
                    nam.setText(p.name);
                }

                TextView bom = (TextView) v.findViewById(R.id.bombText);
                if (bom != null) {
                    bom.setText(p.bomb ? "HAS BOMB" : "");
                    bom.setTextColor(p.bomb ? 0x0000FFFF : 0x000000FF);
                }
            }
            return v;
        }
    }
}
