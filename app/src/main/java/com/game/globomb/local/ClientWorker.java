package com.game.globomb.local;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import com.game.globomb.online.OnlinePlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by Deniz on 31.5.2015.
 */
public class ClientWorker extends Thread {

    private BluetoothServer server;
    private BluetoothSocket socket;
    private InputStream inStream;
    private LocalGameActivity activity;

    public ClientWorker(BluetoothSocket sock, LocalGameActivity activity, BluetoothServer serv) {
        this.activity = activity;
        server = serv;
        socket = sock;
    }
    public void run() {
        boolean end = false;
        byte[] messageLength = new byte[10000];
        String messageString = "";
        try {
            inStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }


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


                activity.runOnUiThread(new PacketHandler(messageString));

                //A full packet is read, do something with it

                messageString = "";
                end = false;

            } catch (IOException e) {
                server.sockets.remove(socket);
                activity.playerMap.remove(socket.toString());
                JSONObject toSend = new JSONObject();
                try {
                    toSend.put("identifier", socket.toString());
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                server.broadcast("kick", toSend);
                e.printStackTrace();
                break;
            }
        }
    }

    //this will be run on UI thread
    public void processPacket(String packet) {
        try {
            Log.e("info", packet);
            JSONObject obj = new JSONObject(packet);
            JSONObject toSend = new JSONObject();
            String id = obj.getString("packet");



            if (id.equals("acknowledge")) {
                LocalPlayer ply = new LocalPlayer(activity);
                ply.identifier = socket.toString();
                ply.socket = socket;
                activity.playerMap.put(ply.identifier, ply);
                ply.latitude = 0;
                ply.longitude = 0;
                ply.bomb = false;
                ply.name = obj.getString("name");
                ply.update();

                JSONObject playerObj = new JSONObject();

                playerObj.put("identifier", ply.identifier);
                playerObj.put("latitude", ply.latitude);
                playerObj.put("longitude", ply.longitude);
                playerObj.put("name", ply.name);
                playerObj.put("bomb", ply.bomb);
                toSend.put("player", playerObj);

                server.sendPacket(socket, "initialize", toSend);
            }
            else if (id.equals("bomb")) {
                for (HashMap.Entry<String, LocalPlayer> entry : activity.playerMap.entrySet()) {
                    entry.getValue().bomb = false;
                }
                LocalPlayer ply = activity.playerMap.get(obj.getString("identifier"));
                if (ply.identifier.equals(activity.selfPlayer))
                    Toast.makeText(activity, "You got the bomb", Toast.LENGTH_LONG).show();

                if (ply != null) {
                    ply.bomb = true;
                }
            }
            else if (id.equals("location")) {
                LocalPlayer ply = activity.playerMap.get(socket.toString());
                if (ply != null) {
                    ply.longitude = obj.getDouble("longitude");
                    ply.latitude = obj.getDouble("latitude");
                    ply.update();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
