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
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private LatLng customerLocation;
    private Marker customerMarker;

    private Boolean isLoggingOut = false;
    private Boolean requestBol = false;

    private Button mLogout, mCallTransporter;

    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        mLogout = findViewById(R.id.logout);
        mCallTransporter = findViewById(R.id.callTransporter);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isLoggingOut = true;
                disconnectCustomer();

                FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(CustomerMapActivity.this, "You have successfully logged out", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        });

        mCallTransporter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //REMOVING THE LISTENERS AND RELEVANT DATA FROM DATABASE WHEN USER CANCELS REQUEST
                if (requestBol){
                    requestBol = false;

                    geoQuery.removeAllListeners();
                    if (transporterLocationRefListener != null){
                        transporterLocationRef.removeEventListener(transporterLocationRefListener);
                    }

                    if (transporterFoundID != null){
                        DatabaseReference transporterRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Transporters").child(transporterFoundID).child("customerTransportID");
                        transporterRef.removeValue();
                        transporterFoundID = null;
                    }
                    transporterFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(customerRef);
                    geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null){
                                Toast.makeText(CustomerMapActivity.this, "There was an error saving your current location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    //REMOVING MARKERS
                    if (customerMarker != null){
                        customerMarker.remove();
                    }

                    if (mTransporterMarker != null){
                        mTransporterMarker.remove();
                    }

                    mCallTransporter.setText("Call a transporter");
                }
                else {
                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(customerRef);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null){
                                Toast.makeText(CustomerMapActivity.this, "There was an error saving your current location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    customerLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    customerMarker = mMap.addMarker(new MarkerOptions().position(customerLocation).title("You are here"));

                    mCallTransporter.setText("Getting you a transporter...");

                    //METHOD TO GET THE NEAREST TRANSPORTER
                    getClosestTransporter();
                }

            }
        });
    }

    //radius INDICATES A CIRCULAR AREA AROUND THE CUSTOMER
    private int radius = 1;
    //CHECK IF TRANSPORTER IS FOUND
    private Boolean transporterFound = false;
    //ID OF TRANSPORTER
    private String transporterFoundID;
    private GeoQuery geoQuery;

    private void getClosestTransporter(){
        DatabaseReference transporterLocation = FirebaseDatabase.getInstance().getReference("transporterAvailable");

        GeoFire geoFire = new GeoFire(transporterLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(customerLocation.latitude, customerLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!transporterFound && requestBol){
                    transporterFound = true; //transporter has been found
                    transporterFoundID = key;

                    DatabaseReference transporterRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Transporters").child(transporterFoundID);
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerTransportID", customerID);   //will be put under Transporters->customerTransportID->ID of the customer requesting service
                    transporterRef.updateChildren(map);


                    mCallTransporter.setText("Looking for Transporter's location...");
                    getTransporterLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!transporterFound){
                    radius++;
                    if (radius <10000) {
                        getClosestTransporter(); //recursion
                    }
                    else {
                        Toast.makeText(CustomerMapActivity.this, "Unfortunately there are no transporters available at the moment", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private Marker mTransporterMarker;
    private DatabaseReference transporterLocationRef;
    private ValueEventListener transporterLocationRefListener;

    private void getTransporterLocation(){
        transporterLocationRef = FirebaseDatabase.getInstance().getReference().child("transporterWorking").child(transporterFoundID).child("l");
        transporterLocationRefListener = transporterLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol){
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
                    if (mTransporterMarker != null){
                        mTransporterMarker.remove();
                    }


                    mCallTransporter.setText("Transporter Found (Press again to cancel)");
                    mTransporterMarker = mMap.addMarker(new MarkerOptions().position(transporterLatLng).title("Your Transporter").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_transporter1)));
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

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){

                   /* if(!customerId.equals("") && mLastLocation!=null && location != null){
                        rideDistance += mLastLocation.distanceTo(location)/1000;
                    }

                    */
                    mLastLocation = location;


                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


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
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                startActivity(new Intent(CustomerMapActivity.this, TransporterLogin.class));
                            }
                        }).create().show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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

    private void disconnectCustomer(){
        if (mFusedLocationClient != null){
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null){
                    Toast.makeText(CustomerMapActivity.this, "Your location was not removed from database", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(CustomerMapActivity.this, "Your location is no longer being recorded", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*@Override
    protected void onStop() {
        super.onStop();

        //remove the location from the database (saved in onLocationResult) whenever user closes or minimizes the app
        if (!isLoggingOut){
            disconnectCustomer();
        }
    }*/
}
