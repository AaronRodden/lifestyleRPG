package dev.lifeStyleRPG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity{
    MapFragment mapFragment;
    LatLng updated_view;
    FragmentManager manager = getSupportFragmentManager();
    public static final int REQUESTCODE = 1;
    BottomNavigationView bottomNavigationView;

//    FirebaseFirestore fStore;
//    FirebaseAuth mFireBaseAuth;
//    String userID;
//    EditText emailId;

    //This is called whenever the activity is started up, or when momentarily stopped
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MapsAcvitity", "onCreate");

        setContentView(R.layout.activity_maps);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.maps_screen:
                        break;
                    case R.id.player_profile_screen:
                        Intent profileIntent = new Intent(getApplicationContext(), PlayerMenu.class);
                        startActivity(profileIntent);
                        break;
                    case R.id.trails_screen:
                        /*Intent trailsIntent = new Intent(getApplicationContext(),Trails.class);
                        startActivityForResult(trailsIntent, REQUESTCODE);
                        */
                        break;
                    case R.id.settings_screen:
                        break;
                }
                return true;
            }
        });
        /**
         * TODO, currently deals with state changes within the activity, when leaving activity
         * it is desired to save the information on the map, especially when the user is still tracking
         * location. Fragment's OnPause is called, therefore we are still able to obtain updates from
         * the location service.
         */
        if (savedInstanceState != null){
            Log.e("MapsActivity", "savedInstance not null");
            mapFragment = (MapFragment) manager.getFragment(savedInstanceState,"maps_fragment");
        }else {
            Log.e("MapsActivity", "savedInstance null!");
            mapFragment = new MapFragment();
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            fragmentTransaction.add(R.id.map_container, mapFragment);
            fragmentTransaction.commit();
        }
    }
    //Save the fragment's state when leaving. This happens when onPause is called
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e("MapsAcvitity", "onSaveInstance");
            //Save the fragment's instance
        outState.putBoolean("started", true);
        manager.putFragment(outState, "maps_fragment", mapFragment);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUESTCODE){
            if(resultCode == Activity.RESULT_OK){
                Log.e("MapsActivity", "There is some result from trails activity");
                updated_view = data.getParcelableExtra("LatLng");
                Bundle bundle = new Bundle();
                bundle.putParcelable("updated_view", updated_view);
                //Trail search update camera. The reasoning was for the trails activity to send a result to
                //the maps activity, and the maps activity interacts with methods in map fragment to interact
                //mapFragment.updateCamera(bundle);
            }
            if(resultCode == Activity.RESULT_CANCELED){
                Log.e("MapsActivity", "No result from trails activity");
            }
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        bottomNavigationView.getMenu().findItem(R.id.maps_screen).setChecked(true);
    }

    //User pauses the activity, like goes on to a different activity.
    @Override
    protected void onPause(){
        super.onPause();
        Log.e("MapsActivity", "OnPause");
    }


//    public void updateExp() {
//
//        fStore = FirebaseFirestore.getInstance();
//        mFireBaseAuth = FirebaseAuth.getInstance();
//
//
//        userID = mFireBaseAuth.getCurrentUser().getUid();
//        emailId = findViewById(R.id.editText);
//
//        TextView exptext;
//        TextView trailsdone;
//        TextView level;
//        exptext = findViewById(R.id.totalEXP);
//        trailsdone = findViewById(R.id.trailsDone);
////        level = findViewById()
//        String temp = exptext.toString();
//        int numTemp = Integer.parseInt(temp) + 10;
//
////        Map<String, Object> user = new HashMap<>();
////        user.put("login", emailId);
////        user.put("spriteID", 0);
////        user.put("level", 1);
////        user.put("experience", numTemp); //update experience
////        user.put("trails failed", 0);
////        user.put("userid", userID);
//
//        // Document parameters = userID
//        fStore.collection("trails").document(userID)
//                .update(
//                        "experience", numTemp
//                )
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Toast.makeText(getApplicationContext(), "Experience Updated!", Toast.LENGTH_LONG).show();
//                    }
//                });
//    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.e("MapsAcvitity", "onDestroy");
    }
}
