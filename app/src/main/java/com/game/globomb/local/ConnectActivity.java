package com.game.globomb.local;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.game.globomb.R;

import java.util.ArrayList;
import java.util.List;


public class ConnectActivity extends ActionBarActivity {

    public List<ServerEntry> items;
    private BroadcastReceiver mReceiver;
    private ListAdapter adapter;
    private BluetoothAdapter btAdapter;
    public String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        items = new ArrayList<ServerEntry>();
        ListView listView = (ListView) findViewById(R.id.listView);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            name = extras.getString("name", "generic player");
        }

        Button hostbutton = (Button) findViewById(R.id.hostbutton);

        hostbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConnectActivity.this, LocalGameActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("local", true);
                intent.putExtra("host", true);
                startActivity(intent);
            }
        });

        adapter = new ListAdapter(this, R.layout.list_row, items);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ServerEntry entry = items.get(position);
                Intent intent = new Intent(ConnectActivity.this, LocalGameActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("local", true);
                intent.putExtra("host", false);
                intent.putExtra("device", entry.device);
                startActivity(intent);
            }
        });

        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    ServerEntry entry = new ServerEntry();
                    entry.device = device;
                    entry.name = device.getName();
                    if (!items.contains(device))
                        items.add(entry);
                    adapter.notifyDataSetChanged();
                }
            }
        };

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver); // Don't forget to unregister during onDestroy
        btAdapter.cancelDiscovery();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        btAdapter.startDiscovery();
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

    private class ServerEntry {

        public BluetoothDevice device;
        public String name;
    }

    private class ListAdapter extends ArrayAdapter<ServerEntry> {

        public ListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public ListAdapter(Context context, int resource, List<ServerEntry> items) {
            super(context, resource, items);
        }



        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.list_row, null);
            }

            ServerEntry p = getItem(position);

            if (p != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.titleText);

                if (tt1 != null) {
                    tt1.setText(p.name);
                }
            }

            return v;
        }

    }
}
