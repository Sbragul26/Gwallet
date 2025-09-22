package com.example.gwallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gwallet.HomePage;

public class GetStarted extends AppCompatActivity {

    Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_started);

        // Find button
        btnGetStarted = findViewById(R.id.btnGetStarted);

        // Handle button click
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Example: Move to LoginActivity (you can change it later)
                Intent intent = new Intent(GetStarted.this, HomePage.class);
                startActivity(intent);

                // Optional: show toast
                Toast.makeText(GetStarted.this, "Let's Get Started!", Toast.LENGTH_SHORT).show();

                // Close this activity so user can’t go back
                finish();
            }
        });
    }
}
