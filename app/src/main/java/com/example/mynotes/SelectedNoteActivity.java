package com.example.mynotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class SelectedNoteActivity extends AppCompatActivity {

    private EditText editNote;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_note);


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
    }
}