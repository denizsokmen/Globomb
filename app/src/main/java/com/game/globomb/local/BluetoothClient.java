package com.game.globomb.local;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import com.game.globomb.online.OnlinePlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by Deniz on 31.5.2015.
 */
public class BluetoothClient extends Thread {
    public final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public final BluetoothSocket mmSocket;
    public final BluetoothDevice mmDevice;

    public OutputStream outStream;
    public InputStream inStream;
    private BluetoothAdapter mBluetoothAdapter;
    private LocalGameActivity activity;

    public BluetoothClient(BluetoothDevice device , LocalGameActivity activity) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.activity = activity;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        boolean end = false;
        byte[] messageLength = new byte[1000];
        String messageString = "";

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            connectException.printStackTrace();
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            closeException.printStackTrace();
            }
            return;
        }

        try {
            outStream = mmSocket.getOutputStream();
            inStream = mmSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject object = new JSONObject();
        try {
            object.put("name", "asdf");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        activity.sendPacket("acknowledge", object);


        DataInputStream in = new DataInputStream(inStream);

        while(true) {

            try {

                int bytesRead = 0;

                messageLength[0] = in.readByte();
                messageLength[1] = in.readByte();
                ByteBuffer byteBuffer = ByteBuffer.wrap(messageLength, 0, 2);

                int bytesToRead = byteBuffer.getShort();

                while (!end) {
                    bytesRead = in.read(messageLength);
                    messageString += new String(messageLength, 0, bytesRead);
                    Log.e("packread", messageString);
                    if (messageString.length() == bytesToRead) {
                        end = true;
                    }
                }

                //A full packet is read, do something with it
                activity.runOnUiThread(new PacketHandler(messageString));
                messageString="";
                end = false;

            }catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }


    //this will be run on UI thread
    public void processPacket(String packet) {
        try {
            JSONObject obj = new JSONObject(packet);
            String id = obj.getString("packet");
            Toast.makeText(activity, "Got packet: "+id, Toast.LENGTH_LONG).show();
            Log.e("info", obj.toString());
            /*gameSocket.on("message", messageListener);
            gameSocket.on("initialize", initializeListener);
            gameSocket.on("kick", kickListener);
            gameSocket.on("gamestate", gamestateListener);
            gameSocket.on("explode", explodeListener);*/

            if (id.equals("initialize")) {
                JSONObject player = obj.getJSONObject("player");
                String playerid = player.getString("identifier");
                double longitude = player.getDouble("longitude");
                double latitude = player.getDouble("latitude");
                boolean bomb = player.getBoolean("bomb");
                String name = player.getString("name");

                LocalPlayer ply = new LocalPlayer(activity);
                activity.playerMap.put(playerid, ply);
                ply.identifier = playerid;
                ply.latitude = latitude;
                ply.longitude = longitude;
                ply.bomb = bomb;
                ply.name = name;
                ply.update();
                activity.selfPlayer = playerid;
            }
            else if (id.equals("kick")) {
                String playerid = obj.getString("identifier");
                Toast.makeText(activity, "OnlinePlayer disconnected", Toast.LENGTH_SHORT).show();
                LocalPlayer ply = activity.playerMap.get(playerid);
                if (ply != null) {
                    activity.playerMap.remove(playerid);
                    ply.marker.remove();
                }
            }
            else if (id.equals("gamestate")) {
                JSONArray players = obj.getJSONArray("players");
                for(int i = 0; i < players.length(); i++) {
                    JSONObject player = players.getJSONObject(i);
                    String playerid = player.getString("identifier");
                    double longitude = player.getDouble("longitude");
                    double latitude = player.getDouble("latitude");
                    boolean bomb = player.getBoolean("bomb");
                    String name = player.getString("name");

                    LocalPlayer ply = activity.playerMap.get(playerid);
                    if (ply == null) {
                        ply = new LocalPlayer(activity);
                        activity.playerMap.put(playerid, ply);

                    }
                    ply.identifier = playerid;
                    ply.latitude = latitude;
                    ply.longitude = longitude;
                    ply.bomb = bomb;
                    ply.name = name;
                    ply.update();
                }
                int number = obj.getInt("time");
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    private class PacketHandler implements Runnable {
        private String data;
        public PacketHandler(String _data) {
            this.data = _data;
        }

        public void run() {
            processPacket(data);
        }
    }
}