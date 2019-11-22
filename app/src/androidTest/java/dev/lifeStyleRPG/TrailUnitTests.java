package dev.lifeStyleRPG;

import androidx.lifecycle.ViewModelProviders;

import org.junit.Test;

import static dev.lifeStyleRPG.MapsActivity.endTrail;
import static dev.lifeStyleRPG.MapsActivity.startTrail;

public class TrailUnitTests {
    public static mapsViewModel viewModel;
    MapsActivity mapsObj = new MapsActivity();
    @Test
    public void CreateTrailCheckTrail() {
        viewModel = ViewModelProviders.of(mapsObj).get(mapsViewModel.class);
        startTrail("unitTestTrail");
        endTrail();
    }
}
