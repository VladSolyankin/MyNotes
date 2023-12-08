package com.example.mynotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class SelectedNoteActivity extends AppCompatActivity {

    private EditText editNote;
    private TextView itemNameTextView;
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

        itemNameTextView = findViewById(R.id.itemNameTextView);
        String itemTitle = getIntent().getStringExtra("noteTitle");
        itemNameTextView.setText(itemTitle);

        editNote = findViewById(R.id.editTextNote);
        sharedPreferences = getSharedPreferences("MyNotesPrefs", Context.MODE_PRIVATE);

        String savedNote = sharedPreferences.getString(itemTitle, "");
        editNote.setText(savedNote);

        loadNoteContent();
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

        updateContentField();
    }

    private void loadNoteContent() {
        fireStore.collection("users").document(currentUser.getUid())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<Map<String, Object>> notes = (List<Map<String, Object>>) document.get("notes");

                            int indexToLoad = findIndexByTitle(notes, itemNameTextView.getText().toString());
                            if (indexToLoad >= 0 && indexToLoad < notes.size()) {
                                Map<String, Object> noteToLoad = notes.get(indexToLoad);
                                String content = (String) noteToLoad.get("content");

                                editNote.setText(content);
                            }
                        }
                    }
                });
    }

    private void updateContentField() {
        fireStore.collection("users").document(currentUser.getUid())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<Map<String, Object>> notes = (List<Map<String, Object>>) document.get("notes");

                            int indexToLoad = findIndexByTitle(notes, itemNameTextView.getText().toString());
                            if (indexToLoad >= 0 && indexToLoad < notes.size()) {
                                Map<String, Object> noteToLoad = notes.get(indexToLoad);
                                noteToLoad.put("content", editNote.getText().toString());

                                fireStore.collection("users").document(currentUser.getUid())
                                        .update("notes", notes);
                            }
                        }
                    }
                });
    }

    private int findIndexByTitle(List<Map<String, Object>> notes, String titleToFind) {
        for (int i = 0; i < notes.size(); i++) {
            Map<String, Object> note = notes.get(i);
            String title = (String) note.get("title");

            if (titleToFind.equals(title)) {
                return i;
            }
        }

        return -1;
    }


}