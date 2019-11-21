package dev.lifeStyleRPG;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {
    private View mapView;
    private static GoogleMap mMap;
    //for permissions, basically an arbitrary number to mark/identify requests
    final static int REQUEST_CODE = 100;
    private static mapsViewModel viewModel;
    private FirebaseFirestore fstore;
    private FirebaseAuth mFireBaseAuth;

    Button locationButton;
    String locButt_text;
    String trail_name;
    Intent locationIntent;

    //for tracking user.
    private static Circle player_pos;
    private static CircleOptions circle_properties = new CircleOptions()
            .radius(20f)
            .strokeWidth(3f)
            .strokeColor(Color.RED)
            .fillColor(Color.BLUE);
    private static Polyline currentTrail;
    private static PolylineOptions currentTrailOptions = new PolylineOptions()
            .visible(false)
            .width(3);

    //Viewmodel holds the info, this holds the actual polyline. Draw polylines gets information from
    //the maps viewmodel and inserts a polyline corresponding to its id here
    private HashMap<String,Polyline> map_trails = new HashMap<>();
    private static PolylineOptions map_trails_options = new PolylineOptions()
            .visible(true)
            .width(3)
            .color(Color.GREEN);

    /**
     * Next few Overrides deal with saving the state of the fragment.
     * Currently rotating will save the fragment state correctly
     * Leaving the activity and saving the fragment state is a TODO
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    //Note, with this logic, this is only called once. These should be initializers
    //Only called again when the fragment is destroyed.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Log.e("MapsFragment", "onCreate");
        mapView = inflater.inflate(R.layout.fragment_map, null);

        mFireBaseAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();

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
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.e("MapsFragment", "onActivityCreated");

        //Let's pull information from firestore here
        //Attaches a Listener that performs an action once complete
        //Maybe here we store the trails in the view model
        fstore.collection("trails").get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot document: task.getResult()){
                            //Log.e("MapsFragment", document.getId() + "=>" + document.getData());
                            viewModel.insertTrail(document.getId(), document.getData());
                        }
                        //Also draw the trails
                        drawTrails(viewModel.returnTrailMap().keySet());
                    }else{
                        Log.d("MapsFragment","Error getting documents");
                    }
                }
            });

        //restore fragment state here
        if(savedInstanceState != null){
            viewModel.setString(savedInstanceState.getString("button_txt"));
            //If we come back from a pause or something, and they didn't stop the trail
            //we must populate the view model again.
            Log.e("Fragment", savedInstanceState.getParcelableArrayList("trail").toString());
            if (savedInstanceState.getBoolean("isMaking") == true){
                trail_name=savedInstanceState.getString("trail_name");
                continueTrail(savedInstanceState.getString("trail_name"), savedInstanceState.getParcelableArrayList("trail"));
            }
            viewModel.setPlayerPos(savedInstanceState.getParcelable("last_pos"));
        }
    }

    private void drawTrails(Set<String> keySet) {
        for(String key: keySet){
            Polyline trail;
            trail = mMap.addPolyline(map_trails_options);
            trail.setPoints((List<LatLng>)viewModel.returnTrailMap().get(key).get("trailPoints"));
            trail.setVisible(true);
            map_trails.put(key,trail);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("button_txt", viewModel.get_current_text());
        ArrayList<LatLng> tmp = viewModel.getTrail();
        outState.putParcelableArrayList("trail", tmp);
        outState.putString("trail_name", viewModel.getTrailName());
        outState.putBoolean("isMaking", viewModel.isMakingTrail());
        outState.putParcelable("last_pos", viewModel.getLastPos());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("MapsFragment", "onResume");
        locationButton.setText(viewModel.get_current_text());
        trail_name = viewModel.getTrailName();
        SupportMapFragment smf = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        smf.getMapAsync(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        //remove duplicate player pos
        if(player_pos != null)
            player_pos.remove();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("MapsFragment", "onDestroy");
        //unregister the listener
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(myBroadcastReceiver);
    }


    /**
     * Register onclick listeners here. This is how to do it with a fragment
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.MapsLocationButton:
                startLocationService(view);
                break;
        }
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
        Log.e("MapsFragment", "onMapReady");
        mMap = googleMap;
        LatLng asdf = new LatLng(-34.058,22.43);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(asdf,15));
        locationButton.setText(viewModel.get_current_text());
        /*Get last known coordinates, and i*/
        if(viewModel.getLastPos() != null && player_pos == null)
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
            //ask for permissions.
            //need to still handle a deny request
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                Log.d("startLocationService","not granted");
                //if permissions aren't set, ask
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            } else {

                /**
                 * Sets up insert trail name dialogue
                 */
                final EditText txtUrl = new EditText(getContext());
                new AlertDialog.Builder(getContext())
                        .setTitle("Trail Name")
                        .setView(txtUrl)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(txtUrl.getText().toString().equals("") || txtUrl.getText() == null){
                                Toast.makeText(getContext(),"Please enter a name", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            trail_name = txtUrl.getText().toString();

                            /**
                            * Starts the location service
                            */
                            getActivity().startService(locationIntent);
                            startTrail(trail_name);
                            Log.e("startloc", "start service");
                            locButt_text = getResources().getString(R.string.stop_location);
                            locationButton.setText(R.string.stop_location);
                            viewModel.setString(getResources().getString(R.string.stop_location));
                            dialogInterface.dismiss();
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    }).show();
                }
        }else if(locButt_text.equals(getResources().getString(R.string.stop_location))){
            Log.d("stopLocationService", locButt_text);
            Map<String, Object> payload = new HashMap<>();
            getActivity().stopService(locationIntent);

            //Deals with the case if user starts and stops quickly
            if(viewModel.getPlayerPos() == null){
                viewModel.setString(getResources().getString(R.string.start_location));
                locationButton.setText(R.string.start_location);
                locButt_text = getResources().getString(R.string.start_location);
                return;
            }
            //To track the city name
            Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(viewModel.getPlayerPos().latitude,viewModel.getPlayerPos().longitude,1);
                String cityName = addresses.get(0).getAddressLine(0);
                payload.put("location",cityName);
            //Throws IOexception if geocoder isn't available
            } catch (IOException e) {
                Toast.makeText(getContext(),"network unavailable",Toast.LENGTH_SHORT).show();
                //empty string null...
                payload.put("location","");
            }
            payload.put("userid",mFireBaseAuth.getCurrentUser().getUid());
            payload.put("name", viewModel.getTrailName());
            payload.put("time",new Timestamp(new Date()));
            //firebase only accepts geopoints, so we have to convert the trail's latlng to geopoint
            ArrayList<GeoPoint> gpTrail= new ArrayList<>();
            ArrayList<LatLng> trail = viewModel.getTrail();
            if(trail != null){
                Iterator<LatLng> it = trail.iterator();
                while(it.hasNext()){
                    LatLng t = it.next();
                    gpTrail.add(new GeoPoint(t.latitude,t.longitude));
                }
                payload.put("trailPoints",gpTrail);
            }else{
                payload.put("trailPoints","");
            }

            //Adds in the payload
            fstore.collection("trails").
                    add(payload)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(getContext(),"Trail successfully saved", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(),"Not saved to cloud", Toast.LENGTH_SHORT).show();
                        }
                    });
            endTrail();
            viewModel.setString(getResources().getString(R.string.start_location));
            locationButton.setText(R.string.start_location);
            locButt_text = getResources().getString(R.string.start_location);
        }
    }

    //Start trail
    public static void startTrail(String name) {
        viewModel.resetTrail();
        viewModel.setTrailName(name);
        viewModel.setMakingTrail(true);
        currentTrailOptions.visible(true);
    }
    //start trail from a pause
    public static void continueTrail(String name, ArrayList<LatLng> prefix){
        viewModel.resetTrail();
        viewModel.setTrailName(name);
        viewModel.setTrail(prefix);
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
        //viewModel.stashTrail();
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


}
