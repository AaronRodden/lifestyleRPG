package dev.lifeStyleRPG;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button btn = (Button)findViewById(R.id.loginButton);

        btn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, MapsActivity.class));
            }
    });
//        TextView textView = (TextView) findViewById(R.id.mainTitle);
//        Typeface typeface = Typeface.createFromAsset(getAssets(), R.font.splatch);
//        textView.setTypeface(typeface);
    }
}
