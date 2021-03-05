package com.ahmedjeylani.audiostories;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.AUDIOREF_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.AUDIO_STORY_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.CREATOR_ID_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.DATE_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.FEED_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.FILE_NAME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.INFO_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NO_LIKES_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NO_RECORDINGS_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.STORIES_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.UNIQUEID_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERNAME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Utilities.getCurrentDateAndTimeFeed;
import static com.ahmedjeylani.audiostories.Utilities.getCurrentDateAndTimeFile;

public class CreateAudioStory extends AppCompatActivity {

    private ImageButton recordBtn, playBtn;
    private Chronometer chronometer;
    private ProgressBar progressBar;

    private BaseUser userInfo;
    private boolean setFromHomePage;
    private Feed chosenFeed;

    private StorageReference storageReference;
    private DatabaseReference feedRef;

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private static String filePath = null;

    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private boolean uploadingAudio = false;
    private boolean isRecording = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_audio_story);

        getSupportActionBar().setTitle(R.string.create_audio_activity);

        recordBtn = findViewById(R.id.record_button_id);
        playBtn = findViewById(R.id.play_recording_button_id);
        ImageButton doneBtn = findViewById(R.id.done_button_id);
        chronometer = findViewById(R.id.timer_chronometer_id);
        progressBar = findViewById(R.id.recording_progressbar_id);
        chronometer.setText("00");
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long t = SystemClock.elapsedRealtime() - chronometer.getBase();
                chronometer.setText(DateFormat.format("ss", t));
                int seconds = Integer.parseInt(chronometer.getText().toString());
                progressBar.setProgress(seconds * 5, true);
            }
        });

        playBtn.setEnabled(false);
        doneBtn.setEnabled(false);
        playBtn.setTag(R.drawable.ic_play_recording_white);


        ActivityCompat.requestPermissions(CreateAudioStory.this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        Intent intent = getIntent();
        userInfo = UserCache.loadCache();
        setFromHomePage = (boolean) intent.getExtras().get("sentFromHome");

        if (setFromHomePage) {
            feedRef = databaseReference.child(FEED_REF);
        } else {
            chosenFeed = (Feed) intent.getExtras().get("chosenFeed");
            feedRef = databaseReference.child(STORIES_REF).child(chosenFeed.getUniqueID());
        }

        storageReference = FirebaseStorage.getInstance().getReference().child(AUDIO_STORY_STORAGE_REF);

        recordBtn.setOnTouchListener((view, motionEvent) -> {

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                Toast.makeText(CreateAudioStory.this, "Recording...", Toast.LENGTH_SHORT).show();
                // Record to the external cache directory for visibility
                filePath = getExternalCacheDir().getAbsolutePath();
                fileName = "audiostory_" + getCurrentDateAndTimeFile() + ".aac";
                filePath += "/" + fileName;
                chronometer.setBase(SystemClock.elapsedRealtime());
                chronometer.start();
                startRecording();

            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                stopRecording();
                playBtn.setEnabled(true);
                playBtn.setBackground(getDrawable(R.drawable.oval));
                doneBtn.setEnabled(true);
                doneBtn.setBackground(getDrawable(R.drawable.oval));
                chronometer.stop();
            }

            return false;
        });

        doneBtn.setOnClickListener(view -> {

            if (setFromHomePage) {
                MaterialAlertDialogBuilder alertMsgOnPost = new MaterialAlertDialogBuilder(this);
                alertMsgOnPost.setTitle((R.string.alert_info_title))
                .setMessage((R.string.alert_supp_msg));

                // Set up the audioStoryName
                final EditText audioStoryName = new EditText(this);
                // Specify the type of audioStoryName expected; this, for example, sets the audioStoryName as a password, and will mask the text
                audioStoryName.setInputType(InputType.TYPE_CLASS_TEXT);
                audioStoryName.setSingleLine(false);
                alertMsgOnPost.setView(audioStoryName);

                // Set up the buttons
                alertMsgOnPost.setPositiveButton("OK", (dialog, which) -> sendStoryToDatabase(audioStoryName.getText().toString()));
                alertMsgOnPost.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                alertMsgOnPost.show();
            } else {
                sendStoryToDatabase("");
            }

        });

        playBtn.setOnClickListener(view -> {
            if (playBtn.getTag().equals(R.drawable.ic_play_recording_white)) {
                startPlaying();
                playBtn.setImageResource(R.drawable.ic_stop_recording_white);
                playBtn.setTag(R.drawable.ic_stop_recording_white);
                recordBtn.setEnabled(false);
            } else {
                stopPlaying();
                playBtn.setImageResource(R.drawable.ic_play_recording_white);
                playBtn.setTag(R.drawable.ic_play_recording_white);
                recordBtn.setEnabled(true);
            }
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void sendStoryToDatabase(String input) {
        if(uploadingAudio) {
            Toast.makeText(CreateAudioStory.this, "Already Attempting to Upload", Toast.LENGTH_SHORT).show();
            return;
        }
        uploadingAudio = true;
        final String infoFromUser = input;
        final String fileNameProp = userInfo.getUniqueID() +"-"+fileName;
        StorageReference audioRef = storageReference.child(fileNameProp);
        Uri audioUri = Uri.fromFile(new File(filePath));
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/aac")
                .build();
        audioRef.putFile(audioUri,metadata).addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
            String strDownloadUri = uri.toString();

            DatabaseReference saveFeedRef = feedRef.push();
            String feedKey = saveFeedRef.getKey();
            String feedInfo = infoFromUser;

            Map<String,Object> map2 = new HashMap<>();
            map2.put(UNIQUEID_KEY_NAME,feedKey);
            map2.put(DATE_KEY_NAME,getCurrentDateAndTimeFeed());
            map2.put(USERNAME_KEY_NAME,userInfo.getUsername());
            map2.put(INFO_KEY_NAME,feedInfo);
            map2.put(AUDIOREF_KEY_NAME, strDownloadUri);
            map2.put(NO_LIKES_KEY_NAME, "0");
            map2.put(NO_RECORDINGS_KEY_NAME, "0");
            map2.put(CREATOR_ID_KEY_NAME, userInfo.uniqueID);
            map2.put(FILE_NAME_KEY_NAME, fileNameProp);

            saveFeedRef.updateChildren(map2);

            if(!setFromHomePage) {

                final DatabaseReference storyRef = FirebaseDatabase.getInstance().getReference().child(FEED_REF).child(chosenFeed.getUniqueID()).child(NO_RECORDINGS_KEY_NAME);
                storyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String noRecordingsStr = dataSnapshot.getValue(String.class);
                        int noRecordings = Integer.valueOf(noRecordingsStr);
                        noRecordings++;

                        storyRef.setValue("" + noRecordings);
                        uploadingAudio = false;
                        finish();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            uploadingAudio = false;
            finish();
        }));
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            Toast.makeText(this, "Playing...", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "file name: " + fileName);
            player.setDataSource(filePath);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(filePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setMaxDuration(20000);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        recorder.setOnInfoListener((mr, what, extra) -> {
            if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
                Toast.makeText(CreateAudioStory.this, "Recording stopped. Time Limit reached",
                        Toast.LENGTH_LONG).show();
                playBtn.setEnabled(true);
                chronometer.stop();
            }
        });

        isRecording = true;
        recorder.start();
    }

    private void stopRecording() {
        try {
            if(recorder!=null && isRecording) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        } catch (Exception e){
            Toast.makeText(CreateAudioStory.this, "To record hold down the microphone button",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressBar.setProgress(0); // Fixes bug that doesn't clear progress
    }
}
