package dev.lifeStyleRPG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

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
