package com.example.mynotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class
RegisterUserActivity extends AppCompatActivity {

    private FirebaseAuth userAuth;
    private FirebaseFirestore fireStore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        userAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();


        TextView title = findViewById(R.id.titleTextView);
        title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        title.setShadowLayer(10f, 2f, 2f, Color.BLACK);

        TextInputEditText usernameEditText = findViewById(R.id.usernameEditText);
        TextInputEditText emailEditText = findViewById(R.id.emailEditText);
        TextInputEditText phoneEditText = findViewById(R.id.phoneEditText);
        TextInputEditText passwordEditText = findViewById(R.id.passwordEditText);

        Button registerButton = findViewById(R.id.registerButton);
        TextView loginTextView = findViewById(R.id.loginTextView);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = String.valueOf(emailEditText.getText());
                String password = String.valueOf(passwordEditText.getText());

                userAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d("Log: ", "createUserWithEmail:success");
                                    FirebaseUser user = userAuth.getCurrentUser();

                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("name", String.valueOf(usernameEditText.getText()));
                                    userData.put("email", user.getEmail());
                                    userData.put("phone", String.valueOf(phoneEditText.getText()));
                                    userData.put("notes", new ArrayList<>());

                                    DocumentReference userDocument = fireStore
                                            .collection("users")
                                            .document(user.getUid());

                                    userDocument.set(userData);

                                    Intent intent = new Intent(RegisterUserActivity.this, UserNotesActivity.class);
                                    startActivity(intent);
                                } else {
                                    Log.w("Log: ", "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(RegisterUserActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
            }
        });
        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterUserActivity.this, LoginUserActivity.class);
                startActivity(intent);
            }
        });
    }

}