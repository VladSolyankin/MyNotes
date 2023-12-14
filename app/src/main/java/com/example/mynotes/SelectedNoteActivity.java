package com.example.mynotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
    private int currentTypeface = Typeface.NORMAL;
    private boolean isUnderlined = false;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.font_style_menu, menu);

        MenuItem boldStyle = menu.findItem(R.id.bold_style);
        MenuItem italicStyle = menu.findItem(R.id.italic_style);
        MenuItem underlineStyle = menu.findItem(R.id.underline_style);

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
                isUnderlined = false;
                editNote.setText(spannable);
                return;
            }
        }

        spannable.setSpan(underlineSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        isUnderlined = true;

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
                    currentTypeface = Typeface.NORMAL;
                    editNote.setText(spannable);
                    return;
                }
            }
        }

        spannable.setSpan(new StyleSpan(typeface), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        editableText.replace(0, editableText.length(), spannable);
    }
}