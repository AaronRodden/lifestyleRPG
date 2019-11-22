package dev.lifeStyleRPG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapsActivity extends AppCompatActivity{
    MapFragment mapFragment;

    FragmentManager manager = getSupportFragmentManager();
    public static final int REQUESTCODE = 1;
    BottomNavigationView bottomNavigationView;

    //This is called whenever the activity is started up, or when momentarily stopped
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MapsAcvitity", "onCreate");

        setContentView(R.layout.activity_maps);


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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.maps_screen:
                        break;
                    case R.id.player_profile_screen:
                        break;
                    case R.id.trails_screen:
                        Intent trailsIntent = new Intent(getApplicationContext(),Trails.class);
                        startActivityForResult(trailsIntent, REQUESTCODE);
                        break;
                    case R.id.settings_screen:
                        break;
                }
                return true;
            }
        });
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
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.e("MapsAcvitity", "onDestroy");
        //unregister the listener
    }
}
