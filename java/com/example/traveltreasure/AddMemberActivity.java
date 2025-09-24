package com.example.traveltreasure;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddMemberActivity extends AppCompatActivity {

    private EditText memberName, memberEmail;
    private Button saveMemberButton;
    private DatabaseReference databaseReference;
    private String itemName;
    FirebaseUser user;
    FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        memberName = findViewById(R.id.memberName);
        memberEmail = findViewById(R.id.memberEmail);
        saveMemberButton = findViewById(R.id.saveMemberButton);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // Get the item name passed from Detail Activity
        itemName = getIntent().getStringExtra("itemName");

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                .child("Groups").child(itemName)
                .child("members");

        saveMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMember();
            }
        });
    }

    private void addMember() {
        String name = memberName.getText().toString().trim();
        String email = memberEmail.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(AddMemberActivity.this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique ID for each member
        String memberId = databaseReference.push().getKey();

        Member member = new Member(name, email);

        if (memberId != null) {
            databaseReference.child(memberId).setValue(member).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(AddMemberActivity.this, "Member added", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                } else {
                    Toast.makeText(AddMemberActivity.this, "Failed to add member", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static class Member {
        public String name;
        public String email;

        public Member() {
            // Default constructor required for calls to DataSnapshot.getValue(Member.class)
        }

        public Member(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
