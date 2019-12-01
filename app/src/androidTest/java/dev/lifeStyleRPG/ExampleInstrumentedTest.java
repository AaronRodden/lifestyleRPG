package dev.lifeStyleRPG;

import android.content.Context;
//import android.support.test.InstrumentationRegistry;
//import android.support.test.runner.AndroidJUnit4;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        FirebaseAuth mFireBaseAuth;
        FirebaseFirestore fStore;
        String userID;

        FirebaseApp.initializeApp(appContext);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String email = "unitTestEmail@email.com";

        Map<String, Object> user = new HashMap<>();
        user.put("login", email);
        user.put("spriteID", 0);
        user.put("level", 1);
        user.put("experience", 0);
        user.put("trails failed", 0);

        Task<QuerySnapshot> snapshotBefore = db.collection("users").get();
        db.collection("users").add(user);
        Task<QuerySnapshot> snapshotAfter = db.collection("users").get();

        Assert.assertNotEquals(snapshotBefore,snapshotAfter);

    }
}
