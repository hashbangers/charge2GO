package com.example.rkinabhi.merchack;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "from main activity";
    EditText name;
    EditText charge;
    EditText mileage;
    Button goToMap;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.Name);
        charge = findViewById(R.id.Charge);
        mileage = findViewById(R.id.Mileage);
        goToMap = findViewById(R.id.go_to_map);
        intent = new Intent(this, MapsActivity.class);
        goToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("Name", name.getText().toString());
                intent.putExtra("Charge", charge.getText().toString());
                intent.putExtra("Mileage", mileage.getText().toString());
                startActivity(intent);
            }
        });
    }
}
