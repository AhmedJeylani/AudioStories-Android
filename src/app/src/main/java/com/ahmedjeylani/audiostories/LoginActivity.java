package com.ahmedjeylani.audiostories;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USER_REF;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity{

    private EditText emailTextField,passwordTextField;
    private TextView signUpLink;
    private Button signInBtn;
    private LinearLayout loginProgressView;
    private ScrollView loginFormView;
    private FirebaseAuth fAuth;
    private FirebaseUser fUser;
    private DatabaseReference userDatabase;
    private BaseUser userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailTextField = findViewById(R.id.email);
        passwordTextField = findViewById(R.id.password);
        loginProgressView = findViewById(R.id.progress_view);
        loginFormView = findViewById(R.id.login_form);
        signUpLink = findViewById(R.id.register_link);
        fAuth = FirebaseAuth.getInstance();
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        signInBtn = findViewById(R.id.email_sign_in_button);
        userDatabase = FirebaseDatabase.getInstance().getReference().child(USER_REF);

        getSupportActionBar().setTitle(R.string.login_activity);

        // Creating a clickable sign up text and change the color to orange
        String text = (String) signUpLink.getText();
        SpannableString ss = new SpannableString(text);

        ClickableSpan signUpSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#FCA311"));
                ds.setUnderlineText(false);
            }
        };

        ss.setSpan(signUpSpan, 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signUpLink.setText(ss);
        signUpLink.setMovementMethod(LinkMovementMethod.getInstance());

        TextView forgottenPassTextView = findViewById(R.id.forgetten_pass_id);

        forgottenPassTextView.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, ForgottenPasswordActivity.class);
            startActivity(intent);
        });

        if(fUser != null) {
            showProgressView();
            getUserDetails(userDatabase);
        }

        // Disable login button if text fields are empty
        emailTextField.addTextChangedListener(loginTextWatcher);
        passwordTextField.addTextChangedListener(loginTextWatcher);

        signInBtn.setOnClickListener(view -> {
            String email = emailTextField.getText().toString();
            final String password = passwordTextField.getText().toString();

            if (!Utilities.validateEmail(emailTextField) || !validatePassword(passwordTextField)) {
                return;
            }
            else {
                showProgressView();
                fAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(LoginActivity.this, task -> {
                    if(task.isSuccessful()) {
                        fUser = FirebaseAuth.getInstance().getCurrentUser();
                        if(fUser == null) {
                            hideProgressView();

                            passwordTextField.getText().clear();
                            Toast.makeText(LoginActivity.this, "INVESTIGATE", Toast.LENGTH_SHORT).show();

                        }
                        //TODO - Make this a check to see if Email is verified
                        else if(fUser.isEmailVerified()) {
                            getUserDetails(userDatabase);
                            // If the userInfo has no internet, double check if fUser is null

                        } else if(!fUser.isEmailVerified()) {
                            hideProgressView();
                            MaterialAlertDialogBuilder alertOnUserNotVerified = new MaterialAlertDialogBuilder(LoginActivity.this);

                            alertOnUserNotVerified.setTitle("Error")
                                    .setMessage("You haven't verified your email, please do this before continuing. The verification email could take up 20 minutes to send")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        passwordTextField.getText().clear();
                                        FirebaseAuth.getInstance().signOut();
                                    });
                            alertOnUserNotVerified.show();

                            // If this else statement is hit INVESTIGATE!!
                        } else {
                            hideProgressView();

                            MaterialAlertDialogBuilder alertOnUnknownErr = new MaterialAlertDialogBuilder(LoginActivity.this);
                            alertOnUnknownErr.setTitle("Error")
                                    .setMessage("Unknown Error!, please contact support");
                            alertOnUnknownErr.show();

                        }

                    }
                    // This else statement is run when userInfo enters incorrect details
                    else {
                        hideProgressView();
                        //Log.e("LOGIN", "onComplete: " + task.);
                        passwordTextField.getText().clear();
                        Toast.makeText(LoginActivity.this, "Incorrect Credentials", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private TextWatcher loginTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // To do something later on
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(!Utilities.isFieldEmpty(emailTextField) || !Utilities.isFieldEmpty(passwordTextField)) {
                signInBtn.setEnabled(true);
                signInBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                signInBtn.setTextColor(Color.WHITE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // To do something
        }
    };

    private void getUserDetails(DatabaseReference userReference) {
        // This gets the current users ID which we use to get the users information from the database
        String userID = fUser.getUid();

        // Get all the values in the users database
        userReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                // Can't Login as Event Organiser Yet
                //Stores all the values in studentData
                userInfo = dataSnapshot.getValue(BaseUser.class);


                if(userInfo == null) {
                    Toast.makeText(LoginActivity.this, "We can't get your information", Toast.LENGTH_SHORT).show();
                    FirebaseAuth.getInstance().signOut();
                }
                else {
                    try {

                        Log.v("E_VALUE---------------", userInfo.getName());
                        Intent joinLectureIntent = new Intent(LoginActivity.this,HomeActivity.class);
                        UserCache.cacheUser(userInfo);
                        startActivity(joinLectureIntent);
                        finish();

                    }catch (Exception exception){
                        Log.v("E_VALUE---------------", exception.getMessage());
                    }
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Look into adding something here


            }
        });
    }

    private boolean validatePassword(EditText passwordInput) {

        if (Utilities.isFieldEmpty(passwordInput)) {
            passwordTextField.setError("Password can't be empty", null);
            passwordTextField.requestFocus();
            return false;
        } else {
            passwordTextField.setError(null);
            return true;
        }
    }

    private void showProgressView() {
        loginProgressView.setVisibility(View.VISIBLE);
        loginFormView.setVisibility(View.GONE);
    }

    private void hideProgressView() {
        loginProgressView.setVisibility(View.GONE);
        loginFormView.setVisibility(View.VISIBLE);
    }
}

