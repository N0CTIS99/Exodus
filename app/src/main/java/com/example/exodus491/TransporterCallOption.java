package com.example.exodus491;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TransporterCallOption extends AppCompatActivity {

    private String customerPhoneNumber;

    private Button goBack;

    private static final int REQUEST_CALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_call_option);

        goBack = findViewById(R.id.goBack);

        Intent intent = getIntent();
        customerPhoneNumber = intent.getStringExtra("Customer phone number");
        makePhoneCall();

        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TransporterCallOption.this, TransporterMapActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void makePhoneCall(){
        if (ContextCompat.checkSelfPermission(TransporterCallOption.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(TransporterCallOption.this,
                    new String[] {Manifest.permission.CALL_PHONE}, REQUEST_CALL);
        }else {
            String dial = "tel:" + customerPhoneNumber;
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CALL){
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                makePhoneCall();
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(TransporterCallOption.this, TransporterMapActivity.class);
        startActivity(intent);
        finish();
        return;
    }
}
