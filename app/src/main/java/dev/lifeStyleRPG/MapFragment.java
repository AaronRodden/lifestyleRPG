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

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {
    View mapView;
    public static GoogleMap mMap;
    Bundle savedState;

    //for permissions, basically an arbitrary number to mark/identify requests
    final static int REQUEST_CODE = 100;
    public static mapsViewModel viewModel;

    Button locationButton;
    String locButt_text;
    Intent locationIntent;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mapView = inflater.inflate(R.layout.fragment_map, null);

        //initialize the locationButton
        locationButton = mapView.findViewById(R.id.MapsLocationButton);
        locationButton.setOnClickListener(this);
        //set up the viewModel class, bind it to this activity
        viewModel = ViewModelProviders.of(this).get(mapsViewModel.class);
        //set text of location button from viewmodel

        //Register this activity to receive messages
        //So actions with "sample-event" are found
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(myBroadcastReceiver, new IntentFilter("sample-event"));

        return mapView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SupportMapFragment smf = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));

        smf.getMapAsync(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("MapsAcvitity", "onDestroy");
        //unregister the listener
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(myBroadcastReceiver);
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
        Log.e("MapsActivity", "onMapReady");
        mMap = googleMap;
        /*Get last known coordinates*/
        if(viewModel.getLastPos() != null)
            player_pos = mMap.addCircle(circle_properties.center(viewModel.getLastPos()));
        if(viewModel.getTrail() != null)
            currentTrail = mMap.addPolyline(currentTrailOptions);
        currentTrail.setPoints(viewModel.getTrail());
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
        locationIntent = new Intent(getActivity(), LocationService.class);
        if (locButt_text.equals(getResources().getString(R.string.start_location))){
            Log.d("startLocationService", locButt_text);
            //ask for permissions.
            //need to still handle a deny request
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                Log.d("startLocationService","not granted");
                //if permissions aren't set, ask
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            } else {
                getActivity().startService(locationIntent);
                startTrail("test-name");
                Log.d("startloc", "start service");
                locButt_text = getResources().getString(R.string.stop_location);
                locationButton.setText(R.string.stop_location);
                viewModel.setString(getResources().getString(R.string.stop_location));
            }
        }else if(locButt_text.equals(getResources().getString(R.string.stop_location))){
            Log.d("stopLocationService", locButt_text);
            endTrail();
            getActivity().stopService(locationIntent);
            viewModel.setString(getResources().getString(R.string.start_location));
            locationButton.setText(R.string.start_location);
            locButt_text = getResources().getString(R.string.start_location);
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
            viewModel.setPlayerPos(pos);

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

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.MapsLocationButton:
                Log.e("fragment", "DOOODD");
                startLocationService(view);
                break;
        }
    }
}
