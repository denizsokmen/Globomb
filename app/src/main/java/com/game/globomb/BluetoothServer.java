package com.game.globomb;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;
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
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Globomb Server", uuid);
        } catch (IOException e) { }
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
}