package dev.lifeStyleRPG;
import androidx.lifecycle.ViewModel;

public class mapsViewModel extends ViewModel {
    private String current_text = "Track Location" ;


    public String get_current_text(){
        return current_text;
    }

    public void setString(String s){
        current_text = s;
    }

}
