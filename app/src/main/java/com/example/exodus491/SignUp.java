package com.example.exodus491;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    //THIS IS FOR THE CUSTOMER SIGN-UP ACTIVITY

    private Button button;
    private EditText etEmail, etFname, etPass, etPhone;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user!=null){
                    Toast.makeText(SignUp.this, "Sign up successful. You will be logged in automatically", Toast.LENGTH_SHORT).show();;
                }
            }
        };

        button = findViewById(R.id.signUpBT); //sign up button
        etEmail = findViewById(R.id.etEmail); //email
        etFname = findViewById(R.id.etFname); //Full Name
        etPass = findViewById(R.id.etPass); //Password
        etPhone = findViewById(R.id.etPhone); //Phone Number

        //User registration
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkIfFieldIsFilled()){
                    final String email = etEmail.getText().toString();
                    final String password = etPass.getText().toString();
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                Toast.makeText(SignUp.this, "Sign up error", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                String user_id = mAuth.getCurrentUser().getUid(); //get a random UID
                                DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);


                                final String fName = etFname.getText().toString();
                                final String phone = etPhone.getText().toString();

                                Map newPost = new HashMap();
                                newPost.put("fullName", fName);
                                newPost.put("phoneNumber", phone);

                                current_user_db.setValue(newPost);
                                Toast.makeText(SignUp.this, "Sign up successful. You will be logged in automatically", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {

                    Toast.makeText(SignUp.this, "Please fill up all of the above fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }

    public boolean checkIfFieldIsFilled(){
        if (TextUtils.isEmpty(etEmail.getText().toString()) ||
                TextUtils.isEmpty(etFname.getText().toString()) ||
                TextUtils.isEmpty(etPass.getText().toString()) ||
                TextUtils.isEmpty(etPhone.getText().toString()) ){
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(SignUp.this, Customer.class));
        finish();
        return;
    }
}
