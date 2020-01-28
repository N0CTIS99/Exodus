package com.example.exodus491;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Customer extends AppCompatActivity {

    private Button customerLogin, customerSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        customerLogin = findViewById(R.id.customerLogin);
        customerSignup = findViewById(R.id.customerSignup);

        customerLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Customer.this, Login.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        customerSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Customer.this, SignUp.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(Customer.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
