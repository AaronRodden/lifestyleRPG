package dev.lifeStyleRPG;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.widget.Toast;

public class LocationService extends Service {
    //location manager
    private LocationManager locationManager;
    private LocationListener locationListener = new myLocationListener();

    //booleans for accepting/declining location access
    private boolean gps_enabled = false;
    private boolean network_enabled = false;

    //handler is in charge of running threads
    private Handler handler = new Handler();
    //thread this service runs on
    private Runnable rt;
    Intent intent;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //service onStartCommand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("asdf", "onStartCommand: ");
        this.intent = intent;
        //start the thread which will track location
        rt = new Runnable() {
            public void run() {
                location();
            }
        };
        handler.post(rt);
        return START_STICKY;
    }

    void location() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d("location", "hello");
        //I think this asks for permission first. Permissions should probably be asked earlier, like in the maps activity
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("location", "user did not grant permission");
        }else{
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    //we want to override onLocation changed function, plus do stuff within this class
    private class myLocationListener implements LocationListener{
        double lat_new,lon_new;

        @Override
        public void onLocationChanged(Location location) {
            //log the latitude and long
            if(location != null){
                locationManager.removeUpdates(locationListener);
                lat_new = location.getLatitude();
                lon_new = location.getLongitude();
                Log.v("Location-Listener", "Location changed " + lat_new + " " + lon_new);
            }

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }
}
