package com.example.mynotes;

import androidx.annotation.NonNull;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserNotesActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private RecyclerView recyclerView;
    private UserNotesAdapter itemAdapter;
    private List<NoteModel> notesList = new ArrayList<>();
    private Uri selectedImageUri;
    private int selectedImageResource;
    private ImageView dialogImageView;
    private FirebaseFirestore fireStore;
    private FirebaseAuth userAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_notes);

        recyclerView = findViewById(R.id.recyclerView);
        fireStore = FirebaseFirestore.getInstance();
        userAuth = FirebaseAuth.getInstance();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                itemAdapter.getFilter().filter(newText);
                return true;
            }
        });

        return true;
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
                        NoteModel note = new NoteModel(selectedImageUri, itemName, getCurrentDate());
                        itemAdapter.addItem(note);

                        FirebaseUser user = userAuth.getCurrentUser();

                        Map<String, Object> noteMap = new HashMap<>();
                        noteMap.put("title", note.getItemName());
                        noteMap.put("date", note.getDate());
                        noteMap.put("content", "");
                        noteMap.put("imagePath", note.getImageResource());

                        fireStore.collection("users")
                                .document(user.getUid())
                                .update("notes", FieldValue.arrayUnion(noteMap))
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        itemAdapter.notifyDataSetChanged();
                                    }
                                });
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

    private Date getCurrentDate() {
        return Calendar.getInstance().getTime();
    }
}