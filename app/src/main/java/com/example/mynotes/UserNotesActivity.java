package com.example.mynotes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserNotesActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private RecyclerView recyclerView;
    private UserNotesAdapter itemAdapter;
    private List<NoteModel> notesList = new ArrayList<>();
    private Uri selectedImageUri;
    private int selectedImageResource;
    private ImageView dialogImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_notes);

        recyclerView = findViewById(R.id.recyclerView);

        Spinner sortSpinner = findViewById(R.id.sortSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                handleSortSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("error: ", " spinner failed");
            }
        });

        itemAdapter = new UserNotesAdapter(notesList);
        recyclerView.setAdapter(itemAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        FloatingActionButton addItemButton = findViewById(R.id.addItemButton);
        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddItemDialog();
            }
        });

    }

    private void handleSortSelection(int position) {
        switch (position) {
            case 0:
                itemAdapter.setSortByDate(false);
                itemAdapter.setAscendingOrder(true);
                break;
            case 1:
                itemAdapter.setSortByDate(false);
                itemAdapter.setAscendingOrder(false);
                break;
            case 2:
                itemAdapter.setSortByDate(true);
                itemAdapter.setAscendingOrder(true);
                break;
            case 3:
                itemAdapter.setSortByDate(true);
                itemAdapter.setAscendingOrder(false);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Picasso.get().load(selectedImageUri).into(dialogImageView);
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri selectedImageUri) {
        String result = null;
        if (Objects.equals(selectedImageUri.getScheme(), "content")) {
            Cursor cursor = getContentResolver().query(selectedImageUri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = selectedImageUri.getLastPathSegment();
        }
        return result;
    }

    private void showAddItemDialog() {
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);
        alertDialog = builder.create();

        final EditText editTextName = dialogView.findViewById(R.id.editTextName);
        dialogImageView = dialogView.findViewById(R.id.imageView);

        dialogImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        Button addButton = dialogView.findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String itemName = editTextName.getText().toString().trim();
                if (!itemName.isEmpty()) {
                    if (!isTitleDuplicated(itemName)) {
                        itemAdapter.addItem(new NoteModel(selectedImageUri, itemName));
                        itemAdapter.notifyDataSetChanged();
                        alertDialog.dismiss();
                    }
                    else {
                        Toast.makeText(UserNotesActivity.this, "Title duplicated", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(UserNotesActivity.this, "Empty title", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.show();
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    private boolean isTitleDuplicated(String itemName) {
        for (NoteModel note: itemAdapter.getItemList()) {
            if (Objects.equals(note.getItemName(), itemName)) return true;
        }
        return false;
    }
}