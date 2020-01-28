package com.example.exodus491;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class TransporterMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    private Boolean isLoggingOut = false;

    private Button mLogout;
    private ImageButton btCall;

    //Related to customer info viewing
    private LinearLayout mCustomerInfo;
    private TextView mCustomerName, mCustomerPhone;

    private FusedLocationProviderClient mFusedLocationClient;

    private String customerID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_map);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mCustomerInfo = findViewById(R.id.customerInfo);
        mCustomerName = findViewById(R.id.customerName);
        mCustomerPhone = findViewById(R.id.customerPhone);

        mLogout = findViewById(R.id.logout);
        btCall = findViewById(R.id.bt_call);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isLoggingOut = true;
                disconnectTransporter();

                FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(TransporterMapActivity.this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(TransporterMapActivity.this, "You have successfully logged out", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        });

        btCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = mCustomerPhone.getText().toString();
                Intent intent =  new Intent(TransporterMapActivity.this, TransporterCallOption.class);
                intent.putExtra("Customer phone number", phoneNumber);
                startActivity(intent);
            }
        });

        //METHOD TO GET THE CUSTOMER WHO HAS REQUESTED SERVICE
        getAssignedCustomer();
    }

    private void getAssignedCustomer(){

        String transporterID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Transporters").child(transporterID).child("customerTransportID");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    //CUSTOMER HAS REQUESTED SERVICE
                    customerID = dataSnapshot.getValue().toString();
                    //GET THE LOCATION OF THE CUSTOMER
                    getAssignedCustomerLocation();
                    //GET CUSTOMER INFO
                    getAssignedCustomerInfo();
                }
                //TRANSPORT REQUEST HAS BEEN CANCELED
                //REMOVE MARKER AND EVENT LISTENERS
                else {
                    customerID = "";
                    if (customerLocationMarker != null){
                        customerLocationMarker.remove();
                    }

                    if (assignedCustomerLocationRefListener != null){
                        assignedCustomerLocationRef.removeEventListener(assignedCustomerLocationRefListener);
                    }
                    mCustomerInfo.setVisibility(View.GONE);
                    mCustomerName.setText("");
                    mCustomerPhone.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    //GETTING CUSTOMER INFO
    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("fullName")!=null){
                        mCustomerName.setText("Name: "+ map.get("fullName").toString());
                    }
                    if(map.get("phoneNumber")!=null){
                        mCustomerPhone.setText("Phone number: "+map.get("phoneNumber").toString());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private Marker customerLocationMarker;
    private DatabaseReference assignedCustomerLocationRef;
    private ValueEventListener assignedCustomerLocationRefListener;

    private void getAssignedCustomerLocation(){
        assignedCustomerLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerID).child("l");
        assignedCustomerLocationRefListener = assignedCustomerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerID.equals("")){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;
                    //GET LATITUDE
                    if (map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    //GET LONGITUDE
                    if (map.get(1) != null){
                        locationLong = Double.parseDouble(map.get(1).toString());
                    }

                    //PUT A TRANSPORTER'S LOCATION ON MAP
                    LatLng transporterLatLng = new LatLng(locationLat, locationLong);
                    customerLocationMarker = mMap.addMarker(new MarkerOptions().position(transporterLatLng).title("Your Customer Location"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //PERIODICALLY CHECK LOCATION
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //CHECK IF ANDROID VERSION IS HIGH ENOUGH
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
            //If permission is not granted
            else{
                checkLocationPermission();
            }
        }

    }

    //onLocationChanged
    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){

                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("transporterAvailable");
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("transporterWorking");

                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    switch (customerID){
                        //if no customers are requesting service, meaning customerID=""

                        case "":{
                            //TRANSPORTER IS AVAILABLE SO PUT LOCATION IN AVAILABLE AND REMOVE FROM WORKING

                            geoFireWorking.removeLocation(userId, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null){
                                        Toast.makeText(TransporterMapActivity.this, "Your location was not removed from Working", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            geoFireAvailable.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null){
                                        Toast.makeText(TransporterMapActivity.this, "There was an error saving your current location", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        //Toast.makeText(TransporterMapActivity.this, "Your current location is being saved", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            break;
                        }

                        //when customerID != ""(there is a customer)
                        default:{

                            //TRANSPORTER IS WORKING SO PUT LOCATION IN WORKING AND REMOVE FROM AVAILABLE

                            geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null){
                                        Toast.makeText(TransporterMapActivity.this, "Your location was not removed from Working", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            geoFireWorking.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if (error != null){
                                        Toast.makeText(TransporterMapActivity.this, "There was an error saving your current location", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        //Toast.makeText(TransporterMapActivity.this, "Your current location is being saved", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            break;
                        }
                    }


                }
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Give location permission?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(TransporterMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                startActivity(new Intent(TransporterMapActivity.this, TransporterLogin.class));
                            }
                        }).create().show();
            }
            else{
                ActivityCompat.requestPermissions(TransporterMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:{
                if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Please provide permission", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }
    }

    private void disconnectTransporter(){
        if (mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("transporterAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null){
                    Toast.makeText(TransporterMapActivity.this, "Your location was not removed from database", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(TransporterMapActivity.this, "Your location is no longer being recorded", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*@Override
    protected void onStop() {
        super.onStop();

        //remove the location from the database (saved in onLocationResult) whenever user closes or minimizes the app
        if (!isLoggingOut){
            disconnectTransporter();
        }
    }*/
}
