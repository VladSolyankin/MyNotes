package com.example.mynotes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserNotesAdapter extends RecyclerView.Adapter<UserNotesAdapter.ViewHolder> {

    private List<NoteModel> itemList;
    private List<NoteModel> filteredNotesList;
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean sortByDate;
    private boolean ascendingOrder;
    private final FirebaseFirestore fireStore = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public UserNotesAdapter(List<NoteModel> itemList) {
        this.itemList = itemList;
        this.sortByDate = false;
        this.ascendingOrder = true;
        this.filteredNotesList = new ArrayList<>(itemList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rw_notes_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        NoteModel currentItem = filteredNotesList.get(position);
        sharedPreferences = context.getSharedPreferences("MyNotesPrefs", Context.MODE_PRIVATE);

        Picasso.get().load(R.drawable.brightness).into(holder.itemImage);
        holder.itemName.setText(currentItem.getItemName());
        holder.textViewDate.setText(DateFormat.getDateInstance().format(currentItem.getDate()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SelectedNoteActivity.class);
                intent.putExtra("noteTitle", currentItem.getItemName());
                intent.putExtra("noteIndex", position);
                context.startActivity(intent);
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem(position);

                if (user != null) {
                    fireStore.runTransaction(new Transaction.Function<Void>() {
                        @Nullable
                        @Override
                        public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            DocumentReference userRef = fireStore
                                    .collection("users")
                                    .document(user.getUid());

                            DocumentSnapshot snapshot = transaction.get(userRef);
                            List<Map<String, Object>> notes = (List<Map<String, Object>>) snapshot.get("notes");

                            if (notes != null) {
                                int indexToRemove = -1;
                                for (int i = 0; i < notes.size(); i++) {
                                    Map<String, Object> note = notes.get(i);
                                    if (note.get("title").equals(currentItem.getItemName())) {
                                        indexToRemove = i;
                                        break;
                                    }
                                }
                                if (indexToRemove != -1) {
                                    notes.remove(indexToRemove);
                                    transaction.update(userRef, "notes", notes);
                                }
                            }
                            return null;
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredNotesList.size();
    }

    public void addItem(NoteModel item) {
        itemList.add(item);
        notifyItemInserted(itemList.size() - 1);
        sortItems();
        getFilter().filter("");
    }

    public void removeItem(int position) {
        if (position >= 0 && position < itemList.size()) {
            itemList.remove(position);
            filteredNotesList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, itemList.size());
        }
    }

    public void updateData(List<NoteModel> newList) {
        itemList.clear();
        itemList.addAll(newList);
        notifyDataSetChanged();
    }

    public void setSortByDate(boolean sortByDate) {
        this.sortByDate = sortByDate;
        sortItems();
    }

    public void setAscendingOrder(boolean ascendingOrder) {
        this.ascendingOrder = ascendingOrder;
        sortItems();
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String filterText = constraint.toString().toLowerCase().trim();
                List<NoteModel> filteredList = new ArrayList<>();

                if (filterText.isEmpty()) {
                    filteredList.addAll(itemList);
                } else {
                    for (NoteModel note : itemList) {
                        if (note.getItemName().toLowerCase().contains(filterText)) {
                            filteredList.add(note);
                        }
                    }
                }

                FilterResults filterResult = new FilterResults();
                filterResult.values = filteredList;
                return filterResult;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.values != null) {
                    filteredNotesList.clear();
                    filteredNotesList.addAll((List<NoteModel>) results.values);
                    notifyDataSetChanged();
                }
            }
        };
    }

    private void sortItems() {
        Comparator<NoteModel> comparator;
        if (sortByDate) {
            comparator = new Comparator<NoteModel>() {
                @Override
                public int compare(NoteModel item1, NoteModel item2) {
                    if (ascendingOrder) {
                        return item1.getDate().compareTo(item2.getDate());
                    } else {
                        return item2.getDate().compareTo(item1.getDate());
                    }
                }
            };
        } else {
            comparator = new Comparator<NoteModel>() {
                @Override
                public int compare(NoteModel item1, NoteModel item2) {
                    if (ascendingOrder) {
                        return item1.getItemName().compareToIgnoreCase(item2.getItemName());
                    } else {
                        return item2.getItemName().compareToIgnoreCase(item1.getItemName());
                    }
                }
            };
        }

        Collections.sort(filteredNotesList, comparator);
        notifyDataSetChanged();
    }

    public List<NoteModel> getItemList() {
        return itemList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName;
        TextView textViewDate;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
