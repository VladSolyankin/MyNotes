package com.example.mynotes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserNotesAdapter extends RecyclerView.Adapter<UserNotesAdapter.ViewHolder> {

    private List<NoteModel> itemList;
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean sortByDate;
    private boolean ascendingOrder;

    public UserNotesAdapter(List<NoteModel> itemList) {
        this.itemList = itemList;
        this.sortByDate = false;
        this.ascendingOrder = true;
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
        NoteModel currentItem = itemList.get(position);
        sharedPreferences = context.getSharedPreferences("MyNotesPrefs", Context.MODE_PRIVATE);

        //holder.itemImage.setImageResource(currentItem.getImageResource());
        Picasso.get().load(currentItem.getImageResource()).into(holder.itemImage);
        holder.itemName.setText(currentItem.getItemName());
        holder.textViewDate.setText(DateFormat.getDateInstance().format(currentItem.getDate()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SelectedNoteActivity.class);
                intent.putExtra("noteTitle", currentItem.getItemName());
                context.startActivity(intent);
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem(position);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(currentItem.getItemName());
                editor.apply();
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void addItem(NoteModel item) {
        itemList.add(item);
        notifyItemInserted(itemList.size() - 1);
        sortItems();
    }
    public void removeItem(int position) {
        itemList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    public void setSortByDate(boolean sortByDate) {
        this.sortByDate = sortByDate;
        sortItems();
    }

    public void setAscendingOrder(boolean ascendingOrder) {
        this.ascendingOrder = ascendingOrder;
        sortItems();
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

        Collections.sort(itemList, comparator);
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
