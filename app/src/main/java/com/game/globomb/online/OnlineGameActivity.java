package com.game.globomb.online;

import android.location.Location;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.game.globomb.R;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import java.util.logging.LogRecord;


public class OnlineGameActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String SERVER_URL = "http://10.0.2.2:8080"; //on emulator

    private final String TAG = "OnlineGameActivity";

    public GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    protected Boolean mRequestingLocationUpdates; // Tracks the status of the location updates request. Value changes when the user presses the
                                                  // Start Updates and Stop Updates buttons.
    protected String mLastUpdateTime; // Time when the location was updated represented as a String.
    public Polyline polyline;
    public HashMap<String, OnlinePlayer> playerMap = new HashMap<String, OnlinePlayer>();
    public OnlinePlayer chosen;
    public String selfPlayer;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private MessageListener messageListener;
    private InitializeListener initializeListener;
    private KickListener kickListener;
    private GamestateListener gamestateListener;
    private Socket gameSocket;
    private ExplodeListener explodeListener;

    public String playerName;

    public TextView timeView;

    private Handler mTimerHandler = new Handler();
    private Runnable mTimerExecutor = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        setUpMapIfNeeded();
        buildGoogleApiClient();
        setPlayerName();

        try {
            Log.v(TAG, "Connecting to: " + SERVER_URL);
            gameSocket = IO.socket(SERVER_URL);
            setupComponents();
        } catch (URISyntaxException e) {
            Log.v(TAG,"Unable to connect to host! \n"+e);
            Toast.makeText(getApplicationContext(), "Unable to connect to host!", Toast.LENGTH_LONG).show();
            mTimerHandler.postDelayed(mTimerExecutor, 4*1000);
        }

        timeView = (TextView) findViewById(R.id.timeView);
    }

    private void setPlayerName() {
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            playerName = extras.getString("name", "unnamed player");
        }
    }

    private void setupComponents(){

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


    public synchronized void sendPacket(String name, JSONObject packet) {
        gameSocket.emit(name, packet);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        gameSocket.disconnect();
        gameSocket.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_online_game, menu);
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
                            for (HashMap.Entry<String, OnlinePlayer> entry : playerMap.entrySet()) {
                                if (marker.equals(entry.getValue().marker)) {
                                    chosen = entry.getValue();
                                    OnlinePlayer onlinePlayer = playerMap.get(selfPlayer);
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 12.0f), 2000, null);
                                    polyline = mMap.addPolyline(new PolylineOptions()
                                            .add(onlinePlayer.marker.getPosition())
                                            .add(marker.getPosition()));

                                    if (onlinePlayer.bomb) {
                                        Toast.makeText(getApplicationContext(), "Send to " + chosen.name, Toast.LENGTH_LONG).show();
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
                        tvLat.setText("OnlinePlayer: " + arg0.getTitle());

                        // Setting the longitude
                        tvLng.setText("Longitude:" + latLng.longitude);

                        // Returning the view containing InfoWindow contents
                        return v;

                    }
                });
            }
        }
    }



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

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Location services suspended. Please reconnect.", Toast.LENGTH_LONG);
    }

    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        JSONObject object = new JSONObject();
        try {
            object.put("longitude", mCurrentLocation.getLongitude());
            object.put("latitude", mCurrentLocation.getLatitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        sendPacket("location", object);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection failed.", Toast.LENGTH_LONG);
    }
}
