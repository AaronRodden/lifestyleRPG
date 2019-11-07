package dev.lifeStyleRPG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Trails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trails);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.maps_screen:
                        Intent mapsIntent = new Intent(getApplicationContext(),MapsActivity.class);
                        startActivity(mapsIntent);
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
}
