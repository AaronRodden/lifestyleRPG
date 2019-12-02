package dev.lifeStyleRPG; import android.util.Log; 
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * View model to save fragment UI state. Contains metadata on button text, trails on the map,
 * and player trail's.
 *
 */
public class mapsViewModel extends ViewModel {
    //For startlocation button
    private String currentText = "Start Quest" ;
    private LatLng currentLocation;
    private String currentTrailName = "";
    private boolean makingTrail = false;
    private boolean runningTrail = false;
    // This is an arraylist for the trail being run or created
    private ArrayList<LatLng> currentTrail = new ArrayList<>();
    // This is a map of all the trails located on the map
    // Index by trailID instead of trail name
    private HashMap<String, Map> trails = new HashMap<>();


    public String getCurrentText(){
        return currentText;
    }

    public void setString(String s) { currentText = s; }

    public void setPlayerPos(LatLng pos) { currentLocation = pos; }
    public LatLng getPlayerPos() { return currentLocation; }

    // I don't know if these actually need to be synchronized; it sounds like the location
    // service runs in a separated thread? But it all goes through our main maps activity
    // so idk
    public synchronized void resetTrail() {
        currentTrail = new ArrayList<>();
        // info logs?
        Log.i("maps-viewmodel", "Trail reset");
    }

    public synchronized void deleteTrail(String name) {
        if(currentTrailName.equals(name)) {
            currentTrail.clear();
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

    public synchronized void setRunningTrail(boolean bool) {
        runningTrail = bool;
    }

    public boolean isRunningTrail() {
        return runningTrail;
    }

    public synchronized void setCurrentTrailName(String name) {
        currentTrailName = name;
    }

    public String getCurrentTrailName() {
        return currentTrailName;
    }

    public synchronized void setCurrentTrail(ArrayList<LatLng> trail) {
        currentTrail = trail;
    }

    public ArrayList<LatLng> getCurrentTrail() {
        return currentTrail;
    }

    public ArrayList<LatLng> getTrailById(String id) {
        return (ArrayList) trails.get(id).get("trailPoints");
    }

    // TODO: get rid of this and add a way for the user to select a trail without knowing
    // the ID of the user that created it
    public Map getTrailByName(String name) {
        for(String key : trails.keySet()) {
            Log.e("viewmodel", "comparing key " + key);
            if(key.endsWith(name)) {
                return trails.get(key);
            }
        }
        Log.e("In TrailByName function", "Wow here I am ");
        return null;
    }

    public HashMap<String, Map> getTrailMap() { return trails; }

    /**
     * This function is called to store firebase document containing trail information into the
     * hashmap trails. data contains trailPoints, name (trail name), userid, time, location.
     * @param trailId
     * @param data
     */
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
        trails.put(currentTrailName, currentTrail);
    }*/

    public synchronized void appendToTrail(LatLng toAppend) {
        currentTrail.add(toAppend);
        // verbose logs?
        Log.v("maps-viewmodel", "Added new point to trail");
    }

    //Keep creating trail from a pause
    public void continueTrail(ArrayList<LatLng> prefix){
        currentTrail = new ArrayList<>(prefix);
    }

    //get last position of current trail
    public LatLng getLastPos() {
        return currentTrail.size() == 0 ? null : currentTrail.get(currentTrail.size() - 1);
    }
}
