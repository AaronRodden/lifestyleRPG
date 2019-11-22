package dev.lifeStyleRPG;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
                    StringBuilder trailstxt = new StringBuilder("Trails Failed: \n");
                    trailstxt.append(document.get("trails failed").toString());
                    exp.append(document.get("experience").toString());
                    exptext.setText(exp.toString());
                    trailsdone.setText(trailstxt.toString());
                    mainmenu.setText(theuser);
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });



    }

}
