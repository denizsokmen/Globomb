package com.game.globomb;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PipedInputStream;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class GameActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    private final String TAG = "GameActivity";

    public GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;


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

    public Button buttonLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            playerName = extras.getString("name", "generic player");
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

    private void startConnection() {
        messageListener = new MessageListener(this);
        initializeListener = new InitializeListener(this);
        kickListener = new KickListener(this);
        gamestateListener = new GamestateListener(this);
        explodeListener = new ExplodeListener(this);

        Log.v(TAG,"Starting...");


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
        gameSocket.emit("acknowledge", object);


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
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 19.0f), 2000, null);
                        if (playerMap.get(selfPlayer).marker == marker)
                            return true;
                        else
                            return false;
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


        JSONObject object = new JSONObject();
        try {
            object.put("longitude", mCurrentLocation.getLongitude());
            object.put("latitude", mCurrentLocation.getLatitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        gameSocket.emit("location", object);
    }
}
