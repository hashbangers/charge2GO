package com.example.rkinabhi.merchack;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private Location primaryDestinationLocation;
    private Location currentLocation;
    private Location secondaryDestinationLocation;

    Marker originMarker;
    Marker destinationMarker;
    Marker currentMarker;
    Marker donorMarker;

    FirebaseDatabase database;
    GeoFire userGeofire;
    GeoFire requestsGeoFire;
    DatabaseReference userReference;
    DatabaseReference requestsReference;
    DatabaseReference requestLocationReference;

    DatabaseReference requestNotificationsReference;
    DatabaseReference responseNotificationsReference;

    String name;
    double charge;
    double mileage;
    double batteryCapacity;

    double totalPathDistance;
    double distanceToDonor;
    double distanceToDestination;

    double requestRadius;

    ArrayList<LatLng> originToPrimaryDestinationPathPoints;
    ArrayList<LatLng> currentLocationToSecondaryDestinationPathPoints;

    ArrayList<Double> pointDistances;
    ArrayList<Double> currentToDonorpointDistances;
    boolean outOfCharge = false;
    Button requestCharge;

    Circle outerCircle;
    Circle innerCircle;

    CarState carState;

    Dialog sendRequestDialog;
    Dialog receiveRequestDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        database = FirebaseDatabase.getInstance();
        name = this.getIntent().getStringExtra("Name");

        charge = Double.parseDouble(this.getIntent().getStringExtra("Charge"));
        mileage = Double.parseDouble(this.getIntent().getStringExtra("Mileage"));
        batteryCapacity = Double.parseDouble(this.getIntent().getStringExtra("Batterycapacity"));

        userReference = database.getReference("CARS/"+name);
        requestsReference = database.getReference("REQUESTS/"+name);
        requestNotificationsReference = database.getReference("REQUESTS");

        responseNotificationsReference = database.getReference("CARS/"+name+"/response");

        userGeofire = new GeoFire(userReference);
        requestsGeoFire = new GeoFire(requestsReference);
        sendRequestDialog = new Dialog(this);
        receiveRequestDialog = new Dialog(this);
        requestRadius = 3218;
        carState = CarState.AtOrigin;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        requestCharge = findViewById(R.id.request_button);
        requestCharge.setOnClickListener(new RequestChargeListener());
        requestCharge.setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
        requestNotificationsReference.addChildEventListener(new RequestNotificationsListener());

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
                originMarker = googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()))
                        .title("Origin Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.origin_icon)));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), 15));
                getDestinationLocation();
            }
        });

        database.getReference("CHARGINGSTATIONS").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                new GeoFire(dataSnapshot.getRef()).getLocation("Location", new LocationCallback() {
                    @Override
                    public void onLocationResult(String key, GeoLocation location) {
                        String Occupied = Boolean.toString(dataSnapshot.child("Occupied").getValue(boolean.class));
                        if(Occupied.equalsIgnoreCase("false")) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(location.latitude, location.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.b_cs)));
                        } else {
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(location.latitude, location.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.r_cs)));
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        database.getReference("TRUCKS").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                new GeoFire(dataSnapshot.getRef()).getLocation("Location", new LocationCallback() {
                    @Override
                    public void onLocationResult(String key, GeoLocation location) {
                        String Occupied = Boolean.toString(dataSnapshot.child("Occupied").getValue(boolean.class));
                        if(Occupied.equalsIgnoreCase("false")) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(location.latitude, location.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.b_t)));
                        } else {
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(location.latitude, location.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.r_t)));
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    void getDestinationLocation(){
        Toast.makeText(MapsActivity.this, "CHOOSE DESTINATION LOCATION", Toast.LENGTH_LONG).show();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                primaryDestinationLocation = new Location(LocationManager.GPS_PROVIDER);
                primaryDestinationLocation.setLatitude(latLng.latitude);
                primaryDestinationLocation.setLongitude(latLng.longitude);
                userGeofire.setLocation("primaryDestinationLocation",
                        new GeoLocation(primaryDestinationLocation.getLatitude(), primaryDestinationLocation.getLongitude()));

                destinationMarker = mMap.addMarker(new MarkerOptions().position(
                        new LatLng(primaryDestinationLocation.getLatitude(), primaryDestinationLocation.getLongitude()))
                        .title("Destination Location")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_icon)));
                mMap.setOnMapClickListener(null);
                carState = CarState.TravellingToPrimaryDestination;
                getDirections(originLocation, primaryDestinationLocation);

            }
        });
    }

    private void travelToPrimaryLocation() {
        carState = CarState.TravellingToPrimaryDestination;
        currentLocation = new Location(LocationManager.GPS_PROVIDER);
        currentLocation.setLatitude(originToPrimaryDestinationPathPoints.get(0).latitude);
        currentLocation.setLongitude(originToPrimaryDestinationPathPoints.get(0).longitude);
        userGeofire.setLocation("currentLocation",
                new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
        currentMarker = mMap.addMarker(new MarkerOptions()
            .position(originToPrimaryDestinationPathPoints.get(0))
            .title("Current Location")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.g_car)));

        currentMarker.setSnippet(Double.toString(charge));
        database.getReference("CARS/"+name+"/Charge").setValue(charge);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originToPrimaryDestinationPathPoints.get(0), 15));
        animateMarker();
    }

    private void travelToSecondaryLocation(){
        carState = CarState.TravellingToSecondaryDestination;
        animateMarkerTwo();

    }

    private void animateRequestCircle(){
        final Handler handler = new Handler();
        //drawing a circle
        outerCircle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())).radius(requestRadius));
        innerCircle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())).radius(0));

        handler.post(new Runnable() {

            int i=0;
            @Override
            public void run() {
                    innerCircle.setRadius(i);
                    i+=4;
                    if(i >= requestRadius)
                        i=0;
                    if(carState == CarState.WaitingForResponse)
                        handler.postDelayed(this, 1);
            }
        });
    }

    private void animateMarkerTwo() {

        final Handler handler = new Handler();

        handler.post(new Runnable() {
            int i=1;

            @Override
            public void run() {
                if(i< currentLocationToSecondaryDestinationPathPoints.size()){
                    currentLocation.setLatitude(currentLocationToSecondaryDestinationPathPoints.get(i).latitude);
                    currentLocation.setLongitude(currentLocationToSecondaryDestinationPathPoints.get(i).longitude);
                    userGeofire.setLocation("currentLocation",
                            new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));


                    distanceToDestination -= currentToDonorpointDistances.get(i-1);
                    charge -= currentToDonorpointDistances.get(i-1)/(mileage*1.6);
                    if(charge<batteryCapacity*0.2){
                        carState = CarState.ChargeDefecient;
                        requestCharge.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                    }
                    currentMarker.setPosition(currentLocationToSecondaryDestinationPathPoints.get(i));
                    currentMarker.setSnippet(Double.toString(charge));
                    database.getReference("CARS/"+name+"/Charge").setValue(charge);
                    database.getReference("CARS/"+name+"/DistanceToDestination").setValue(distanceToDestination);

                    i++;
                    Log.d(TAG, "car hasnt reached donor yet");
                    if(currentLocation.equals(secondaryDestinationLocation)) {
                        carState = CarState.Charging;
                        Log.d(TAG, "run: CHARGING");
                    }
                    if(carState != CarState.Charging) {
                        handler.postDelayed(this, 160);
                    }
                }
            }
        });
    }
    private void animateMarker() {

        final Handler handler = new Handler();

        handler.post(new Runnable() {
            int i=1;

            @Override
            public void run() {
                if(i< originToPrimaryDestinationPathPoints.size()){
                    currentLocation.setLatitude(originToPrimaryDestinationPathPoints.get(i).latitude);
                    currentLocation.setLongitude(originToPrimaryDestinationPathPoints.get(i).longitude);
                    userGeofire.setLocation("currentLocation",
                            new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));


                    distanceToDestination -= pointDistances.get(i-1);
                    charge -= pointDistances.get(i-1)/(mileage*1.6);
                    if(charge<batteryCapacity*0.2){
                        carState = CarState.ChargeDefecient;
                        requestCharge.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                    }
                    currentMarker.setPosition(originToPrimaryDestinationPathPoints.get(i));
                    currentMarker.setSnippet(Double.toString(charge));
                    database.getReference("CARS/"+name+"/Charge").setValue(charge);
                    database.getReference("CARS/"+name+"/DistanceToDestination").setValue(distanceToDestination);

                    i++;
                    Log.d(TAG, "the thing ran for the "+i+"th time");
                    if(carState != CarState.ChargeDefecient && carState != CarState.Responding) {
                        handler.postDelayed(this, 160);
                    }
                }
            }
        });
    }

    void getDirections(Location A, Location B){
        String url =  getRequestUrl(
                new LatLng(A.getLatitude(), A.getLongitude()),
                new LatLng(B.getLatitude(), B.getLongitude()));

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
            if(carState == CarState.TravellingToPrimaryDestination) {
                PolylineOptions polylineOptions = null;
                originToPrimaryDestinationPathPoints = new ArrayList<LatLng>();

                for (List<HashMap<String, String>> path : lists) {
                    ArrayList points = new ArrayList();
                    polylineOptions = new PolylineOptions();

                    for (HashMap<String, String> point : path) {
                        double lat = Double.parseDouble(point.get("lat"));
                        double lon = Double.parseDouble(point.get("lon"));
                        points.add(new LatLng(lat, lon));
                        originToPrimaryDestinationPathPoints.add(new LatLng(lat, lon));
                    }

                    polylineOptions.addAll(points);
                    polylineOptions.width(15);
                    polylineOptions.color(Color.BLUE);
                    polylineOptions.geodesic(true);
                }

                if (polylineOptions != null) {
                    mMap.addPolyline(polylineOptions);
                    findTotalPathDistance();
                    Toast.makeText(MapsActivity.this, "Total travel distance is " + totalPathDistance, Toast.LENGTH_SHORT).show();
                    travelToPrimaryLocation();
                } else {
                    Toast.makeText(MapsActivity.this, "Direction not found", Toast.LENGTH_SHORT).show();
                }
            } else if(carState == CarState.TravellingToSecondaryDestination){
                PolylineOptions polylineOptions = null;
                currentLocationToSecondaryDestinationPathPoints = new ArrayList<LatLng>();
                for (List<HashMap<String, String>> path : lists) {
                    ArrayList points = new ArrayList();
                    polylineOptions = new PolylineOptions();

                    for (HashMap<String, String> point : path) {
                        double lat = Double.parseDouble(point.get("lat"));
                        double lon = Double.parseDouble(point.get("lon"));
                        points.add(new LatLng(lat, lon));
                        currentLocationToSecondaryDestinationPathPoints.add(new LatLng(lat, lon));
                    }

                    polylineOptions.addAll(points);
                    polylineOptions.width(15);
                    polylineOptions.color(Color.RED);
                    polylineOptions.geodesic(true);
                }

                if (polylineOptions != null) {
                    mMap.addPolyline(polylineOptions);
                    findCurrentToDonorPathDistance();
                    Toast.makeText(MapsActivity.this, "Total travel distance is " + totalPathDistance, Toast.LENGTH_SHORT).show();
                    travelToSecondaryLocation();
                } else {
                    Toast.makeText(MapsActivity.this, "Direction not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class RequestChargeListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            sendRequestDialog.setContentView(R.layout.send_request_dialog);
            Button sendRequest = sendRequestDialog.findViewById(R.id.btnReq);
            sendRequest.setOnClickListener(new SendRequest());
            sendRequestDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            sendRequestDialog.show();
            carState = CarState.ChargeDefecient;
        }
    }

    private class SendRequest implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            EditText requestChargeAmount = sendRequestDialog.findViewById(R.id.requestChargeAmount);
            requestsReference.child("requiredCharge").setValue(requestChargeAmount.getText().toString());
            requestsGeoFire.setLocation("currentLocation", new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
            sendRequestDialog.dismiss();
            carState = CarState.WaitingForResponse;
            animateRequestCircle();
            responseNotificationsReference.addChildEventListener(new ResponseNotificationsListener());
        }
    }


    private class RequestNotificationsListener implements ChildEventListener {
        @Override
        public void onChildAdded(final DataSnapshot dataSnapshotSuper, String s) {
            final String requestingCarName = dataSnapshotSuper.getKey();
            if(!requestingCarName.equals(name)) {
                DatabaseReference reqChargeTemp = database.getReference("REQUESTS/"+requestingCarName+"/requiredCharge");
                Log.d(TAG, "onChildAdded: "+"sup");
                reqChargeTemp.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String requiredCharge = dataSnapshot.getValue(String.class);
                        Log.d(TAG, "onChildAdded rech from value: "+requiredCharge);
                        DatabaseReference curLocTemp = dataSnapshotSuper.child("currentLocation").getRef();
                        curLocTemp.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                new GeoFire(dataSnapshotSuper.getRef()).getLocation("currentLocation", new LocationCallback() {
                                    @Override
                                    public void onLocationResult(String key, GeoLocation location) {
                                        Location requestorLocation = new Location(LocationManager.GPS_PROVIDER);
                                        requestorLocation.setLatitude(location.latitude);
                                        requestorLocation.setLongitude(location.longitude);
                                        Log.d(TAG, "onLocationResult lat: " + Double.toString(location.latitude));
                                        Log.d(TAG, "onLocationResult lng: " + Double.toString(location.longitude));
                                        if ((requestorLocation.distanceTo(currentLocation) < requestRadius))  {
                                            if(charge-batteryCapacity*0.2-distanceToDestination/mileage> 0){
                                                receiveRequestDialog.setContentView(R.layout.recieve_request_dialog);
                                                TextView canGiveCharge = receiveRequestDialog.findViewById(R.id.textView2);
                                                canGiveCharge.setText(Double.toString(charge-batteryCapacity*0.2-distanceToDestination/mileage));
                                                receiveRequestDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                                                TextView requestedCharge = receiveRequestDialog.findViewById(R.id.textView);
                                                final EditText donatingCharge = receiveRequestDialog.findViewById(R.id.editText3);
                                                Button accept = receiveRequestDialog.findViewById(R.id.yes);
                                                Button ignore = receiveRequestDialog.findViewById(R.id.button2);
                                                if(!MapsActivity.this.isFinishing()) {
                                                    receiveRequestDialog.show();
                                                }
                                                requestedCharge.setText(requestingCarName + " is requesting for "+ requiredCharge + "Kwh");
                                                donatingCharge.addTextChangedListener(new TextWatcher() {
                                                    @Override
                                                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                                                    @Override
                                                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                                        TextView tv = receiveRequestDialog.findViewById(R.id.textView3);try {
                                                            tv.setText(Double.toString(Double.parseDouble(charSequence.toString()) * 5));
                                                        } catch (Exception e){}
                                                    }
                                                    @Override
                                                    public void afterTextChanged(Editable editable) {}
                                                });

                                                accept.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        carState = CarState.Responding;
                                                        requestNotificationsReference.removeValue();
                                                        DatabaseReference responseRefernce = database.getReference("CARS/"+requestingCarName+"/response/"+name);
                                                        responseRefernce.child("OTP").setValue("1243");
                                                        responseRefernce.child("donatingCharge").setValue(donatingCharge.getText().toString());
                                                        receiveRequestDialog.dismiss();
                                                        new GeoFire(responseRefernce).setLocation("currentLocation", new GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude()));
                                                    }
                                                });

                                                ignore.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        receiveRequestDialog.dismiss();
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {}
                                });
                            }
                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {}
                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {}
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onCancelled(DatabaseError databaseError) {}
    }


    private class ResponseNotificationsListener implements ChildEventListener{

        @Override
        public void onChildAdded(final DataSnapshot dataSnapshotSuper, String s) {
                String respondingCar = dataSnapshotSuper.getKey();
                Log.d(TAG, "respondingCarIs: "+respondingCar);
                DatabaseReference donatingChargeTempRef = dataSnapshotSuper.child("donatingCharge").getRef();
                donatingChargeTempRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String donatingCharge = dataSnapshot.getValue(String.class);
                        Log.d(TAG, "donatingChargeIs :"+donatingCharge);
                        DatabaseReference otpRef = dataSnapshotSuper.child("OTP").getRef();
                        otpRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String otp = dataSnapshot.getValue(String.class);
                                Log.d(TAG, "otp is: "+otp);
                                Toast.makeText(MapsActivity.this, "USE OTP -1234- TO CHARGE YOUR CAR", Toast.LENGTH_SHORT).show();
                                DatabaseReference curLocTemp = dataSnapshotSuper.child("currentLocation").getRef();
                                curLocTemp.addChildEventListener(new ChildEventListener() {
                                    @Override
                                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                        new GeoFire(dataSnapshotSuper.getRef()).getLocation("currentLocation", new LocationCallback() {
                                            @Override
                                            public void onLocationResult(String key, GeoLocation location) {
                                                donorMarker = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
                                                .title("Donor Car")
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.o_car)));
                                                secondaryDestinationLocation = new Location(LocationManager.GPS_PROVIDER);
                                                secondaryDestinationLocation.setLatitude(location.latitude);
                                                secondaryDestinationLocation.setLongitude(location.longitude);

                                                innerCircle.remove();
                                                outerCircle.remove();
                                                carState = CarState.TravellingToSecondaryDestination;
                                                getDirections(currentLocation, secondaryDestinationLocation);
                                                //
                                                //
                                                //EDITED HERE
                                                dataSnapshotSuper.getRef().removeValue();
                                            }
                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {}
                                        });
                                    }
                                    @Override
                                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                                    @Override
                                    public void onChildRemoved(DataSnapshot dataSnapshot) { }
                                    @Override
                                    public void onChildMoved(DataSnapshot dataSnapshot, String s) { }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) { }
                                });
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {}
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
        @Override
        public void onCancelled(DatabaseError databaseError) {}
    }

    private void findTotalPathDistance(){
        pointDistances = new ArrayList<Double>();
        for(int i = 0; i< originToPrimaryDestinationPathPoints.size()-1; i++){

            Location first = new Location(LocationManager.GPS_PROVIDER);
            first.setLatitude(originToPrimaryDestinationPathPoints.get(i).latitude);
            first.setLongitude(originToPrimaryDestinationPathPoints.get(i).longitude);

            Location second = new Location(LocationManager.GPS_PROVIDER);
            second.setLatitude(originToPrimaryDestinationPathPoints.get(i+1).latitude);
            second.setLongitude(originToPrimaryDestinationPathPoints.get(i+1).longitude);
            double tempDistance = first.distanceTo(second)/1000;
            pointDistances.add(tempDistance);
            totalPathDistance+=tempDistance;
        }
        distanceToDestination = totalPathDistance;

        Frag1 newFragment = new Frag1();
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.fragHere, newFragment);
        trans.addToBackStack(null);
        trans.commit();

    }

    private void findCurrentToDonorPathDistance(){
        currentToDonorpointDistances = new ArrayList<Double>();
        for(int i = 0; i< currentLocationToSecondaryDestinationPathPoints.size()-1; i++){

            Location first = new Location(LocationManager.GPS_PROVIDER);
            first.setLatitude(currentLocationToSecondaryDestinationPathPoints.get(i).latitude);
            first.setLongitude(currentLocationToSecondaryDestinationPathPoints.get(i).longitude);

            Location second = new Location(LocationManager.GPS_PROVIDER);
            second.setLatitude(currentLocationToSecondaryDestinationPathPoints.get(i+1).latitude);
            second.setLongitude(currentLocationToSecondaryDestinationPathPoints.get(i+1).longitude);
            double tempDistance = first.distanceTo(second)/1000;
            currentToDonorpointDistances.add(tempDistance);
            distanceToDonor+=tempDistance;
        }
        distanceToDestination = distanceToDonor;
    }


}
