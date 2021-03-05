package com.ahmedjeylani.audiostories;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.BIO_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.IMAGE_REF_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NAME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGES_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGE_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.UNIQUEID_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERNAME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USER_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USER_TYPE_KEY_NAME;
import static com.ahmedjeylani.audiostories.Utilities.getCurrentDateAndTimeFile;

public class RegisterActivity extends AppCompatActivity {
    private static final Pattern userPasswordPattern =
            Pattern.compile("^(?=.*[A-Za-z])(?=\\S+$).{6,}$"); // Password has to be minimum 6 characters

    private static final Pattern usernamePattern = Pattern.compile("\\A\\w{3,18}\\z"); // no special characters, only numbers and letters and no whitepaces

    private EditText nameTextField, emailTextField, passwordTextField, reenterPasswordTextField, usernameTextField;
    private Button signUpBtn;
    private CircleImageView profileImage;
    private static final int PICK_IMAGE = 10;
    private static final int REQUEST_TAKE_PHOTO = 9;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private FirebaseUser currentUser;
    private Uri resultUri;
    private String STRdownloadURI, currentPhotoPath;
    private Boolean usernameExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setTitle(R.string.register_activity);

        nameTextField = findViewById(R.id.name_field_id);
        usernameTextField = findViewById(R.id.username_field_id);
        emailTextField = findViewById(R.id.email_field_id);
        passwordTextField = findViewById(R.id.password_field_id);
        reenterPasswordTextField = findViewById(R.id.reenter_password_field_id);
        profileImage = findViewById(R.id.register_profile_image_id);

        // Disable login button if text fields are empty
        nameTextField.addTextChangedListener(loginTextWatcher);
        usernameTextField.addTextChangedListener(loginTextWatcher);
        reenterPasswordTextField.addTextChangedListener(loginTextWatcher);


        signUpBtn = findViewById(R.id.signUp_button_id);

        signUpBtn.setOnClickListener(v -> registerUser());

        profileImage.setOnClickListener(view -> {
            String[] options = new String[] {"Camera", "Gallery", "Cancel"};
            final MaterialAlertDialogBuilder alertOnChoosingImage = new MaterialAlertDialogBuilder(RegisterActivity.this);
            alertOnChoosingImage.setTitle("Choose one of the options below")
                    .setItems(options, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    openCamera();
                                    break;
                                case 1:
                                    openGallery();
                                    break;
                                case 2:
                                    break;

                            }
                        }
                    });
            alertOnChoosingImage.create();
            alertOnChoosingImage.show();
        });

    }

    private TextWatcher loginTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // To do something later on
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(!Utilities.isFieldEmpty(nameTextField) || !Utilities.isFieldEmpty(usernameTextField)
            || !Utilities.isFieldEmpty(reenterPasswordTextField)) {
                signUpBtn.setEnabled(true);
                signUpBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                signUpBtn.setTextColor(Color.WHITE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // To do something
        }
    };

    private void registerUser() {
        String email = emailTextField.getText().toString();
        String password = passwordTextField.getText().toString();

        if(!validateUsername(usernameTextField) || !Utilities.validateEmail(emailTextField) ||
                !validateName(nameTextField) || !validatePassword(passwordTextField, reenterPasswordTextField)) {
            return;
        }

        if(resultUri == null) {
            MaterialAlertDialogBuilder alertOnRegister = new MaterialAlertDialogBuilder(RegisterActivity.this);
            alertOnRegister.setTitle("Warning!")
                    .setMessage("Are you sure you will like to continue without uploading a profile image.");
            // Set up the buttons
            alertOnRegister.setPositiveButton("Yes", (dialog, which) -> {
                startRegistrationProcess(email, password, null);
            });
            alertOnRegister.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            alertOnRegister.show();
            return;
        }

        startRegistrationProcess(email, password, resultUri);
    }

    private void startRegistrationProcess(String email, String password, Uri imageUri) {
        disableButton();
        FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener(authResult -> {
            if (authResult.getUser().isAnonymous()) {
                usernameExists = false;
                databaseReference.child(USER_REF).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Iterable<DataSnapshot> children = dataSnapshot.getChildren();

                        for(DataSnapshot child: children) {
                            BaseUser user = child.getValue(BaseUser.class);
                            if (usernameTextField.getText().toString().toLowerCase().equals(user.getUsername().toLowerCase())) {
                                Toast.makeText(RegisterActivity.this, "Username already exits" , Toast.LENGTH_SHORT).show();
                                usernameExists = true;
                                FirebaseAuth.getInstance().getCurrentUser().delete();
                                FirebaseAuth.getInstance().signOut();
                                enableButton();
                                return;
                            }
                        }
                        FirebaseAuth.getInstance().getCurrentUser().delete();
                        FirebaseAuth.getInstance().signOut();
                        if (RegisterActivity.this.usernameExists) {
                            enableButton();
                            return;
                        }
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                currentUser = authResult.getUser();
                                if (imageUri != null) {
                                    final String fileNameProp = currentUser.getUid() + PROFILE_IMAGE_NAME;
                                    StorageReference filePath = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_STORAGE_REF).child(fileNameProp);
                                    filePath.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                                            STRdownloadURI = uri.toString();
                                            sendUserInfoToDatabase(STRdownloadURI);
                                        });

                                    });
                                } else {
                                    sendUserInfoToDatabase("");
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                enableButton();
                                Toast.makeText(RegisterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        enableButton();
                    }
                });
            }
        });
    }

    private void sendUserInfoToDatabase(String downloadUri) {
        DatabaseReference uniqueIdRef = databaseReference.child(USER_REF).child(currentUser.getUid());

        Map<String,Object> map = new HashMap<String, Object>();
        map.put(UNIQUEID_KEY_NAME, currentUser.getUid());
        map.put(USERNAME_KEY_NAME, usernameTextField.getText().toString());
        map.put(NAME_KEY_NAME, nameTextField.getText().toString()); // TODO: this has been removed?
        map.put(IMAGE_REF_KEY_NAME, downloadUri);
        map.put(USER_TYPE_KEY_NAME, "standard");
        map.put(BIO_KEY_NAME,"");

        uniqueIdRef.updateChildren(map);

        if (currentUser != null) {
            //Takes about 20-30 mins to send
            currentUser.sendEmailVerification().addOnCompleteListener(RegisterActivity.this, task -> {
                if(task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Register Successful", Toast.LENGTH_SHORT).show();
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(RegisterActivity.this, "Email Verification Sent", Toast.LENGTH_SHORT).show();
                    RegisterActivity.this.finish();
                } else {
                    Toast.makeText(RegisterActivity.this,"Email Verification hasn't sent, please contact Support",Toast.LENGTH_LONG).show();
                }

            });
        }
    }

    private void disableButton() {
        signUpBtn.setEnabled(false);
        signUpBtn.setText(R.string.registering_loading);
    }

    private void enableButton() {
        signUpBtn.setEnabled(true);
        signUpBtn.setText(R.string.sign_up_btn);
    }

    private boolean validateUsername(EditText usernameInput) {
        String username = usernameTextField.getText().toString();

        if (Utilities.isFieldEmpty(usernameInput)) {
            usernameTextField.setError("Username can't be empty");
            usernameTextField.requestFocus();
            return false;
        }  else if(!usernamePattern.matcher(username).matches()) {
            usernameTextField.setError("Please enter a valid username");
            usernameTextField.requestFocus();
            return false;
        } else {
            usernameTextField.setError(null);
            return true;
        }
    }

    private boolean validateName(EditText fullNameInput) {
        String userFullNameInput = nameTextField.getText().toString();

        if (Utilities.isFieldEmpty(fullNameInput)){
            nameTextField.setError("Name can't be empty");
            nameTextField.requestFocus();
            return false;
        } else if (!userFullNameInput.matches("^[a-zA-Z]+(\\s[a-zA-Z]+)?$")){
            nameTextField.setError("Name is invalid, alphabetical characters only");
            nameTextField.requestFocus();
            return false;
        } else {
            nameTextField.setError(null);
            return true;
        }
    }

    private boolean validatePassword(EditText passwordInputField, EditText passwordInputValField) {
        String passwordInput = passwordTextField.getText().toString();
        String passwordInputVal = reenterPasswordTextField.getText().toString();

        if (Utilities.isFieldEmpty(passwordInputField) || Utilities.isFieldEmpty(passwordInputValField)) {
            passwordTextField.setError("Password can't be empty", null);
            passwordInputField.setError("Password can't be empty", null);
            passwordTextField.requestFocus();
            passwordInputField.requestFocus();
            return false;
        } else if (!userPasswordPattern.matcher(passwordInput).matches()) {
            passwordTextField.setError("Password not strong enough.", null);
            passwordTextField.requestFocus();
            return false;
        } else if(!passwordInput.equals(passwordInputVal)) {
            reenterPasswordTextField.setError("Passwords don't match", null);
            reenterPasswordTextField.requestFocus();
            return false;
        } else if (passwordInput.length() < 6) {
            passwordTextField.setError("Password is too short, \nmin. 6 characters",
                    null);
            passwordTextField.requestFocus();
            return false;
        } else {
            passwordTextField.setError(null);
            return true;
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void cropImage(Uri imageFileUri) {
        CropImage.activity(imageFileUri)
                .setGuidelines(CropImageView.Guidelines.OFF)
                .setFixAspectRatio(true)
                .setAspectRatio(1,1)
                .setActivityTitle("Crop Event Image")
                .setBackgroundColor(Color.parseColor("#86FF0004"))
                .setBorderCornerColor(Color.WHITE)
                .setBorderLineColor(Color.parseColor("#0191e4"))
                .start(this);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = getCurrentDateAndTimeFile();
        String imageFileName = "AudioStories" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            //gets Uri of the chosen image
            Uri imageFileUri = data.getData();
            cropImage(imageFileUri);

        } else if (resultCode == RESULT_OK && requestCode == REQUEST_TAKE_PHOTO) {
            galleryAddPic();
            File f = new File(currentPhotoPath);
            Uri imageFileUri = Uri.fromFile(f);
            cropImage(imageFileUri);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();
                profileImage.setImageURI(resultUri);


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
