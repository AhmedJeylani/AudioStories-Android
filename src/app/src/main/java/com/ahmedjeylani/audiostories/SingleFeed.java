package com.ahmedjeylani.audiostories;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.ListAdapters.CustomStoryListAdapter;
import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGES_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGE_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.STORIES_REF;

public class SingleFeed extends AppCompatActivity implements OnCompletionListener {

    private BaseUser userInfo;
    private Feed chosenFeed;
    private ImageButton playBtn, addBtn;
    private ListView feedListView;
    private ArrayList<Feed> feedList = new ArrayList<>();
    private MediaPlayer mediaPlayer = null;
    private MediaPlayer mp = null;
    private final String LOG_TAG = "SingleFeed";
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private Runnable runnable;
    private Handler handler;
    private int playedCounter = 0;
    private Button playAllBtn;
    private boolean firstStoryPlaying = false;
    private RelativeLayout emptyState;
    private CustomStoryListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_feed);

        Intent homeIntent = getIntent();
        userInfo = UserCache.loadCache();
        chosenFeed = (Feed) homeIntent.getExtras().get("pressedFeed");

        feedListView = findViewById(R.id.single_feed_list);
        TextView username = findViewById(R.id.single_feed_username_id);
        TextView info = findViewById(R.id.single_feed_info_id);
        TextView date = findViewById(R.id.single_feed_date_id);
        TextView createAudioText = findViewById(R.id.create_audio_text_id);
        playBtn = findViewById(R.id.single_feed_play_button_id);
        addBtn = findViewById(R.id.single_feed_addBtn_id);
        CircleImageView feedImage = findViewById(R.id.single_feed_image_id);
        progressBar = findViewById(R.id.single_feed_audio_progressbar_id);
        emptyState = findViewById(R.id.empty_state_id);

        handler = new Handler();
        seekBar = new SeekBar(this);
        seekBar.setVisibility(View.INVISIBLE);
        playBtn.setTag(R.drawable.ic_play_media_blue);

        playAllBtn = findViewById(R.id.play_all_button_id);

        username.setText(chosenFeed.getUsername());
        info.setText(chosenFeed.getInfo());
        date.setText(chosenFeed.getDate());

        StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_STORAGE_REF).child(chosenFeed.getCreatorID() + PROFILE_IMAGE_NAME);
        storage.getDownloadUrl().addOnSuccessListener(uri -> Picasso.get().load(uri).into(feedImage));

        addBtn.setOnClickListener(view -> {
            Intent addIntent = new Intent(SingleFeed.this,CreateAudioStory.class);
            addIntent.putExtra("chosenFeed",chosenFeed);
            addIntent.putExtra("sentFromHome",false);
            startActivity(addIntent);
        });

        createAudioText.setOnClickListener(view -> {
            Intent addIntent = new Intent(SingleFeed.this,CreateAudioStory.class);
            addIntent.putExtra("chosenFeed",chosenFeed);
            addIntent.putExtra("sentFromHome",false);
            startActivity(addIntent);
        });

        playBtn.setOnClickListener(view -> {
            if(playBtn.getTag().equals(R.drawable.ic_play_media_blue)) {
                startPlaying(chosenFeed.getAudioRef());
                playBtn.setBackgroundResource(R.drawable.ic_stop_blue);
                playBtn.setTag(R.drawable.ic_stop_blue);
                changeProgressBar();
            } else {
                stopPlaying();
                playBtn.setBackgroundResource(R.drawable.ic_play_media_blue);
                playBtn.setTag(R.drawable.ic_play_media_blue);
            }
        });

        playAllBtn.setOnClickListener(view -> {

            if(firstStoryPlaying) {
                Toast.makeText(SingleFeed.this, "Already playing a story", Toast.LENGTH_SHORT).show();
                return;
            }
            if(playAllBtn.getText().toString().toLowerCase().equals("play all")) {
                playAllBtn.setText("Stop");
                try {
                    mp = new MediaPlayer();
                    mp.setDataSource(chosenFeed.getAudioRef());
                    mp.prepare(); // might take long! (for buffering, etc)
                    mp.start();
                    mp.setOnCompletionListener(SingleFeed.this); // Important
                } catch (Exception exc) {

                }
            } else if(playAllBtn.getText().toString().toLowerCase().equals("stop")) {
                playAllBtn.setText("Play All");
                try {
                    mp.stop();
                    mp.release();
                    mp = null;
                } catch (Exception exc) {

                }
            }


        });

        Utilities.showEmptyState(feedList, feedListView, null, emptyState, addBtn);
        DatabaseReference singleFeedRef = FirebaseDatabase.getInstance().getReference().child(STORIES_REF);
        singleFeedRef.child(chosenFeed.getUniqueID()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                feedList.add(feed);
                Utilities.showEmptyState(feedList, feedListView, null, emptyState, addBtn);
                adapter = new CustomStoryListAdapter(SingleFeed.this,feedList, userInfo, chosenFeed.getUniqueID());
                feedListView.setAdapter(adapter);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                int index = -1;
                for(Feed item : feedList) {
                    if(item.getUniqueID().equals(feed.getUniqueID())) {
                        index = feedList.indexOf(item);
                    }
                }

                if(index != -1) {
                    feedList.remove(index);
                    Utilities.showEmptyState(feedList, feedListView, null, emptyState, addBtn);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(playedCounter < feedList.size()) {
            try {
                if (mp != null) {
                    if (mp.isPlaying()) {
                        mp.stop();
                        mp.reset();
                        mp.release();
                        mp = null;
                    }
                }
                //for(Feed item: feedList) {
                    mp = new MediaPlayer();
                    mp.setAudioAttributes(
                            new AudioAttributes
                                    .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
                    mp.setDataSource(feedList.get(playedCounter).getAudioRef());
                    mp.prepare(); // might take long! (for buffering, etc)
                    mp.start();
                    mp.setOnCompletionListener(this);
                    //Log.e(LOG_TAG, playedCounter + " --------- " + feedList.get(playedCounter).getInfo()); 
                    playedCounter++;
                //}
            } catch (Exception exc) {
                
            }
        } else if(playedCounter == feedList.size()){
            playedCounter = 0;
            if (mp != null) {
                
                mp.stop();
                mp.release();
                mp = null;

                
            }
            playAllBtn.setText("Play All");
            Toast.makeText(this, "Full Story Played", Toast.LENGTH_SHORT).show();
        }
    }

    private void startPlaying(String url) {
        firstStoryPlaying = true;
        feedListView.setEnabled(false);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
            progressBar.setMax(mediaPlayer.getDuration());
            seekBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        feedListView.setEnabled(true);
        firstStoryPlaying = false;
        progressBar.setProgress(0);
        seekBar.setProgress(0);
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void changeProgressBar() {

        if(mediaPlayer != null) {
            progressBar.setProgress(mediaPlayer.getCurrentPosition());
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
        } else {
            return;
        }


        if(mediaPlayer.isPlaying()) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    changeProgressBar();
                }
            };
            handler.postDelayed(runnable,1000);
        } else {
            stopPlaying();
            playBtn.setBackgroundResource(R.drawable.ic_play_media_blue);
            playBtn.setTag(R.drawable.ic_play_media_blue);
        }

    }
}
