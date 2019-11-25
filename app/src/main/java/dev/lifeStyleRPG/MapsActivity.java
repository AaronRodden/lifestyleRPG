package dev.lifeStyleRPG;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    public static GoogleMap mMap;
    private static LatLng point1 = new LatLng(51.5, -0.1);
    private static LatLng point2 = new LatLng(40.7, -74.0);
    private static LatLng[] endpoints = {point1, point2};
    private static int count = 0;
    //for permissions, basically an arbitrary number to mark/identify requests
    final static int REQUEST_CODE = 100;
    public static final int REQUESTCODE = 1;
    public static mapsViewModel viewModel;

    Button locationButton;
    String locButt_text;
    Intent locationIntent;
    BottomNavigationView bottomNavigationView;
    //for tracking user.
    public static Circle player_pos;
    public static CircleOptions circle_properties = new CircleOptions()
                .radius(20f)
                .strokeWidth(3f)
                .strokeColor(Color.RED)
                .fillColor(Color.BLUE);
    public static Polyline currentTrail;
    public static PolylineOptions currentTrailOptions = new PolylineOptions()
                .visible(false)
                .width(3);

    FirebaseFirestore fStore;
    FirebaseAuth mFireBaseAuth;

    //This is called whenever the activity is started up, or when momentarily stopped
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //initialize the locationButton
        locationButton = findViewById(R.id.MapsLocationButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.maps_screen:
                        break;
                    case R.id.player_profile_screen:
                        Intent profileIntent = new Intent(MapsActivity.this, PlayerMenu.class);
                        startActivity(profileIntent);
                        break;
                    case R.id.trails_screen:
//                        Intent trailsIntent = new Intent(getApplicationContext(),Trails.class);
//                        startActivityForResult(trailsIntent, REQUESTCODE);
                        break;
                    case R.id.settings_screen:
                        break;
                }
                return true;
            }
        });
        //set up the viewModel class, bind it to this activity
        viewModel = ViewModelProviders.of(this).get(mapsViewModel.class);
        //set text of location button from viewmodel
        locationButton.setText(viewModel.get_current_text());

        //Register this activity to receive messages
        //So actions with "sample-event" are found
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, new IntentFilter("sample-event"));
    }

    //This method is called when the activity is going to be destroyed, not paused
    //This should save state of the activity, send data to firebase etc.
    @Override
    protected void onDestroy(){
        //unregister the listener
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myBroadcastReceiver);
        super.onDestroy();
    }

    public static void setEndpoint(final LatLng latLng) {
        endpoints[count] = latLng;
        ++count;
        count %= 2;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.e("MapsActivity", "onMapREady");
        mMap = googleMap;



        Polyline line = mMap.addPolyline(new PolylineOptions()
            .add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
            .width(5)
            .color(Color.RED));
    }

    /**
     *Method for starting Location Service
     * Maybe for the text, we'll check if the service is active instead of relying on string.
     * Since if the activity is cut short before the view model is set, then there might be problems
     *https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
     * i'll implement that later
     */
    public void startLocationService(View view) {
        //this is the location button on maps
        locationButton = (Button) view;
        locButt_text = locationButton.getText().toString();
        locationIntent = new Intent(this, LocationService.class);
        if (locButt_text.equals(getResources().getString(R.string.start_location))){
            Log.d("startLocationService", locButt_text);
            //ask for permissions.
            //need to still handle a deny request
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                Log.d("startLocationService","not granted");
                //if permissions aren't set, ask
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            } else {
                startService(locationIntent);
                startTrail("test-name");
                Log.d("startloc", "start service");
                locationButton.setText(R.string.stop_location);
                viewModel.setString(getResources().getString(R.string.stop_location));
            }
        }else if(locButt_text.equals(getResources().getString(R.string.stop_location))){
            Log.d("stopLocationService", locButt_text);
            endTrail();
            stopService(locationIntent);
            viewModel.setString(getResources().getString(R.string.start_location));
            locationButton.setText(R.string.start_location);
        }
    }


    public static void startTrail(String name) {
        viewModel.resetTrail();
        viewModel.setTrailName(name);
        viewModel.setMakingTrail(true);
        currentTrailOptions.visible(true);
    }

    // discards the current trail
    public static void discardTrail() {
        viewModel.setMakingTrail(false);
        viewModel.deleteTrail(viewModel.getTrailName());
        currentTrailOptions.visible(false);
    }

    public static void endTrail() {
        viewModel.setMakingTrail(false);
        viewModel.stashTrail();
        currentTrailOptions.visible(false);
    }

    public void updateExp() {
        FirebaseFirestore fStore;
        FirebaseAuth mFireBaseAuth;
        TextView exptext;
        TextView trailsdone;
        exptext = findViewById(R.id.totalEXP);
        trailsdone = findViewById(R.id.trailsDone);
        fStore = FirebaseFirestore.getInstance();
        mFireBaseAuth = FirebaseAuth.getInstance();

        String temp = exptext.toString();
        int numTemp = Integer.parseInt(temp) + 10;
        exptext.setText(numTemp);

        //        String userID = mFireBaseAuth.getCurrentUser().getUid();
//        Task<QuerySnapshot> snapshot = fStore.collection("users").get();
//        QuerySnapshot snap = snapshot.getResult();
//        fStore.collection("users").whereEqualTo("userid",userID);
//        .get();
//        .get()
//        .then(function(snap){
//            snap.forEach(function(doc) {
//                console.log(doc.id, " => ", doc.data());
//                // Build doc ref from doc.id
//                db.collection("users").doc(doc.id).update({foo: "bar"});
//        };
    }

    /*
    BroadCast receiver to interact with a local broadcast manasger from Location Service.
    Below methods will interact with the maps activity.
     */
    private static BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0.0);
            double lon = intent.getDoubleExtra("lon", 0.0);

            LatLng pos = new LatLng(lat,lon);
            //if play_pos is initialized on the map remove it.
            if(player_pos != null){
                player_pos.remove();
            }

            //Log.d("receiver", "Got message: " + message);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos,15));
            player_pos = mMap.addCircle(circle_properties.center(pos));

            // add new point every 4 meters
            if(viewModel.isMakingTrail() && (viewModel.getLastPos() == null
                    || getDistance(pos, viewModel.getLastPos()) > 4)) {
                if(currentTrail != null) {
                    currentTrail.remove();
                }
                viewModel.appendToTrail(pos);
                currentTrail = mMap.addPolyline(currentTrailOptions);
                currentTrail.setPoints(viewModel.getTrail());
            }
        }
    };

    // couldn't find any method to get the distance between 2 LatLng's so
    // we gotta do something jank
    private static double getDistance(LatLng a, LatLng b) {
        Location locA = new Location("a");
        locA.setLatitude(a.latitude);
        locA.setLongitude(a.longitude);
        Location locB = new Location("b");
        locB.setLatitude(b.latitude);
        locB.setLongitude(b.longitude);

        return locA.distanceTo(locB);
    }
}
