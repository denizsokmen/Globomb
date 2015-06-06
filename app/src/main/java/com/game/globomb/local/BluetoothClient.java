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

        activity.onConnectionSuccessful();
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
            activity.receivePacket(id, obj);
            Log.e("info", obj.toString());
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