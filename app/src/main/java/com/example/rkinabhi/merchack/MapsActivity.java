package com.example.rkinabhi.merchack;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "hello world";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location originLocation;
    private Location destinationLocation;
    private Location currentLocation;
    Marker originMarker;
    Marker destinationMarker;
    Marker currentMarker;
    FirebaseDatabase database;
    GeoFire userGeofire;
    GeoFire requestsGeoFire;
    DatabaseReference userReference;
    DatabaseReference requestsReference;
    String name;
    double charge;
    double mileage;
    ArrayList<LatLng> pathPoints;
    boolean outOfCharge = false;
    Button requestCharge;

    Dialog requestDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        database = FirebaseDatabase.getInstance();
        name = this.getIntent().getStringExtra("Name");
        charge = Double.parseDouble(this.getIntent().getStringExtra("Charge"));
        mileage = Double.parseDouble(this.getIntent().getStringExtra("Mileage"));
        userReference = database.getReference("CARS/"+name);
        requestsReference = database.getReference("REQUESTS/"+name);
        userGeofire = new GeoFire(userReference);
        requestsGeoFire = new GeoFire(requestsReference);
        requestDialog = new Dialog(this);


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        requestCharge = findViewById(R.id.request_button);

        //changed with popup code - on hit of request button
        requestCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestDialog.setContentView(R.layout.request_dialog);
                Button sendRequest = requestDialog.findViewById(R.id.btnReq);
                final EditText requestChargeAmount = requestDialog.findViewById(R.id.requestChargeAmount);

                // on hit of send request button
                sendRequest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestsReference.child("requiredCharge").setValue(requestChargeAmount.getText().toString());
                        requestsGeoFire.setLocation("currentLocation", new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
                        requestDialog.dismiss();
                    }
                });
                requestDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                requestDialog.show();
                outOfCharge = true;
            }
        });
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        @SuppressLint("MissingPermission") Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                originLocation = task.getResult();
                userGeofire.setLocation("originLocation",new GeoLocation(originLocation.getLatitude(), originLocation.getLongitude()));
                originMarker = googleMap.addMarker(new MarkerOptions().position(new LatLng(originLocation.getLatitude(), originLocation.getLongitude())).title("Origin Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), 15));
                getDestinationLocation();
            }
        });
    }

    void getDestinationLocation(){
        Toast.makeText(MapsActivity.this, "CHOOSE DESTINATION LOCATION", Toast.LENGTH_LONG).show();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                destinationLocation = new Location(LocationManager.GPS_PROVIDER);
                destinationLocation.setLatitude(latLng.latitude);
                destinationLocation.setLongitude(latLng.longitude);
                userGeofire.setLocation("destinationLocation",
                        new GeoLocation(destinationLocation.getLatitude(), destinationLocation.getLongitude()));
                destinationMarker = mMap.addMarker(new MarkerOptions().position(
                        new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude()))
                        .title("Destination Location"));
                mMap.setOnMapClickListener(null);
                getDirections();
            }
        });
    }

    private void startTravel() {
        currentLocation = new Location(LocationManager.GPS_PROVIDER);
        currentLocation.setLatitude(pathPoints.get(0).latitude);
        currentLocation.setLongitude(pathPoints.get(0).longitude);
        userGeofire.setLocation("currentLocation",
                new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
        currentMarker = mMap.addMarker(new MarkerOptions()
            .position(pathPoints.get(0))
            .title("Current Location"));

        currentMarker.setSnippet(Double.toString(charge));
        database.getReference("CARS/"+name+"/Charge").setValue(charge);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.get(0), 15));
        animateMarker();
    }

    private void animateMarker() {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 30000;
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            int i=0;

            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                if(i<pathPoints.size()){
                    currentLocation.setLatitude(pathPoints.get(i).latitude);
                    currentLocation.setLongitude(pathPoints.get(i).longitude);
                    userGeofire.setLocation("currentLocation",
                            new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
                    charge--;
                    currentMarker.setPosition(pathPoints.get(i));
                    currentMarker.setSnippet(Double.toString(charge));
                    database.getReference("CARS/"+name+"/Charge").setValue(charge);
                    database.getReference("CARS/"+name+"/Charge").setValue(charge);

                    i++;
                    Log.d(TAG, "the thing ran for the "+i+"th time");
                    if(!outOfCharge) {
                        handler.postDelayed(this, 160);
                    }
                }
            }
        });
    }

    void getDirections(){
        String url =  getRequestUrl(
                new LatLng(originLocation.getLatitude(), originLocation.getLongitude()),
                new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude()));

        TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
        taskRequestDirections.execute(url);
    }

    private String getRequestUrl(LatLng origin, LatLng destination) {
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDest = "destination=" + destination.latitude+","+destination.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String param = strOrigin + "&" + strDest + "&" + sensor + "&" + mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param;
        return url;
    }

    private String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try{
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader((inputStreamReader));
            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while((line =bufferedReader.readLine())!=null){
                stringBuffer.append(line);

            }
            responseString = stringBuffer.toString();
            bufferedReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>>{

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;

            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                Log.d(TAG, "doInBackground: "+jsonObject);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            PolylineOptions polylineOptions = null;
            pathPoints = new ArrayList<LatLng>();

            for(List<HashMap<String, String>> path : lists){
                ArrayList points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for(HashMap<String, String> point : path){
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));
                    points.add(new LatLng(lat, lon));
                    pathPoints.add(new LatLng(lat, lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }

            if(polylineOptions!=null){
                mMap.addPolyline(polylineOptions);
                startTravel();
            } else {
                Toast.makeText(MapsActivity.this, "Direction not found" , Toast.LENGTH_SHORT).show();
            }

        }

    }


}
