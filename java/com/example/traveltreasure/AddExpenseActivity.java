package com.example.traveltreasure;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText categoryEditText, amountEditText;
    private Spinner payerSpinner;
    Button saveExpenseButton;
    private DatabaseReference expensesRef;
    private String itemName;
    FirebaseUser user;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Initialize views
        categoryEditText = findViewById(R.id.categoryEditText);
        amountEditText = findViewById(R.id.amountEditText);
        payerSpinner = findViewById(R.id.payerSpinner);
        saveExpenseButton = findViewById(R.id.saveExpenseButton);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Get data from the Intent
        itemName = getIntent().getStringExtra("ItemName");

        ArrayList<String> memberNames = getIntent().getStringArrayListExtra("MemberNames");

        // Populate the spinner with member names
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, memberNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        payerSpinner.setAdapter(adapter);

        // Reference to the specific item's expenses in Firebase
        expensesRef  = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("expenses");

        // Save expense button click listener
        saveExpenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });
    }

    private void saveExpense() {
        String category = categoryEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String payer = payerSpinner.getSelectedItem().toString();

        if (category.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String expenseId = expensesRef.push().getKey(); // Generate a unique ID for the expense
        Expense expense = new Expense(category, amount, payer);

        if (expenseId != null) {
            expensesRef.child(expenseId).setValue(expense)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddExpenseActivity.this, "Expense saved", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity and return to the previous one
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddExpenseActivity.this, "Failed to save expense: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
