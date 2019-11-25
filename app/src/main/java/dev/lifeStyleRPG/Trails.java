package dev.lifeStyleRPG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The idea of this activity is for the user to locate other trails on the map.
 *
 * We can search either by nearest location.
 *
 * Display out clickable rows that has the trails. User clicks on the trails, the maps activity/fragment
 * is brought to the foreground with the camera zoomed to the trail. Or something like that.
 *
 * This activity will be started for a result, and the result will be something that the Maps Activity
 * can use to locate the trail, maybe a trailid or something.
 *
 * Open to other idea.
 */
public class Trails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trails);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().findItem(R.id.trails_screen).setChecked(true);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.maps_screen:
                        Intent mapsIntent = new Intent(getApplicationContext(),MapsActivity.class);
                        mapsIntent.putExtra("result", "hello");
                        //Trail search update camera. The reasoning was for the trails activity to send a result to
                        //the maps activity, and the maps activity interacts with methods in map fragment to interact
                        //with the map. I set that up, but update camera never seems to be called, and I'm not sure why
                        mapsIntent.putExtra("LatLng", new LatLng(42.23,12.32));
                        setResult(Activity.RESULT_OK, mapsIntent);
                        //don't send any data
                        //setResult(Activity.RESULT_CANCELLED, mapsIntent);
                        finish();
                        break;
                    case R.id.player_profile_screen:
                        break;
                    case R.id.trails_screen:
                        break;
                    case R.id.settings_screen:
                        break;
                }
                return true;
            }
        });
    }
    @Override
    protected void onPause(){
        super.onPause();
        Log.e("Trails", "On Pause");
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.e("Trails", "On Destroy");

    }
}
