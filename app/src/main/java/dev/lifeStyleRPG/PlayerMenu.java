package dev.lifeStyleRPG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerMenu extends AppCompatActivity {
    FirebaseFirestore fStore;
    FirebaseAuth mFireBaseAuth;
    TextView mainmenu;
    TextView exptext;
    TextView trailsdone;
    TextView trailscreated;

    BottomNavigationView bottomNavigationView;


    private static final String TAG = "PlayerMenu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_menu);

        fStore = FirebaseFirestore.getInstance();
        mFireBaseAuth = FirebaseAuth.getInstance();
        String userID = mFireBaseAuth.getCurrentUser().getUid();
        mainmenu = findViewById(R.id.textView);
        exptext = findViewById(R.id.totalEXP);
        trailsdone = findViewById(R.id.trailsDone);
        trailscreated = findViewById(R.id.trailsCreated);
//        trailscreated = findViewById(R.id.)
        DocumentReference docRef = fStore.collection("users").document(userID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    StringBuilder fields = new StringBuilder("");
                    fields.append(document.get("login"));
                    String theuser = fields.toString();
                    StringBuilder exp = new StringBuilder("Total EXP: \n");
                    StringBuilder trailstxt = new StringBuilder("Trails Completed: \n");
                    StringBuilder trailsCreatedText = new StringBuilder("Trails Created: \n");
                    if(document.get("trails completed") != null)
                        trailstxt.append(document.get("trails completed").toString());

                    exp.append(document.get("experience").toString());
                    if(document.get("trails created") != null)
                        trailsCreatedText.append(document.get("trails created").toString());
                    exptext.setText(exp.toString());
                    trailsdone.setText(trailstxt.toString());
                    trailscreated.setText(trailsCreatedText.toString());
                    mainmenu.setText(theuser);
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().findItem(R.id.player_profile_screen).setChecked(true);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.maps_screen:
                        Intent mapsIntent = new Intent(getApplicationContext(),MapsActivity.class);
                        //Trail search update camera. The reasoning was for the trails activity to send a result to
                        //the maps activity, and the maps activity interacts with methods in map fragment to interact
                        mapsIntent.putExtra("LatLng", new LatLng(42.23,12.32));
                        setResult(Activity.RESULT_OK, mapsIntent);
                        //don't send any data
                        //setResult(Activity.RESULT_CANCELLED, mapsIntent);
                        finish();
                        break;
                    case R.id.player_profile_screen:
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
    }
}
