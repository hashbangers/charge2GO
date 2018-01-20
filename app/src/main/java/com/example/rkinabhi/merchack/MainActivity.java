package com.example.rkinabhi.merchack;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "from main activity";
    EditText name;
    EditText charge;
    EditText mileage;
    Button goToMap;
    Intent intent;
    Spinner spinner;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.Name);
        charge = findViewById(R.id.Charge);
        goToMap = findViewById(R.id.go_to_map);
        spinner = findViewById(R.id.car_models);
        ArrayList<String> al = new ArrayList<>();
        al.add("Bclass");
        al.add("EQ");
        adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, al);
        spinner.setAdapter(adapter);
        intent = new Intent(this, MapsActivity.class);
        goToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("Name", name.getText().toString());
                intent.putExtra("Charge", charge.getText().toString());


                FirebaseDatabase.getInstance().getReference("MODELS/CARS/"+spinner.getSelectedItem().toString()+"/Mileage").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        intent.putExtra("Mileage",dataSnapshot.getValue().toString());

                        FirebaseDatabase.getInstance().getReference("MODELS/CARS/"+spinner.getSelectedItem().toString()+"/Batterycapacity").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                intent.putExtra("Batterycapacity", dataSnapshot.getValue().toString());
                                startActivity(intent);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });
    }
}