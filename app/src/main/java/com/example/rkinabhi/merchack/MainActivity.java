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
    Button goToMap;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.Name);
        goToMap = findViewById(R.id.go_to_map);
        intent = new Intent(this, MapsActivity.class);
        goToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: "+name.getText());
                intent.putExtra("Name", name.getText().toString());
                startActivity(intent);
            }
        });
    }
}
