package dev.lifeStyleRPG;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class mapsViewModel extends ViewModel {
    private String current_text = "Track Location" ;
    private LatLng current_loc;
    private String newTrailName = "";
    private boolean makingTrail = false;
    //This is an arraylist for current user creating their trail.
    private ArrayList<LatLng> newTrail = new ArrayList<>();
    //This is a map of all the trails located on the map
    //Index by trailID instead of trail name
   //private HashMap<String, ArrayList<LatLng>> trails = new HashMap<>();
    private HashMap<String, Map> trails = new HashMap<>();


    public String get_current_text(){
        return current_text;
    }

    public void setString(String s){
        current_text = s;
    }

    public void setPlayerPos(LatLng pos) {current_loc = pos;}
    public LatLng getPlayerPos() {return current_loc;}

    // I don't know if these actually need to be synchronized; it sounds like the location
    // service runs in a separated thread? But it all goes through our main maps activity
    // so idk
    public synchronized void resetTrail() {
        newTrail = new ArrayList<>();
        // info logs?
        Log.i("maps-viewmodel", "Trail reset");
    }

    public synchronized void deleteTrail(String name) {
        if(newTrailName.equals(name)) {
            newTrail.clear();
            makingTrail = false;
        }
        trails.remove(name);
        Log.i("maps-viewmodel", "Removed trail with name:" + name);
    }

    public synchronized void setMakingTrail(boolean bool) {
        makingTrail = bool;
    }

    public boolean isMakingTrail() {
        return makingTrail;
    }

    public synchronized void setTrailName(String name) {
        newTrailName = name;
    }

    public String getTrailName() {
        return newTrailName;
    }

    public ArrayList<LatLng> getTrail() {
        return newTrail;
    }

    public HashMap<String, Map> returnTrailMap() {return trails;}

    public synchronized void insertTrail(String trailId, Map data) {
        Iterator<GeoPoint> it = ((ArrayList)data.get("trailPoints")).iterator();
        ArrayList<LatLng> temp = new ArrayList<>();

        while(it.hasNext()){
            GeoPoint t = it.next();
            temp.add(new LatLng(t.getLatitude(),t.getLongitude()));
        }
        data.put("trailPoints", temp);
        trails.put(trailId, data);
        Log.e("viewModel", trails.get(trailId).toString());
    }
    /*public synchronized void stashTrail() {
        trails.put(newTrailName, newTrail);
    }*/

    public synchronized void appendToTrail(LatLng toAppend) {
        newTrail.add(toAppend);
        // verbose logs?
        Log.v("maps-viewmodel", "Added new point to trail");
    }
    public void setTrail(ArrayList<LatLng> prefix){
        newTrail = new ArrayList<>(prefix);
    }

    public LatLng getLastPos() {
        return newTrail.size() == 0 ? null : newTrail.get(newTrail.size() - 1);
    }
}
