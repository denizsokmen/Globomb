package com.game.globomb;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;


public class BluetoothServer extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    public final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;
    private GameActivity game;
    private ArrayList<BluetoothSocket> sockets;

    public BluetoothServer(GameActivity activity) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        game = activity;
        sockets = new ArrayList<BluetoothSocket>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Globomb Server", uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                sockets.add(socket);
                new ClientWorker(socket, game, this).run();
                // Do work to manage the connection (in a separate thread)
                //manageConnectedSocket(socket);

            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }


    public synchronized void sendPacket(BluetoothSocket client, String name, JSONObject packet) {
        try {
            packet.put("packet", name);
            DataOutputStream stream = new DataOutputStream(client.getOutputStream());
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

    public synchronized void broadcast(String name, JSONObject packet) {
        for(BluetoothSocket sock : sockets) {
            sendPacket(sock, name, packet);
        }
    }
}