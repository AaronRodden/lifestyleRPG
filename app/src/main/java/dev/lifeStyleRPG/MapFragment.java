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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

    private String userID;
    private EditText emailId;


    /**
     * This is the camera position read by the trails search activity
     */
    LatLng updated_cam;
    Marker marker;
    MarkerOptions marker_options = new MarkerOptions().anchor((float)0.5,(float)0.5);
    Button locationButton;
    String locButt_text;
    //current trail
    String trail_name;
    Intent locationIntent;

    //for tracking user.
    private static Circle player_pos;
    private static CircleOptions circle_properties = new CircleOptions()
            .radius(20f)
            .strokeWidth(3f)
            .strokeColor(Color.RED)
            .fillColor(Color.BLUE);

    //Trail player is currently making
    private static Polyline currentTrail;
    private static PolylineOptions currentTrailOptions = new PolylineOptions()
            .visible(false)
            .color(R.color.player_trail)
            .width(10);

    //Viewmodel holds the info, this holds the actual polyline. Draw polylines gets information from
    //the maps viewmodel and inserts a polyline corresponding to its id here
    private static HashMap<String,Polyline> map_trails = new HashMap<>();
    private static PolylineOptions map_trails_options = new PolylineOptions()
            .visible(true)
            .width(10)
            .color(R.color.trail);

    /**
     * Next few Overrides deal with saving the state of the fragment.
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

        //Register this activity to receive messages
        //So actions with "sample-event" are found
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(myBroadcastReceiver, new IntentFilter("locationService"));
        return mapView;
    }

    /**
     * Called after Activity's oncreate method has completed, and after on createview.
     * This is used to re-initialize and restore the viewmodel when leaving the Maps activity.
     *
     * I also pull firebase data here.
     *
     * cons: i'm not saving the information - everytime I leave and stuff it pulls all the data from
     * fire base. There's a way to do something like, onupdateslistener to only pull recent documents
     * but I'm not sure how to do that. It's an optimization.
     *
     * Another opt, I'm not saving the hashmap of trails in viewmodel, I'm always repopulating it
     * since this is called after leaving an activity. We could save hashmap state but I'm lazy.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.e("MapsFragment", "onActivityCreated");
        //Let's pull information from firestore here
        //Attaches a Listener that performs an action once complete
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
                        drawTrails(viewModel.getTrailMap().keySet());
                    }else{
                        Log.d("MapsFragment","Error getting documents");
                    }
                }
            });

        //restore fragment state here
        if(savedInstanceState != null){
            viewModel.setString(savedInstanceState.getString("button_txt"));
            Log.e("Fragment", savedInstanceState.getParcelableArrayList("trail").toString());
            // restore trail state
            trail_name=savedInstanceState.getString("trail_name");
            continueTrail(savedInstanceState.getString("trail_name"),
                    savedInstanceState.getParcelableArrayList("trail"),
                    savedInstanceState.getBoolean("isMaking"),
                    savedInstanceState.getBoolean("isRunning"));
            viewModel.setPlayerPos(savedInstanceState.getParcelable("last_pos"));
            //marker state when user clicks a polyline.
            if(savedInstanceState.getParcelable("marker") != null){
                marker_options.position(savedInstanceState.getParcelable("marker"));
                mMap.addMarker(marker_options);
            }
        }
    }
    //Given trailid's from the viewmodel or Firebase, draw out the trails.
    private void drawTrails(Set<String> keySet) {
        for(String key: keySet){
            //if the key alread exists, don't redraw the trail
            if(map_trails.get(key) != null){
                break;
            }
            Polyline trail;
            trail = mMap.addPolyline(map_trails_options);
            trail.setZIndex(-1);
            trail.setPoints((List<LatLng>)viewModel.getTrailMap().get(key).get("trailPoints"));
            trail.setTag(viewModel.getTrailMap().get(key).get("name"));

            if(trail.getPoints().size() == 0){
                break;
            }
            //I need to get caps working so this is a placeholder
            //TODO: Replace this with caps, I have tried but they don't appear.
            CircleOptions startOptions = new CircleOptions()
                    .center(trail.getPoints().get(0))
                    .fillColor(Color.CYAN)
                    .strokeColor(Color.CYAN)
                    .zIndex(1)
                    .radius(5);
            CircleOptions endOptions = new CircleOptions()
                    .center(trail.getPoints().get(trail.getPoints().size()-1))
                    .fillColor(Color.MAGENTA)
                    .strokeColor(Color.MAGENTA)
                    .zIndex(1)
                    .radius(5);
            Circle start = mMap.addCircle(startOptions);
            Circle end = mMap.addCircle(endOptions);

            trail.setClickable(true);
            trail.setVisible(true);
            map_trails.put(key,trail);
        }
    }

    /*
    This is called onPause and onDestroy. Save variables and info needed here.
     */
    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("button_txt", viewModel.getCurrentText());
        ArrayList<LatLng> tmp = viewModel.getCurrentTrail();
        outState.putParcelableArrayList("trail", tmp);
        outState.putString("trail_name", viewModel.getCurrentTrailName());
        outState.putBoolean("isMaking", viewModel.isMakingTrail());
        outState.putBoolean("isRunning", viewModel.isRunningTrail());
        outState.putParcelable("last_pos", viewModel.getLastPos());
        if(marker != null) outState.putParcelable("marker", marker.getPosition());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("MapsFragment", "onResume");
        locationButton.setText(viewModel.getCurrentText());
        trail_name = viewModel.getCurrentTrailName();
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
    /*
    TODO Probably do clean up here, which I haven't done yet.
     */
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
//            case R.id.
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.e("MapsFragment", "onMapReady");
        mMap = googleMap;
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
//                startLocationService(getView());
                // These next two lines are how you start a location service (broadcast reciever will start as well)
                locationIntent = new Intent(getActivity(), LocationService.class);
                getActivity().startService(locationIntent);
//                Log.e("Debug message","In roberts polyline function");
                LatLng midpoint = polyline.getPoints().get(polyline.getPoints().size()/2);
                Map m = viewModel.getTrailByName((String)polyline.getTag());
                String name, uid, username;
                try{
                    name = m.get("name").toString();
                    uid = m.get("userid").toString();
                }catch (NullPointerException e){
                    name = "Unknown";
                    uid = "Unknown";
                }
                if(marker != null) marker.remove();
                marker = mMap.addMarker(marker_options.position(midpoint)
                        .title(name)
                        .snippet(uid));
                marker.showInfoWindow();
                runTrail(polyline.getTag().toString());
                // make the button say "Stop",  and pressing it should let up abandon the trail
                //This is problamatic as whenever you click stop it is trying to make a trail and send it to firebase
//                locButt_text = getResources().getString(R.string.stop_location);
//                locationButton.setText(R.string.stop_location);
//                viewModel.setString(getResources().getString(R.string.stop_location));
            }
        });

        //for debugging purposes because i'm in africa lmao
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-34.058,22.43),15));
        if(updated_cam != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(updated_cam,15));

        locationButton.setText(viewModel.getCurrentText());

        /*Get last known coordinates, and i*/
        if(viewModel.getLastPos() != null && player_pos == null)
            player_pos = mMap.addCircle(circle_properties.center(viewModel.getLastPos()));
        if(viewModel.getCurrentTrail() != null)
            currentTrail = mMap.addPolyline(currentTrailOptions);
        currentTrail.setPoints(viewModel.getCurrentTrail());
    }

    //Trail search update camera. The reasoning was for the trails activity to send a result to
    //the maps activity, and the maps activity interacts with methods in map fragment to interact
    //with the map.
    public void updateCamera(Bundle args){
        if(args != null)
            updated_cam = args.getParcelable("updated_view");
        Log.e("as;ldkfja;lks",updated_cam.toString());
        if(updated_cam != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(updated_cam,15));
        }
    }

    /**
     *Method for starting Location Service
     * Maybe for the text, we'll check if the service is active instead of relying on string.
     */
    public void startLocationService(View view) {
        //this is the location button on maps that starts the service
        locationButton = (Button) view;
        locButt_text = locationButton.getText().toString();
        locationIntent = new Intent(getActivity(), LocationService.class);
        if (locButt_text.equals(getResources().getString(R.string.start_location))){
            //ask for permissions.
            //TODO need to still handle a deny request
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
                            startMakingTrail(trail_name);
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
            getActivity().stopService(locationIntent);

            // Deals with the case if user starts and stops quickly
            if(viewModel.getPlayerPos() == null || viewModel.getCurrentTrail().size() == 0){
                viewModel.setString(getResources().getString(R.string.start_location));
                locationButton.setText(R.string.start_location);
                locButt_text = getResources().getString(R.string.start_location);
                Toast.makeText(getContext(),"Trail too short!", Toast.LENGTH_LONG).show();
                return;
            }else{
                //Empty trails shouldn't be saved into fire base
                storeIntoFirebase();
            }
            endTrail();
            viewModel.setString(getResources().getString(R.string.start_location));
            locationButton.setText(R.string.start_location);
            locButt_text = getResources().getString(R.string.start_location);
        }
    }

    /**
     * User has finished creating a trail, store the new trail info into firebase.
     */
    private void storeIntoFirebase() {
        Map<String, Object> payload = new HashMap<>();
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
        payload.put("name", viewModel.getCurrentTrailName());
        payload.put("time",new Timestamp(new Date()));
        //firebase only accepts geopoints, so we have to convert the trail's latlng to geopoint
        ArrayList<GeoPoint> gpTrail= new ArrayList<>();
        ArrayList<LatLng> trail = viewModel.getCurrentTrail();
        if(trail != null){
            for (LatLng t : trail) {
                gpTrail.add(new GeoPoint(t.latitude, t.longitude));
            }
            payload.put("trailPoints",gpTrail);
        }else{
            payload.put("trailPoints","");
        }

        // trail id's are just the userid with the trail name appended to it
        String trailId = mFireBaseAuth.getCurrentUser().getUid() + viewModel.getCurrentTrailName();
        //Adds in the payload
        fstore.collection("trails")
                .document(trailId)
                .set(payload)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(),"Trail successfully saved", Toast.LENGTH_SHORT).show();
                    }
                })
                //TODO  Maybe a try again thing later on
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),"Not saved to cloud", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Sets up the viewmodel for us to start making a new trail
     * @param name
     */
    public static void startMakingTrail(String name) {
        viewModel.resetTrail();
        viewModel.setCurrentTrailName(name);
        viewModel.setMakingTrail(true);
        for(String key : map_trails.keySet()) {
            map_trails.get(key).setVisible(false);
        }
        currentTrailOptions.visible(true);
    }

    /**
     * This function is called from onActivityCreated and is used to restore the trail state.
     * Not just for making trails; if the user was running a trail at the time, this function
     * should also restore that.
     * @param name
     * @param trail
     * @param isMaking
     * @param isRunning
     */
    public static void continueTrail(String name, ArrayList<LatLng> trail, boolean isMaking, boolean isRunning){
        viewModel.resetTrail();
        viewModel.setCurrentTrailName(name);
        viewModel.setCurrentTrail(trail);
        viewModel.setMakingTrail(isMaking);
        viewModel.setRunningTrail(isRunning);
        // don't display the trail if we aren't running or making one
        for(String key : map_trails.keySet()) {
            // don't want to render other trails if we are running/making one
            map_trails.get(key).setVisible(!(isRunning || isMaking));
        }
        currentTrailOptions.visible(isRunning || isMaking);
    }

    /**
     * Called if we want to get rid of the trail we are currently making
     */
    public static void discardTrail() {
        viewModel.setMakingTrail(false);
        viewModel.deleteTrail(viewModel.getCurrentTrailName());
        viewModel.setCurrentTrailName("");
        currentTrailOptions.visible(false);
        for(String key : map_trails.keySet()) {
            map_trails.get(key).setVisible(true);
        }
    }

    /**
     * Called when we're done making a new trail
     */
    public static void endTrail() {
        viewModel.setMakingTrail(false);
        viewModel.setCurrentTrailName("");
        //viewModel.stashTrail();
        currentTrailOptions.visible(false);
        for(String key : map_trails.keySet()) {
            map_trails.get(key).setVisible(true);
        }
    }

    /**
     * Called when the user would like to run an existing trail
     * @param name
     */
    public static void runTrail(String name) {
        viewModel.resetTrail();
        viewModel.setMakingTrail(false);
        viewModel.setRunningTrail(true);
        viewModel.setCurrentTrailName(name);
        viewModel.setCurrentTrail((ArrayList)viewModel.getTrailByName(name).get("trailPoints"));
        currentTrail.setPoints(viewModel.getCurrentTrail());
//        Log.e("Current Trail: ", currentTrail.getTag().toString());

        // don't render trails besides the one we are running
        for(String key : map_trails.keySet()) {
            if (!(key.endsWith(viewModel.getCurrentTrailName()))){
                map_trails.get(key).setVisible(false);
            }
        }
        currentTrailOptions.visible(true);
    }

    /**
     * Called if the user is running an existing trail and would like
     * to leave without having completed it
     */
    public static void leaveTrail() {
        viewModel.resetTrail();
        viewModel.setRunningTrail(false);
        viewModel.setCurrentTrailName("");
        currentTrailOptions.visible(false);
        for(String key : map_trails.keySet()) {
            map_trails.get(key).setVisible(true);
        }
        // award partial XP? if we have time
    }

    /**
     * Called when the user reaches the end of the trail they are running
     */
    public void completeTrail() {
        viewModel.resetTrail();
        viewModel.setRunningTrail(false);
        viewModel.setCurrentTrailName("");
        currentTrailOptions.visible(false);
        for(String key : map_trails.keySet()) {
            map_trails.get(key).setVisible(true);
        }
        // award XP
        updateExp();
    }

    private void updateExp() {

        fstore = FirebaseFirestore.getInstance();
        mFireBaseAuth = FirebaseAuth.getInstance();
        userID = mFireBaseAuth.getCurrentUser().getUid();

        fstore.collection("users").document(userID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                        snapshot = snap.getResult();
                        DocumentSnapshot snapshot = task.getResult();
                        String priorExp = snapshot.get("experience").toString();
                        int newExp = Integer.parseInt(priorExp) + 10;
                        fstore.collection("users").document(userID)
                                .update(
                                        "experience", newExp
                                )
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getContext(), "Experience Updated!", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    /*
    BroadCast receiver to interact with a local broadcast manager from Location Service.
    Below methods will interact with the maps fragment.
     */
    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e("Where am I?? ", "In the BroadcastReciever!!");

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
                currentTrail.setPoints(viewModel.getCurrentTrail());
            }

            // for now just check to see if the user has reached the last point on the trail
            if(viewModel.isRunningTrail()) {
                LatLng end = currentTrail.getPoints().get(currentTrail.getPoints().size() - 1);
                Log.e("Current LatLng: ", end.toString());
                if (getDistance(pos, end) < 2) {
                    completeTrail();
                    Log.e("Completed a trail: " , "Congrats!!");
                }
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
