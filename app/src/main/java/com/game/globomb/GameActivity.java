package com.game.globomb;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    private final String TAG = "GameActivity";

    public GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    private BluetoothClient client;
    private BluetoothServer server;
    private boolean isHost;
    private boolean isLocal;


    private Marker myMarker;

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private final String SERVER_URL = "http://188.226.228.153:8080/"; //this is only for emulator
    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;


    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private MessageListener messageListener;
    private InitializeListener initializeListener;
    private KickListener kickListener;
    private GamestateListener gamestateListener;
    private Socket gameSocket;
    private ExplodeListener explodeListener;
    public HashMap<String, Player> playerMap = new HashMap<String, Player>();
    public String selfPlayer;
    public String playerName;
    public Player chosen;
    public Polyline polyline;

    public Button buttonLabel;
    private BluetoothDevice device;
    private Handler handler;
    private TimerTask timerTask;
    private Timer timer;
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

        try {
            Log.v(TAG,"Connecting to: "+SERVER_URL);
            gameSocket = IO.socket(SERVER_URL);
        } catch (URISyntaxException e) {
            Log.v(TAG,"Unable to connect to host! \n"+e);
        }

        if (gameSocket != null ) {

        }
        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";
        buttonLabel = (Button) findViewById(R.id.labelbutton);

        buttonLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosen != null) {
                    Player player = playerMap.get(selfPlayer);

                    if (player.bomb) {

                        player.bomb = false;
                        JSONObject object = new JSONObject();
                        try {
                            object.put("identifier", chosen.identifier);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        sendPacket("bomb", object);
                    }
                }
            }
        });

        setUpMapIfNeeded();
        buildGoogleApiClient();

    }
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        startConnection();
    }


    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        setUpMapIfNeeded();
    }

    private void hostGame() {
        server = new BluetoothServer(this);
        server.start();

        Player ply = new Player(this);
        playerMap.put("host", ply);
        ply.identifier = "host";
        ply.latitude = 0;
        ply.longitude = 0;
        ply.bomb = true;
        ply.name = "asd";
        ply.update();
        selfPlayer = "host";

        handler = new Handler();
        runnable = new Runnable()
        {

            public void run()
            {
                sendGamestate();
                handler.postDelayed(this, 1000);
            }
        };

        runnable.run();
    }

    private void startConnection() {
        if (isLocal) {
            if (isHost) {
                hostGame();
            }
            else {
                client = new BluetoothClient(device, this);
                client.start();
            }
        }
        else {
            messageListener = new MessageListener(this);
            initializeListener = new InitializeListener(this);
            kickListener = new KickListener(this);
            gamestateListener = new GamestateListener(this);
            explodeListener = new ExplodeListener(this);

            Log.v(TAG, "Starting...");

            gameSocket.connect();
            gameSocket.on("message", messageListener);
            gameSocket.on("initialize", initializeListener);
            gameSocket.on("kick", kickListener);
            gameSocket.on("gamestate", gamestateListener);
            gameSocket.on("explode", explodeListener);

            JSONObject object = new JSONObject();
            try {
                object.put("name", playerName);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            sendPacket("acknowledge", object);
        }


    }

    protected synchronized void buildGoogleApiClient() {
        Log.v(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
        else {

            gameSocket.emit(name, packet);
        }
    }







    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call  when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if (polyline != null)
                            polyline.remove();

                        if (marker.equals(playerMap.get(selfPlayer).marker)) {

                            return true;
                        }
                        else {
                            for (HashMap.Entry<String, Player> entry : playerMap.entrySet()) {
                                if (marker.equals(entry.getValue().marker)) {
                                    chosen = entry.getValue();
                                    Player player = playerMap.get(selfPlayer);
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 12.0f), 2000, null);
                                    polyline = mMap.addPolyline(new PolylineOptions()
                                            .add(player.marker.getPosition())
                                            .add(marker.getPosition()));

                                    if (player.bomb) {
                                        buttonLabel.setText("Send to " + chosen.name);
                                    }

                                    break;
                                }
                            }
                            return false;
                        }
                    }
                });

                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    // Use default InfoWindow frame
                    @Override
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    // Defines the contents of the InfoWindow
                    @Override
                    public View getInfoContents(Marker arg0) {

                        // Getting view from the layout file info_window_layout
                        View v = getLayoutInflater().inflate(R.layout.marker_layout, null);

                        // Getting the position from the marker
                        LatLng latLng = arg0.getPosition();


                        // Getting reference to the TextView to set latitude
                        TextView tvLat = (TextView) v.findViewById(R.id.tv_title);

                        // Getting reference to the TextView to set longitude
                        TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);

                        // Setting the latitude
                        tvLat.setText("Player: " + arg0.getTitle());

                        // Setting the longitude
                        tvLng.setText("Longitude:" + latLng.longitude);

                        // Returning the view containing InfoWindow contents
                        return v;

                    }
                });
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */

    public void showToast(String text, int duration){
        Context context = getApplicationContext();
        Toast.makeText(context, text, duration).show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Location services connected.");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mCurrentLocation != null ){

                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

            }


//            updateUI();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "Connection failed.");
    }

    @Override
    public void onLocationChanged(Location location) {
//        this.showToast("update", Toast.LENGTH_SHORT);
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        if (isHost) {
            Player ply = playerMap.get("host");
            ply.longitude = mCurrentLocation.getLongitude();
            ply.latitude = mCurrentLocation.getLatitude();
            ply.update();
        }
        JSONObject object = new JSONObject();
        try {
            object.put("longitude", mCurrentLocation.getLongitude());
            object.put("latitude", mCurrentLocation.getLatitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        sendPacket("location", object);
    }

    public void sendGamestate() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject gamestate = new JSONObject();
                    JSONArray players = new JSONArray();
                    for (HashMap.Entry<String, Player> entry : playerMap.entrySet()) {
                        Player ply = entry.getValue();
                        JSONObject playerObj = new JSONObject();

                        playerObj.put("identifier", ply.identifier);
                        playerObj.put("latitude", ply.latitude);
                        playerObj.put("longitude", ply.longitude);
                        playerObj.put("name", ply.name);
                        playerObj.put("bomb", ply.bomb);
                        players.put(playerObj);

                    }

                    gamestate.put("players", players);
                    gamestate.put("time", 10);
                    server.broadcast("gamestate", gamestate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();


    }
}
