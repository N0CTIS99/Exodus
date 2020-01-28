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

public class TransporterLogin extends AppCompatActivity {

    private Button button;
    private EditText etLoginEmail, etLoginPass;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_login);

        button = findViewById(R.id.transporterLoginBT);
        etLoginEmail = findViewById(R.id.transportLoginEmail);
        etLoginPass = findViewById(R.id.transportLoginPass);

        mAuth = FirebaseAuth.getInstance();

        //authentication
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user!=null){
                    Intent intent = new Intent(TransporterLogin.this, TransporterMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        //login button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkIfFieldIsFilled()){
                    final String email = etLoginEmail.getText().toString();
                    final String password = etLoginPass.getText().toString();
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(TransporterLogin.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                Toast.makeText(TransporterLogin.this, "Login error: Incorrect Email or Password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(TransporterLogin.this, "Please enter your email and password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean checkIfFieldIsFilled(){
        if (TextUtils.isEmpty(etLoginEmail.getText().toString()) ||
                TextUtils.isEmpty(etLoginPass.getText().toString()) ){
            return false;
        }
        else {
            return true;
        }
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(TransporterLogin.this, Transporter.class);
        startActivity(intent);
        finish();
    }
}
