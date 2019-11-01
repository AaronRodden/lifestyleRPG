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

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

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

    //for communicating with maps activity
    LocalBroadcastManager localBroadcastManager;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        Log.d("location-service", "on-Destroy");
        handler.removeCallbacks(rt);
        //stop the updates
        if (locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
        super.onDestroy();
    }
    //service onStartCommand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("location-service", "onStartCommand: ");
        this.intent = intent;
        //start the thread which will track location
        rt = new Runnable() {
            @Override
            public void run() {
                location();
                handler.postDelayed(rt, 5000);
            }
        };
        handler.post(rt);
        return START_STICKY;
    }

    void location() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //I think this asks for permission first. Permissions should probably be asked earlier, like in the maps activity
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("location-service", "user did not grant permission");
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    public void sendMessage(){
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent("sample-event");
        intent.putExtra("message", "hello");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //we want to override onLocation changed function, plus do stuff within this class
    private class myLocationListener implements LocationListener {
        double lat_new,lon_new;
        /*
        These are Location Listener methods
         */
        @Override
        public void onLocationChanged(Location location) {
            //log the latitude and long
            if(location != null){
                locationManager.removeUpdates(locationListener);
                lat_new = location.getLatitude();
                lon_new = location.getLongitude();
                Log.v("location-service", "Location changed " + lat_new + " " + lon_new);
                sendMessage();
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
