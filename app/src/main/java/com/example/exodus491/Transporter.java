package com.example.exodus491;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Transporter extends AppCompatActivity {

    private Button transporterLogin, transporterSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter);

        transporterLogin = findViewById(R.id.transporterLogin);
        transporterSignup = findViewById(R.id.transporterSignup);

        transporterLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Transporter.this, TransporterLogin.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        transporterSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Transporter.this, TransporterSignUp.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Transporter.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
