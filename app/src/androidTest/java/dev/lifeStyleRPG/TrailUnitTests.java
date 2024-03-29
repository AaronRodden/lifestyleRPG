package dev.lifeStyleRPG;

import android.content.BroadcastReceiver;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProviders;
import androidx.test.core.app.ActivityScenario;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static dev.lifeStyleRPG.MapFragment.endTrail;
import static dev.lifeStyleRPG.MapFragment.runTrail;
import static dev.lifeStyleRPG.MapFragment.startMakingTrail;

public class TrailUnitTests {
    public static mapsViewModel viewModel;
    public ActivityScenario<MapsActivity> mapsScenario;

    @Test
    public void CreateTrailCheckTrail() {
        mapsScenario = ActivityScenario.launch(MapsActivity.class);
        mapsScenario.onActivity(activity -> {
            viewModel = ViewModelProviders.of(activity).get(mapsViewModel.class);
            startMakingTrail("unitTestTrail");
            endTrail();
        });
        mapsScenario.close();
    }

    @Test
    public void ChangeScreens() {

    }

    @Test
    public void RunExistingTrail() {
        mapsScenario = ActivityScenario.launch(MapsActivity.class);
        mapsScenario.moveToState(Lifecycle.State.CREATED);
        mapsScenario.onActivity(activity -> {
            // need reflection to get the fragment
            try{
                Field viewModelField = MapFragment.class.getDeclaredField("viewModel");
                viewModelField.setAccessible(true);
                viewModel = (mapsViewModel) viewModelField.get(activity.mapFragment);
            } catch(IllegalAccessException e) {
                Assert.fail("Exception while getting viewmodel; testing failed!");
            } catch(NoSuchFieldException e) {
                Assert.fail("Field was not a member of class MapFragment; testing failed!");
            }

            Assert.assertNotNull(viewModel);

            // testing data
            int gpListSize = 10;
            ArrayList<GeoPoint> dummyGpList = new ArrayList<>(gpListSize);
            for(int i = 0; i < gpListSize; ++i) {
                dummyGpList.add(new GeoPoint(Math.random(), Math.random()));
            }
            HashMap<String, Object> dummyTrail = new HashMap<>();
            String userId = "arandomnumber1337";
            String trailName = "Dummy Thick";
            dummyTrail.put("userid", userId);
            dummyTrail.put("name", trailName);
            dummyTrail.put("time", new Timestamp(new Date()));
            dummyTrail.put("trailPoints", dummyGpList);
            String trailId = userId + trailName;
            viewModel.insertTrail(trailId, dummyTrail);
            runTrail(trailName);
            Assert.assertTrue(viewModel.isRunningTrail());

            // more reflection to get access to the broadcast receiver
            BroadcastReceiver hackedReceiver = null;
            try {
                Field receiverField = MapFragment.class.getDeclaredField("myBroadcastReceiver");
                receiverField.setAccessible(true);
                hackedReceiver = (BroadcastReceiver) receiverField.get(activity.mapFragment);
            } catch(IllegalAccessException e) {
                Assert.fail("Exception while accessing broadcast receiver; testing failed!");
            } catch(NoSuchFieldException e) {
                Assert.fail("Field was not a member of class MapFragment; testing failed!");
            }

            Assert.assertNotNull(hackedReceiver);

            Intent dummyIntent = new Intent();
            dummyIntent.putExtra("lat", dummyGpList.get(gpListSize-1).getLatitude());
            dummyIntent.putExtra("lon", dummyGpList.get(gpListSize-1).getLongitude());
            hackedReceiver.onReceive(null, dummyIntent);

            Assert.assertFalse(viewModel.isRunningTrail());
        });
        mapsScenario.close();
    }
}
