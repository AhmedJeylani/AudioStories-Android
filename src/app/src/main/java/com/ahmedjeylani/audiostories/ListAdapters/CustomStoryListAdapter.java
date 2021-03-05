package com.ahmedjeylani.audiostories.ListAdapters;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NO_LIKES_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGES_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGE_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.STORIES_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERS_LIKED_MESSAGES_REF;

public class CustomStoryListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Feed> feedList;
    private MediaRecorder recorder = null;
    private MediaPlayer mediaPlayer = null;
    private final String LOG_TAG = "CustomFeedListAdapter";
    //private ImageButton playBtn;
    //private ProgressBar progressBar;
    private SeekBar seekBar;
    private Runnable runnable;
    private Handler handler;
    private DatabaseReference usersLikedMessagesRef ,numberOfLikesRef;
    private BaseUser currentUserInfo;
    private boolean processLike;
    private String parentFeedId;


    public CustomStoryListAdapter(Context c, ArrayList<Feed> ed, BaseUser currentUserInfo, String parentFeedId) {
        this.context = c;
        this.feedList = ed;
        this.currentUserInfo = currentUserInfo;
        this.parentFeedId = parentFeedId;
    }


    @Override
    public int getCount() {
        return feedList.size();
    }

    @Override
    public Object getItem(int position) {
        return feedList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //This controls how the strings that were passed in are laid out
    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        //inflate = prepare or get ready for rendering
        //context = background information
        //this is equal to one custom row(view)
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_story_row, parent, false);
        }

        TextView username = convertView.findViewById(R.id.story_feed_username_id);
        TextView info = convertView.findViewById(R.id.story_feed_info_id);
        TextView date = convertView.findViewById(R.id.story_feed_date_id);
        TextView noLikes = convertView.findViewById(R.id.story_noLikes_id);
        final ImageButton playBtn = convertView.findViewById(R.id.story_play_button_id);
        final ImageButton likeBtn = convertView.findViewById(R.id.story_like_button_id);
        final CircleImageView feedImage = convertView.findViewById(R.id.story_feed_image_id);
        final ProgressBar progressBar = convertView.findViewById(R.id.story_feed_audio_progressbar_id);
        handler = new Handler();
        seekBar = new SeekBar(context);
        seekBar.setVisibility(View.INVISIBLE);
        playBtn.setTag(R.drawable.ic_play_media_white);

        final Feed singleFeedData = (Feed) this.getItem(position);

        usersLikedMessagesRef = FirebaseDatabase.getInstance().getReference().child(USERS_LIKED_MESSAGES_REF).child(currentUserInfo.getUniqueID());

        username.setText(singleFeedData.getUsername());
        info.setText(singleFeedData.getInfo());
        date.setText(singleFeedData.getDate());
        noLikes.setText(singleFeedData.getNoLikes());

        // Fixes the flickering issue which makes image disappear and reappear
        // This also causes a bug that makes the event the wrong image
        //if(feedImage.getDrawable() == null) {

        StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_STORAGE_REF).child(singleFeedData.getCreatorID() + PROFILE_IMAGE_NAME);

        storage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).placeholder(R.drawable.profile_image_placeholder).into(feedImage);
            }
        });
        //}

//        info.setText(singleEventData.getDesc());
//        feedImage.setImageURI(Uri.parse(singleEventData.getImageRef()));

        usersLikedMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(singleFeedData.getUniqueID())) {
                    likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_liked_orange);
                }
                else {
                    likeBtn.setBackgroundResource(R.drawable.ic_thumbs_up_white);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mediaPlayer != null && playBtn.getTag().equals(R.drawable.ic_play_media_white)) {
                    Toast.makeText(context, "Already playing a story", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(playBtn.getTag().equals(R.drawable.ic_play_media_white)) {
                    startPlaying(singleFeedData.getAudioRef(), progressBar);
                    playBtn.setBackgroundResource(R.drawable.ic_stop_recording_white);
                    playBtn.setTag(R.drawable.ic_stop_recording_white);
                    changeProgressBar(progressBar, playBtn);
                } else {
                    stopPlaying(progressBar);
                    playBtn.setBackgroundResource(R.drawable.ic_play_media_white);
                    playBtn.setTag(R.drawable.ic_play_media_white);
                }
            }
        });

        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processLike = true;
                usersLikedMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final DataSnapshot userLikesDataSnapshot = dataSnapshot;
                        numberOfLikesRef = FirebaseDatabase.getInstance().getReference().child(STORIES_REF).child(parentFeedId).child(singleFeedData.getUniqueID()).child(NO_LIKES_KEY_NAME);
                        numberOfLikesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot ds) {
                                if(processLike) {
                                    if(userLikesDataSnapshot.hasChild(singleFeedData.getUniqueID())) {
                                        likeBtn.setBackgroundResource(R.drawable.ic_thumbs_up_white);
                                        usersLikedMessagesRef.child(singleFeedData.getUniqueID()).removeValue();
                                        String likesStr = ds.getValue(String.class);
                                        int noLikes = Integer.valueOf(likesStr);
                                        noLikes--;
                                        String newNoLikes = ""+ noLikes;
                                        numberOfLikesRef.setValue(newNoLikes);
                                        processLike = false;
                                    } else {
                                        likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_liked_orange);
                                        String likesStr = ds.getValue(String.class);
                                        int noLikes = Integer.valueOf(likesStr);
                                        noLikes++;
                                        String newNoLikes = ""+ noLikes;
//                                        chatLikes.setText(newNoLikes);
                                        Toast.makeText(context,"Liked", Toast.LENGTH_SHORT).show();
                                        usersLikedMessagesRef.child(singleFeedData.getUniqueID()).setValue(singleFeedData.getUsername());
                                        numberOfLikesRef.setValue(newNoLikes);
                                        processLike = false;
                                    }

                                }
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

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) {
                    mediaPlayer.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        notifyDataSetChanged();

        return convertView;
    }

    private void startPlaying(String url, ProgressBar progressBar) {
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

    private void stopPlaying(ProgressBar progressBar) {
        progressBar.setProgress(0);
        seekBar.setProgress(0);
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void changeProgressBar(final ProgressBar progressBar, final ImageButton playBtn) {

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
                    changeProgressBar(progressBar, playBtn);
                }
            };
            handler.postDelayed(runnable,1000);
        } else {
            stopPlaying(progressBar);
            playBtn.setBackgroundResource(R.drawable.ic_play_media_white);
            playBtn.setTag(R.drawable.ic_play_media_white);
        }

    }
}
