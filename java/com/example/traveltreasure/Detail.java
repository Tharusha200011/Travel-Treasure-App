package com.example.traveltreasure;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Detail extends AppCompatActivity {

    TextView detailname;
    ImageView detailimage;
    private LinearLayout membersContainer;
    private String itemName;
    private List<String> memberNames;
    LinearLayout addExpenseLayout;
    ImageButton addMemberButton;
    private LinearLayout expensesContainer;

    private DatabaseReference expensesRef;
    private DatabaseReference membersRef;

    private TextView tvTotalBudget;
    private double totalBudget = 0.0;
    private LinearLayout btnShowSummary;

    FirebaseUser user;
    FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        detailname = findViewById(R.id.detailname);
        detailimage = findViewById(R.id.detailimage);
        addMemberButton = findViewById(R.id.addMemberButton);
        membersContainer = findViewById(R.id.membersContainer);
        addExpenseLayout = findViewById(R.id.addExpenseLayout);
        expensesContainer = findViewById(R.id.expensesContainer);
        tvTotalBudget = findViewById(R.id.tvTotalBudget);
        btnShowSummary = findViewById(R.id.btnShowSummary);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        itemName = getIntent().getStringExtra("Name");

        // Reference to the specific item's members in Firebase
        membersRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("members");

        memberNames = new ArrayList<>();
        fetchAndDisplayMembers();

        // Reference to expenses in Firebase
        expensesRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("expenses");

        // Fetch and display expenses
        fetchAndDisplayExpenses();

        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Detail.this, AddMemberActivity.class);
                intent.putExtra("itemName", getIntent().getStringExtra("Name")); // Pass the item name or ID
                startActivity(intent);
            }
        });

        // Set up the add expense button click listener
        addExpenseLayout.setOnClickListener(view -> {
            // Launch AddExpenseActivity to add a new expense
            Intent intent = new Intent(Detail.this, AddExpenseActivity.class);
            intent.putExtra("ItemName", itemName);
            intent.putStringArrayListExtra("MemberNames", new ArrayList<>(memberNames));
            startActivity(intent);
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            detailname.setText(bundle.getString("Name"));
            Glide.with(this).load(bundle.getString("Image")).into(detailimage);
        }

        btnShowSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Detail.this, SummaryActivity.class);
                intent.putExtra("itemName", itemName);  // Replace with actual item name
                startActivity(intent);
            }
        });
    }

    private void addMemberTextView(String memberId, String memberName, String memberEmail) {
        LinearLayout memberLayout = new LinearLayout(this);
        memberLayout.setOrientation(LinearLayout.HORIZONTAL);
        memberLayout.setPadding(8, 8, 8, 8);
        memberLayout.setBackground(getResources().getDrawable(R.drawable.border));

        TextView memberTextView = new TextView(this);
        memberTextView.setText(String.format("%s", memberName));
        memberTextView.setTextSize(16f);
        memberTextView.setGravity(Gravity.CENTER);
        memberTextView.setPadding(8, 8, 8, 8);

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(R.drawable.baseline_person_remove_24);
        deleteButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        deleteButton.setContentDescription("Delete Member");
        deleteButton.setPadding(8, 8, 8, 8);

        // Set up delete button click listener
        deleteButton.setOnClickListener(view -> deleteMember(memberId));

        // Add TextView and ImageButton to the LinearLayout
        memberLayout.addView(memberTextView);
        memberLayout.addView(deleteButton);

        // Add LinearLayout to the container
        membersContainer.addView(memberLayout);
    }

    private void fetchAndDisplayMembers() {
        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersContainer.removeAllViews(); // Clear the container before adding new views
                memberNames.clear(); // Clear the list of member names

                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    Map<String, String> memberData = (Map<String, String>) memberSnapshot.getValue();
                    if (memberData != null) {
                        String memberId = memberSnapshot.getKey(); // Get the unique ID of the member
                        String memberName = memberData.get("name");
                        String memberEmail = memberData.get("email");
                        memberNames.add(memberName); // Add the member's name to the list
                        addMemberTextView(memberId, memberName, memberEmail);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
                Toast.makeText(Detail.this, "Failed to load members.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteMember(String memberId) {
        // Create an alert dialog
        new AlertDialog.Builder(Detail.this)
                .setTitle("Delete Member")
                .setMessage("Are you sure you want to delete this member?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // User confirmed the deletion, so perform the deletion
                    membersRef.child(memberId).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(Detail.this, "Member deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(Detail.this, "Failed to delete member: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // User canceled the deletion, do nothing
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void fetchAndDisplayExpenses() {
        expensesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                expensesContainer.removeAllViews(); // Clear previous views
                totalBudget = 0.0; // Reset total budget

                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    String key = expenseSnapshot.getKey();  // Unique key of the expense
                    Map<String, String> expenseData = (Map<String, String>) expenseSnapshot.getValue();
                    if (expenseData != null) {
                        String category = expenseData.get("category");
                        String amountStr = expenseData.get("amount");
                        String payer = expenseData.get("payer");

                        // Convert amount to double and add to totalBudget
                        double amount = Double.parseDouble(amountStr);
                        totalBudget += amount;

                        // Add the expense item to the UI
                        addExpenseItem(key, category, amountStr, payer);  // Pass the key as well
                    }
                }

                // Update the total budget TextView
                tvTotalBudget.setText("Total Budget: Rs" + String.format("%.2f", totalBudget));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Detail.this, "Failed to load expenses.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addExpenseItem(String expenseId, String category, String amount, String payer) {
        // Inflate the expense item layout
        View expenseItemView = LayoutInflater.from(this).inflate(R.layout.expense_item, expensesContainer, false);

        // Set the category, amount, and payer
        TextView tvExpenseCategory = expenseItemView.findViewById(R.id.tvExpenseCategory);
        TextView tvExpenseAmount = expenseItemView.findViewById(R.id.tvExpenseAmount);
        TextView tvExpensePayer = expenseItemView.findViewById(R.id.tvExpensePayer);

        tvExpenseCategory.setText(category);
        tvExpenseAmount.setText("Rs" + amount);
        tvExpensePayer.setText(payer);

        // Add the expense item to the container
        expensesContainer.addView(expenseItemView);

        // Set a click listener to open UpdateExpenseActivity
        expenseItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Detail.this, UpdateExpenseActivity.class);
                intent.putExtra("category", category);
                intent.putExtra("amount", amount);
                intent.putExtra("payer", payer);
                intent.putExtra("expenseId", expenseId);
                intent.putExtra("itemName", itemName); // Pass the parent item name
                startActivity(intent);
            }
        });
    }

}