package com.example.mynotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class SelectedNoteActivity extends AppCompatActivity {

    private EditText editNote;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore fireStore;
    FirebaseUser currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_note);

        fireStore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        TextView itemNameTextView = findViewById(R.id.itemNameTextView);
        String itemTitle = getIntent().getStringExtra("noteTitle");
        itemNameTextView.setText(itemTitle);

        editNote = findViewById(R.id.editTextNote);
        sharedPreferences = getSharedPreferences("MyNotesPrefs", Context.MODE_PRIVATE);

        String savedNote = sharedPreferences.getString(itemTitle, "");
        editNote.setText(savedNote);
    }

    @Override
    protected void onPause() {
        super.onPause();
        String itemName = getIntent().getStringExtra("noteTitle");
        String noteText = editNote.getText().toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(itemName, noteText);
        editor.apply();

        String contentField = "content";

        fireStore.collection("users").document(currentUser.getUid())
                .get().addOnSuccessListener(documentSnapshot -> {

                    Map<String, Object> notesList = (Map<String, Object>) documentSnapshot.get("notes");

                    Map<String, Object> currentObject = (Map<String, Object>) documentSnapshot.get(contentField);
                    currentObject.put(contentField, noteText);

                    fireStore.collection("users").document(currentUser.getUid())
                            .update(contentField, currentObject);
                });
    }
}