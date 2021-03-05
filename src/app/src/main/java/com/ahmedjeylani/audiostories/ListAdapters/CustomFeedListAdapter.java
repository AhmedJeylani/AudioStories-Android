package com.ahmedjeylani.audiostories.ListAdapters;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.R;
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

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.FEED_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NO_LIKES_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGES_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGE_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERS_LIKED_MESSAGES_REF;

public class CustomFeedListAdapter extends BaseAdapter {
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
    private Feed playingFeed;


    public CustomFeedListAdapter(Context c, ArrayList<Feed> ed, BaseUser currentUserInfo) {
        this.context = c;
        this.feedList = ed;
        this.currentUserInfo = currentUserInfo;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        //inflate = prepare or get ready for rendering
        //context = background information
        //this is equal to one custom row(view)
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_feed_row, parent, false);
        }

        final Resources resources = convertView.getResources();

        RelativeLayout customRow = convertView.findViewById(R.id.customRowLayout_id);
        TextView username = convertView.findViewById(R.id.feed_username_id);
        TextView info = convertView.findViewById(R.id.feed_info_id);
        TextView date = convertView.findViewById(R.id.feed_date_id);
        TextView noRecordings = convertView.findViewById(R.id.noRecordings_id);
        final TextView noLikes = convertView.findViewById(R.id.noLikes_id);
        final ImageButton playBtn = convertView.findViewById(R.id.play_button_id);
        final ImageButton likeBtn = convertView.findViewById(R.id.like_button_id);
        ImageButton recordBtn = convertView.findViewById(R.id.addRecording_button_id);
        final CircleImageView feedImage = convertView.findViewById(R.id.feed_image_id);
        final ProgressBar progressBar = convertView.findViewById(R.id.feed_audio_progressbar_id);
        handler = new Handler();
        seekBar = new SeekBar(context);
        seekBar.setVisibility(View.INVISIBLE);
        playBtn.setTag(R.drawable.ic_play_media_blue);

        final Feed singleFeedData = (Feed) this.getItem(position);

        usersLikedMessagesRef = FirebaseDatabase.getInstance().getReference().child(USERS_LIKED_MESSAGES_REF).child(currentUserInfo.getUniqueID());

        username.setText(singleFeedData.getUsername());
        info.setText(singleFeedData.getInfo());
        date.setText(singleFeedData.getDate());
        noRecordings.setText(singleFeedData.getNoRecordings());
        noLikes.setText(singleFeedData.getNoLikes());

        StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_STORAGE_REF).child(singleFeedData.getCreatorID() + PROFILE_IMAGE_NAME);

        storage.getDownloadUrl().addOnSuccessListener(uri -> Picasso.get().load(uri).placeholder(R.drawable.profile_image_placeholder).into(feedImage));

        usersLikedMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(singleFeedData.getUniqueID())) {
                    likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_liked_orange);
                    noLikes.setTextColor(resources.getColor(R.color.colorAccent));
                }
                else {
                    likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_blue);
                    noLikes.setTextColor(resources.getColor(R.color.colorPrimary));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        playBtn.setOnClickListener(view -> {
            if(mediaPlayer != null && playingFeed != singleFeedData) {
                Toast.makeText(context, "Already playing a story", Toast.LENGTH_SHORT).show();
                return;
            }
            if(playBtn.getTag().equals(R.drawable.ic_play_media_blue) && playingFeed != singleFeedData) {
                playingFeed = singleFeedData;
                startPlaying(singleFeedData.getAudioRef(), progressBar);
                playBtn.setBackgroundResource(R.drawable.ic_stop_blue);
                playBtn.setTag(R.drawable.ic_stop_blue);
                changeProgressBar(progressBar, playBtn);
            } else {
                stopPlaying(progressBar);
                playBtn.setBackgroundResource(R.drawable.ic_play_media_blue);
                playBtn.setTag(R.drawable.ic_play_media_blue);
            }
        });

        likeBtn.setOnClickListener(view -> {
            processLike = true;
            usersLikedMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final DataSnapshot userLikesDataSnapshot = dataSnapshot;
                    numberOfLikesRef = FirebaseDatabase.getInstance().getReference().child(FEED_REF).child(singleFeedData.getUniqueID()).child(NO_LIKES_KEY_NAME);
                    numberOfLikesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot ds) {
                            if(processLike) {
                                if(userLikesDataSnapshot.hasChild(singleFeedData.getUniqueID())) {
                                    likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_blue);
                                    noLikes.setTextColor(resources.getColor(R.color.colorPrimary));
                                    usersLikedMessagesRef.child(singleFeedData.getUniqueID()).removeValue();
                                    String likesStr = ds.getValue(String.class);
                                    int noLikes1 = Integer.valueOf(likesStr);
                                    noLikes1--;
                                    String newNoLikes = ""+ noLikes1;
                                    numberOfLikesRef.setValue(newNoLikes);
                                    processLike = false;
                                } else {
                                    likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_liked_orange);
                                    noLikes.setTextColor(resources.getColor(R.color.colorAccent));
                                    String likesStr = ds.getValue(String.class);
                                    int noLikes1 = Integer.valueOf(likesStr);
                                    noLikes1++;
                                    String newNoLikes = ""+ noLikes1;
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
        playingFeed = null;
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
            runnable = () -> changeProgressBar(progressBar, playBtn);
            handler.postDelayed(runnable,1000);
        } else {
            stopPlaying(progressBar);
            playBtn.setBackgroundResource(R.drawable.ic_play_media_blue);
            playBtn.setTag(R.drawable.ic_play_media_blue);
        }

    }
}
