package com.game.globomb.online;

import android.location.Location;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.game.globomb.R;
import com.game.globomb.Utility;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
    protected Boolean requestingLocationUpdates = true; // Tracks the status of the location updates request. Value changes when the user presses the
                                                  // Start Updates and Stop Updates buttons.

    protected String lastUpdateTime; // Time when the location was updated represented as a String.

    public Polyline polyline;
    public HashMap<String, OnlinePlayer> playerMap = new HashMap<String, OnlinePlayer>();

    public String selfPlayer;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private MessageListener messageListener;
    private InitializeListener initializeListener;
    private KickListener kickListener;
    private GameStateListener gameStateListener;
    private Socket gameSocket;
    private ExplodeListener explodeListener;

    public String playerName;
    public TextView timeView;
    public TextView playerView;
    public GoogleMap map;
    public Button gloBombButton;
    public Boolean bomb = false;
    public OnlinePlayer chosen = null;
    public OnlinePlayer player = null;

    public Handler timerHandler = new Handler();
    public Runnable timerExecutor = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };
    public ImageView gloBombImage;

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

        gloBombImage = (ImageView) findViewById(R.id.bombImageView);
        gloBombButton = (Button) findViewById(R.id.sendglobomb);
        timeView = (TextView) findViewById(R.id.timeView);
        playerView = (TextView) findViewById(R.id.playerView);

        gloBombButton.setVisibility(View.INVISIBLE);
        gloBombImage.setAlpha(0.0f);


        gloBombButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bomb && chosen != null) {
                        player.bomb = false;
                        gloBombImage.setAlpha(0.1f);
                        gloBombButton.setVisibility(View.INVISIBLE);
                        JSONObject object = new JSONObject();
                        try {
                            object.put("identifier", chosen.identifier);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        gameSocket.emit("bomb", object);
                }
            }
        });

        Log.v(TAG, "In emulator: " + Utility.inEmulator());

        buildGoogleApiClient();

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



        }

        if (requestingLocationUpdates) {
            setupComponents();
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Location services suspended. Please reconnect.", Toast.LENGTH_LONG).show();
    }

    protected void startLocationUpdates() {

        Log.v(TAG, "startLocationUpdates()");


        try {
            if (Utility.inEmulator()) {
                JSONObject object = new JSONObject();
                object.put("longitude", Utility.getRandomLongitude());
                object.put("latitude", Utility.getRandomLatitude());
                gameSocket.emit("location", object);
            }
            else {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
        }
        catch (JSONException e){
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        lastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        try {
            if (!Utility.inEmulator()) {
                JSONObject object = new JSONObject();
                object.put("longitude", currentLocation.getLongitude());
                object.put("latitude", currentLocation.getLatitude());
                gameSocket.emit("location", object);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }



    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection failed.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
        this.map.getUiSettings().setZoomControlsEnabled(true);
        this.map.getUiSettings().setCompassEnabled(false);
        this.map.getUiSettings().setMapToolbarEnabled(false);
        this.map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                gloBombButton.setVisibility(View.INVISIBLE);
            }
        });

        this.map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {

            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.getTitle().equalsIgnoreCase("you")) // if you touch  your own marker
                    gloBombButton.setVisibility(View.INVISIBLE);
                else {
                    if (bomb) {// if you have the bomb
                        gloBombButton.setVisibility(View.VISIBLE);
                        gloBombButton.setText("Send to " + marker.getTitle());
                        for (HashMap.Entry<String, OnlinePlayer> entry : playerMap.entrySet()) {
                            if (marker.equals(entry.getValue().marker)) {
                                chosen = entry.getValue();
                            }
                        }
                    }
                    else {
                        gloBombButton.setVisibility(View.INVISIBLE);
                    }

                }
                marker.showInfoWindow();
                return true;
            }

        });

        setPlayerName();
        Log.v(TAG, "Connecting to: " + SERVER_URL);
        try {
            gameSocket = IO.socket(SERVER_URL);
            gameSocket.connect();

//            setupComponents();

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
        gameStateListener = new GameStateListener(this);
        explodeListener = new ExplodeListener(this);

        Log.v(TAG, "Starting...");


        gameSocket.on("message", messageListener);
        gameSocket.on("initialize", initializeListener);
        gameSocket.on("kick", kickListener);
        gameSocket.on("gamestate", gameStateListener);
        gameSocket.on("explode", explodeListener);

        JSONObject object = new JSONObject();
        try {
            object.put("name", playerName);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        gameSocket.emit("acknowledge", object);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }



    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();


        if (googleApiClient.isConnected() && requestingLocationUpdates) {
            Log.v(TAG, "startLocationUpdates");
            startLocationUpdates();
        }

    }



}
