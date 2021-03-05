package com.ahmedjeylani.audiostories;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;

public class ForgottenPasswordActivity extends AppCompatActivity {

    private EditText emailResetTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotten_password);

        getSupportActionBar().setTitle(R.string.reset_password_activity);

        emailResetTextField = findViewById(R.id.forgotten_pass_email_id);
        Button resetBtn = findViewById(R.id.reset_button_id);

        resetBtn.setOnClickListener(view -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if(!Utilities.validateEmail(emailResetTextField)) {
                return;
            } else {
                auth.sendPasswordResetEmail(emailResetTextField.getText().toString()).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Toast.makeText(ForgottenPasswordActivity.this, "Reset Password Email Sent", Toast.LENGTH_SHORT).show();
                        ForgottenPasswordActivity.this.finish();
                    } else {
                        Toast.makeText(ForgottenPasswordActivity.this, "Issue sending Reset Password Email", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }


}
