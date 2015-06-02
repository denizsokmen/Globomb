package com.game.globomb.online;

import android.location.Location;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Polyline;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;




public class OnlineGameActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String SERVER_URL = "http://10.0.2.2:8080"; //on emulator

    private final String TAG = "OnlineGameActivity";


    private GoogleApiClient googleApiClient;


    protected LocationRequest locationRequest;
    protected Location currentLocation;
    protected Boolean requestingLocationUpdates; // Tracks the status of the location updates request. Value changes when the user presses the
                                                  // Start Updates and Stop Updates buttons.

    protected String lastUpdateTime; // Time when the location was updated represented as a String.

    public Polyline polyline;
    public HashMap<String, OnlinePlayer> playerMap = new HashMap<String, OnlinePlayer>();
    public OnlinePlayer chosen;
    public String selfPlayer;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private MessageListener messageListener;
    private InitializeListener initializeListener;
    private KickListener kickListener;
    private GameStateListener2 gameStateListener2;
    private Socket gameSocket;
    private ExplodeListener explodeListener;

    public String playerName;
    public TextView timeView;
    public TextView playerView;
    public GoogleMap map;

    private Handler timerHandler = new Handler();
    private Runnable timerExecutor = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        FragmentManager myFM = getSupportFragmentManager();
        final SupportMapFragment mapFragment = (SupportMapFragment) myFM
                .findFragmentById(R.id.onlinemap);

        if (mapFragment == null) {
            Toast.makeText(getApplicationContext(), "NULL", Toast.LENGTH_LONG).show();
        }
        else{
            mapFragment.getMapAsync(this);
        }



        timeView = (TextView) findViewById(R.id.timeView);
        playerView = (TextView) findViewById(R.id.playerView);
    }

    private void setPlayerName() {
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            playerName = extras.getString("name", "unnamed player");
        }
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
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }




    public void onConnected(Bundle bundle) {
        Log.v(TAG, "Location services connected.");
        if (currentLocation == null) {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (currentLocation != null ){

                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());

            }


//            updateUI();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // requestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of requestingLocationUpdates and if it is true, we start location updates.
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Location services suspended. Please reconnect.", Toast.LENGTH_LONG).show();
    }

    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        JSONObject object = new JSONObject();
        try {
            object.put("longitude", currentLocation.getLongitude());
            object.put("latitude", currentLocation.getLatitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        gameSocket.emit("location", object);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection failed.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        setPlayerName();
        Log.v(TAG, "Connecting to: " + SERVER_URL);
        try {
            gameSocket = IO.socket(SERVER_URL);
            gameSocket.connect();

            setupComponents();

        } catch (URISyntaxException e) {
            Log.v(TAG, "Server URL is not valid! \n" + e);
            Toast.makeText(getApplicationContext(), "Server URL is not valid!", Toast.LENGTH_LONG).show();
            timerHandler.postDelayed(timerExecutor, 4 * 1000);
            return;
        }
        buildGoogleApiClient();
        setPlayerName();

    }

    private void setupComponents(){

        messageListener = new MessageListener(this);
        initializeListener = new InitializeListener(this);
        kickListener = new KickListener(this);
        gameStateListener2 = new GameStateListener2(this);
        explodeListener = new ExplodeListener(this);

        Log.v(TAG, "Starting...");


        gameSocket.on("message", messageListener);
        gameSocket.on("initialize", initializeListener);
        gameSocket.on("kick", kickListener);
        gameSocket.on("gamestate", gameStateListener2);
        gameSocket.on("explode", explodeListener);

        JSONObject object = new JSONObject();
        try {
            object.put("name", playerName);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        gameSocket.emit("acknowledge", object);
    }
}
