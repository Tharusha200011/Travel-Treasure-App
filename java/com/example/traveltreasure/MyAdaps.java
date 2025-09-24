package com.example.traveltreasure;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MyAdaps extends RecyclerView.Adapter<MyViewHolder> {

    private Context context;

    private List<DataClass> dataList;

    public MyAdaps(Context context,List<DataClass> dataList){
        this.context = context;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(dataList.get(position).getDataimage()).into(holder.recimage);
        holder.recname.setText(dataList.get(position).getDataname());
        holder.recdate.setText(dataList.get(position).getDatadate());

        holder.reccard.setOnClickListener(v -> {
            Intent intent = new Intent(context, Detail.class);
            intent.putExtra("Image", dataList.get(holder.getAdapterPosition()).getDataimage());
            intent.putExtra("Name", dataList.get(holder.getAdapterPosition()).getDataname());
            context.startActivity(intent);
        });

        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Group")
                    .setMessage("Are you sure you want to delete this Group?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteItem(holder.getAdapterPosition()))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void deleteItem(int position) {
        DataClass item = dataList.get(position);
        String itemName = item.getDataname(); // Get the unique identifier for the group

        dataList.remove(position);
        notifyItemRemoved(position);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("Groups");

        ref.child(itemName).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete Group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

}

class MyViewHolder extends RecyclerView.ViewHolder{

    ImageView recimage;
    TextView recname,recdate;
    CardView reccard;
    ImageButton deleteButton;

    public MyViewHolder(@NonNull View itemView){
        super(itemView);

        recimage = itemView.findViewById(R.id.recimage);
        recname = itemView.findViewById(R.id.recname);
        reccard = itemView.findViewById(R.id.recCard);
        recdate = itemView.findViewById(R.id.recdate);
        deleteButton = itemView.findViewById(R.id.deletefolder);

    }
}