package dev.lifeStyleRPG;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterScreen extends AppCompatActivity {
    EditText emailId, password;
    Button btnSignUp;
    FirebaseAuth mFireBaseAuth;
    FirebaseFirestore fStore;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        mFireBaseAuth = FirebaseAuth.getInstance();

        fStore = FirebaseFirestore.getInstance();

        emailId = findViewById(R.id.editText);
        password = findViewById(R.id.editText2);
        btnSignUp = findViewById(R.id.button);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailId.getText().toString();
                String pwd = password.getText().toString();
                if(email.isEmpty()){
                    emailId.setError("Provide an email");
                    emailId.requestFocus();
                }
                else if(pwd.isEmpty()){
                    password.setError("Please enter your password");
                    password.requestFocus();
                }
                else if(!(email.isEmpty() && pwd.isEmpty())){
                    mFireBaseAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(RegisterScreen.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(RegisterScreen.this,"SignUp Unsuccessful, Please Try Again",Toast.LENGTH_SHORT).show();
                            }
                            else {
                                userID = mFireBaseAuth.getCurrentUser().getUid();

                                Map<String, Object> user = new HashMap<>();
                                user.put("login", email);
                                user.put("spriteID", 0);
                                user.put("level", 1);
                                user.put("experience", 0);
                                user.put("trails failed", 0);
                                user.put("userid", userID);
                                fStore.collection("users").document(userID).set(user);
                                startActivity(new Intent(RegisterScreen.this, MainActivity.class));
                            }
                        }
                    });
                }
                else{
                    Toast.makeText(RegisterScreen.this,"Error Occurred!",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
