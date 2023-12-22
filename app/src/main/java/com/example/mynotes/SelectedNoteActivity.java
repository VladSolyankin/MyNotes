package com.example.mynotes;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectedNoteActivity extends AppCompatActivity {

    private EditText editNote;
    private TextView itemNameTextView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore fireStore;
    FirebaseUser currentUser;
    private ImageSwitcher noteImageSwitcher;
    private ImageView nextPictureButton;
    private ImageView previousPictureButton;
    private ImageView newImageButton;
    private AlertDialog alertDialog;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private List<String> images = new ArrayList<>();;
    private int position;
    private int noteIndex;
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

        loadNoteContent();

        newImageButton = findViewById(R.id.addNewPictureButton);
        noteImageSwitcher = findViewById(R.id.noteImageSwitcher);
        noteImageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new
                        ImageSwitcher.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                imageView.setBackgroundColor(0x00000000);
                return imageView;
            }
        });
        previousPictureButton = findViewById(R.id.previousPictureButton);
        nextPictureButton = findViewById(R.id.nextPictureButton);

        if (images.size() != 0) {
            noteImageSwitcher.setImageURI(Uri.parse(images.get(0)));
        }

        pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {


                    if (uri != null) {
                        Log.d("PhotoPicker", "Selected URI: " + uri);
                        int flag = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getApplicationContext().getContentResolver().takePersistableUriPermission(uri, flag);
                        images.add(String.valueOf(uri));
                        updateNoteImages();
                        noteImageSwitcher.setImageURI(uri);
                    } else {
                        Log.d("PhotoPicker", "No media selected");
                    }
                });

        newImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItemDialog();
            }
        });

        previousPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPositionPrev();
                noteImageSwitcher.setImageURI(Uri.parse(images.get(position)));
            }
        });

        nextPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPositionNext();
                noteImageSwitcher.setImageURI(Uri.parse(images.get(position)));
            }
        });

        getNoteImages(itemTitle);
    }

    private void getNoteImages(String itemTitle) {
        fireStore.collection("users")
                .document(currentUser.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            List<Map<String, Object>> notes = (List<Map<String, Object>>) documentSnapshot.get("notes");
                            for (Map<String, Object> note: notes) {
                                if (note.get("title").toString().equals(itemTitle)) {
                                    images = (List<String>) note.get("noteImages");
                                    noteIndex = notes.indexOf(note);
                                    noteImageSwitcher.setImageURI(Uri.parse(images.get(0)));
                                }
                            }
                        }
                    });
    }

    private void updateNoteImages() {
        fireStore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Map<String, Object>> notes = (List<Map<String, Object>>) documentSnapshot.get("notes");

                    if (noteIndex >= 0 && noteIndex < notes.size()) {
                        Map<String, Object> note = notes.get(noteIndex);
                        note.put("noteImages", images);

                        fireStore.collection("users").document(currentUser.getUid())
                                .update("notes", notes)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Update", "NoteImages updated successfully");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Update", "Error updating NoteImages", e);
                                });
                    }
                });
    }

    private void showItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);
        alertDialog = builder.create();

        ImageView dialogImageView = dialogView.findViewById(R.id.imageView);

        dialogImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());

                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    public void setPositionNext() {
        position++;
        if (position > images.size() - 1) {
            position = 0;
        }
    }

    public void setPositionPrev() {
        position--;
        if (position < 0) {
            position = images.size() - 1;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
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
                                String htmlContent = (String) noteToLoad.get("content");

                                Spanned spanned = Html.fromHtml(htmlContent);
                                editNote.setText(new SpannableStringBuilder(spanned));
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
                                noteToLoad.put("content", getNoteStyles());

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.font_style_menu, menu);

        MenuItem boldStyle = menu.findItem(R.id.bold_style);
        MenuItem italicStyle = menu.findItem(R.id.italic_style);
        MenuItem underlineStyle = menu.findItem(R.id.underline_style);
        MenuItem normalAlignment = menu.findItem(R.id.left_alignment);
        MenuItem centerAlignment = menu.findItem(R.id.center_alignment);
        MenuItem oppositeAlignment = menu.findItem(R.id.right_alignment);

        boldStyle.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                applyStyleToSelection(Typeface.BOLD);
                return false;
            }
        });
        italicStyle.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                applyStyleToSelection(Typeface.ITALIC);
                return false;
            }
        });
        underlineStyle.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                applyUnderlineStyleToSelection(new UnderlineSpan());
                return false;
            }
        });
        normalAlignment.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                applyAlignmentToSelection(Layout.Alignment.ALIGN_NORMAL);
                return false;
            }
        });
        centerAlignment.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                applyAlignmentToSelection(Layout.Alignment.ALIGN_CENTER);
                return false;
            }
        });
        oppositeAlignment.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                applyAlignmentToSelection(Layout.Alignment.ALIGN_OPPOSITE);
                return false;
            }
        });

        return true;
    }

    private void applyUnderlineStyleToSelection(UnderlineSpan underlineSpan) {
        int start = editNote.getSelectionStart();
        int end = editNote.getSelectionEnd();

        Editable editable = editNote.getText();
        SpannableStringBuilder spannable = new SpannableStringBuilder(editable);

        UnderlineSpan[] underlineSpans = spannable.getSpans(start, end, UnderlineSpan.class);

        for (UnderlineSpan span : underlineSpans) {
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            if (spanStart <= start && spanEnd >= end) {
                spannable.removeSpan(span);
                editNote.setText(spannable);
                return;
            }
        }

        spannable.setSpan(underlineSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        editable.replace(0, editable.length(), spannable);
    }

    private void applyStyleToSelection(int typeface) {
        int selectionStart = editNote.getSelectionStart();
        int selectionEnd = editNote.getSelectionEnd();

        Editable editableText = editNote.getText();
        SpannableStringBuilder spannable = new SpannableStringBuilder(editableText);

        if (!TextUtils.isEmpty(spannable)) {
            StyleSpan[] styleSpans = spannable.getSpans(selectionStart, selectionEnd, StyleSpan.class);

            for (StyleSpan styleSpan : styleSpans) {
                int spanStart = spannable.getSpanStart(styleSpan);
                int spanEnd = spannable.getSpanEnd(styleSpan);
                if (spanStart <= selectionStart && spanEnd >= selectionEnd && styleSpan.getStyle() == typeface) {
                    spannable.removeSpan(styleSpan);
                    editNote.setText(spannable);
                    return;
                }
            }
        }

        spannable.setSpan(new StyleSpan(typeface), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        editableText.replace(0, editableText.length(), spannable);
    }

    private String getNoteStyles() {
        Editable editable = editNote.getText();
        return Html.toHtml(editable);
    }

    private void applyAlignmentToSelection(Layout.Alignment alignment) {
        int start = editNote.getSelectionStart();
        int end = editNote.getSelectionEnd();

        Editable editable = editNote.getText();
        SpannableStringBuilder spannable = new SpannableStringBuilder(editable);

        AlignmentSpan[] alignmentSpans = spannable.getSpans(start, end, AlignmentSpan.class);
        for (AlignmentSpan span : alignmentSpans) {
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            if (spanStart <= start && spanEnd >= end) {
                spannable.removeSpan(span);
            }
        }

        spannable.setSpan(new AlignmentSpan.Standard(alignment), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        editable.replace(0, editable.length(), spannable);

        updateContentField();
    }

}
