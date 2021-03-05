package com.ahmedjeylani.audiostories;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.IMAGE_REF_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGES_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGE_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USER_REF;
import static com.ahmedjeylani.audiostories.Utilities.getCurrentDateAndTimeFile;

public class EditProfileActivity extends AppCompatActivity {

    private Button updateProfileImageBtn;
    private CircleImageView profileImage;

    private static final int PICK_IMAGE = 10;
    private static final int REQUEST_TAKE_PHOTO = 9;

    private FirebaseAuth fAuth;
    private FirebaseUser currentUser;
    private BaseUser userInfo;
    private DatabaseReference userReference;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private Uri resultUri;
    private String currentPhotoPath, STRdownloadURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        getSupportActionBar().setTitle(R.string.edit_profile_activity);

        fAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userReference = databaseReference.child(USER_REF);

        userInfo = UserCache.loadCache();
        profileImage = findViewById(R.id.edit_profile_image_id);

        Button resetPasswordLinkBtn = findViewById(R.id.reset_password_btn_id);
        updateProfileImageBtn = findViewById(R.id.change_image_btn_id);

        Utilities.loadImage(userInfo, profileImage);

        // select new profile image
        profileImage.setOnClickListener(view -> {
            String[] options = new String[] {"Camera", "Gallery", "Cancel"};
            final MaterialAlertDialogBuilder alertOnImageChange = new MaterialAlertDialogBuilder(EditProfileActivity.this);
            alertOnImageChange.setTitle("Choose one of the options below")
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
            alertOnImageChange.create();
            alertOnImageChange.show();
        });


        // click update profile image btn
        updateProfileImageBtn.setOnClickListener(v -> uploadProfileImage());

        resetPasswordLinkBtn.setOnClickListener(view -> {

            String userEmail = currentUser.getEmail();

            fAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    MaterialAlertDialogBuilder alertOnPassReset = new MaterialAlertDialogBuilder(this);
                    alertOnPassReset.setTitle("Important")
                            .setMessage("Password reset email has been sent, please check your junk mail if you cannot find it. The app will sign you out. Please reset your password before you sign in.")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    FirebaseAuth.getInstance().signOut();
                                    Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears navigation stack
                                    startActivity(intent);
                                    finish();
                                }
                            });
                    AlertDialog alert = alertOnPassReset.create();
                    alert.show();



                }else{
                    Toast.makeText(EditProfileActivity.this, "Error in sending password reset email", Toast.LENGTH_SHORT).show();
                }
            });

        });
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

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void uploadProfileImage() {

        if (resultUri == null) {
            Toast.makeText(this, "Choose a profile image", Toast.LENGTH_SHORT).show();
        }
        else {
            DatabaseReference uniqueIdRef = userReference.child(userInfo.getUniqueID());
            final String fileNameProp = userInfo.getUniqueID() + PROFILE_IMAGE_NAME;
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_STORAGE_REF).child(fileNameProp);
            filePath.putFile(resultUri).addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                STRdownloadURI = uri.toString();
                Map<String,Object> map = new HashMap<>();
                map.put(IMAGE_REF_KEY_NAME, STRdownloadURI);
                userInfo.setImageRef(STRdownloadURI);
                UserCache.cacheUser(userInfo);
                uniqueIdRef.updateChildren(map);
                updateProfileImageBtn.setVisibility(View.GONE);
                Toast.makeText(this, "Profile Image Updated", Toast.LENGTH_SHORT).show();
            }));
        }
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
                updateProfileImageBtn.setVisibility(View.VISIBLE);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
