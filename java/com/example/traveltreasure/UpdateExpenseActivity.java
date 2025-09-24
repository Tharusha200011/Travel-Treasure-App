package com.example.traveltreasure;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class UpdateExpenseActivity extends AppCompatActivity {

    private EditText etCategory, etAmount;
    private Spinner spinnerPayer;
    private Button btnUpdateExpense, btnDeleteExpense;
    private DatabaseReference expensesRef;
    private String expenseId;
    private String itemName;  // Name of the main item
    private List<String> membersList = new ArrayList<>();  // List to hold members
    FirebaseUser user;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_expense);

        // Initialize views
        etCategory = findViewById(R.id.etCategory);
        etAmount = findViewById(R.id.etAmount);
        spinnerPayer = findViewById(R.id.spinnerPayer);
        btnUpdateExpense = findViewById(R.id.btnUpdateExpense);
        btnDeleteExpense = findViewById(R.id.btnDeleteExpense);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Get data from Intent
        String category = getIntent().getStringExtra("category");
        String amount = getIntent().getStringExtra("amount");
        String payer = getIntent().getStringExtra("payer");
        expenseId = getIntent().getStringExtra("expenseId");
        itemName = getIntent().getStringExtra("itemName");

        // Set the existing data in the input fields
        etCategory.setText(category);
        etAmount.setText(amount);
        // Populate the spinner with member names
        populatePayerSpinner(payer);

        // Firebase reference to the specific expense
        expensesRef  = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("expenses")
                .child(expenseId);

        // Update expense on button click
        btnUpdateExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateExpense();
            }
        });

        // Delete expense on button click
        btnDeleteExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteExpense();
            }
        });
    }

    private void populatePayerSpinner(String selectedPayer) {
        DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("members");

        membersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                membersList.clear();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String memberName = snapshot.child("name").getValue(String.class);
                    if (memberName != null) {
                        membersList.add(memberName);
                    }
                }

                // Set up spinner with the members
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, membersList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPayer.setAdapter(adapter);

                // Set the selected payer
                if (selectedPayer != null) {
                    int position = membersList.indexOf(selectedPayer);
                    if (position >= 0) {
                        spinnerPayer.setSelection(position);
                    }
                }
            }
        });
    }

    private void updateExpense() {
        String updatedCategory = etCategory.getText().toString().trim();
        String updatedAmount = etAmount.getText().toString().trim();
        String updatedPayer = spinnerPayer.getSelectedItem().toString();  // Get the selected payer

        if (TextUtils.isEmpty(updatedCategory) || TextUtils.isEmpty(updatedAmount) || TextUtils.isEmpty(updatedPayer)) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the expense in Firebase
        expensesRef.child("category").setValue(updatedCategory);
        expensesRef.child("amount").setValue(updatedAmount);
        expensesRef.child("payer").setValue(updatedPayer);

        Toast.makeText(UpdateExpenseActivity.this, "Expense updated successfully", Toast.LENGTH_SHORT).show();

        // Finish and return to the previous activity
        finish();
    }

    private void confirmDeleteExpense() {
        // Create an AlertDialog to confirm the deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteExpense();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteExpense() {
        // Remove the expense from Firebase
        expensesRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UpdateExpenseActivity.this, "Expense deleted successfully", Toast.LENGTH_SHORT).show();
                finish();  // Return to the previous activity
            } else {
                Toast.makeText(UpdateExpenseActivity.this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
            }
        });
    }
}