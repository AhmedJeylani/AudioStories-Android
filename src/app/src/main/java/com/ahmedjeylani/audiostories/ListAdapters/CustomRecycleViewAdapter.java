package com.ahmedjeylani.audiostories.ListAdapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.R;
import com.ahmedjeylani.audiostories.SingleFeed;
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

import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.FEED_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.NO_LIKES_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGES_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.PROFILE_IMAGE_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERS_LIKED_MESSAGES_REF;

public class CustomRecycleViewAdapter extends RecyclerView.Adapter<CustomRecycleViewAdapter.ViewHolder>{

    private static final String TAG = "RecyclerViewAdapter";

    private Context context;
    private ArrayList<Feed> feedList;
    private MediaRecorder recorder = null;
    private MediaPlayer mediaPlayer = null;
    private SeekBar seekBar;
    private Runnable runnable;
    private Handler handler;
    private DatabaseReference usersLikedMessagesRef ,numberOfLikesRef;
    private BaseUser currentUserInfo;
    private boolean processLike;
    private Feed playingFeed;

    public CustomRecycleViewAdapter(Context c, ArrayList<Feed> ed, BaseUser currentUserInfo) {
        this.context = c;
        this.feedList = ed;
        this.currentUserInfo = currentUserInfo;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_feed_row, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        final Feed singleFeedData = this.feedList.get(position);
        final Resources resources = context.getResources();
        try {
            StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE_IMAGES_STORAGE_REF);
            storage.child(singleFeedData.getCreatorID() + PROFILE_IMAGE_NAME).getDownloadUrl().addOnSuccessListener(uri -> Picasso.get().load(uri).placeholder(R.drawable.profile_image_placeholder).into(holder.feedImage)).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting Profile Image and setting Image. (" + e.getLocalizedMessage() + ")");
            });
        } catch (Exception ex) {
            Log.e(TAG, "Error getting Profile Image and setting Image");
        }


        holder.username.setText(singleFeedData.getUsername());
        holder.info.setText(singleFeedData.getInfo());
        holder.date.setText(singleFeedData.getDate());
        holder.noRecordings.setText(singleFeedData.getNoRecordings());
        holder.noLikes.setText(singleFeedData.getNoLikes());

        usersLikedMessagesRef = FirebaseDatabase.getInstance().getReference().child(USERS_LIKED_MESSAGES_REF).child(currentUserInfo.getUniqueID());

        usersLikedMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(singleFeedData.getUniqueID())) {
                    holder.likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_liked_orange);
                    holder.noLikes.setTextColor(resources.getColor(R.color.colorAccent));
                }
                else {
                    holder.likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_blue);
                    holder.noLikes.setTextColor(resources.getColor(R.color.colorPrimary));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        holder.playBtn.setOnClickListener(view -> {
            if(mediaPlayer != null && playingFeed != singleFeedData) {
                Toast.makeText(context, "Already playing a story", Toast.LENGTH_SHORT).show();
                return;
            }
            if(holder.playBtn.getTag().equals(R.drawable.ic_play_media_blue) && playingFeed != singleFeedData) {
                playingFeed = singleFeedData;
                startPlaying(singleFeedData.getAudioRef(), holder.progressBar);
                holder.playBtn.setBackgroundResource(R.drawable.ic_stop_blue);
                holder.playBtn.setTag(R.drawable.ic_stop_blue);
                changeProgressBar(holder.progressBar, holder.playBtn);
            } else {
                stopPlaying(holder.progressBar);
                holder.playBtn.setBackgroundResource(R.drawable.ic_play_media_blue);
                holder.playBtn.setTag(R.drawable.ic_play_media_blue);
            }
        });

        holder.likeBtn.setOnClickListener(view -> {
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
                                    holder.likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_blue);
                                    holder.likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_blue);
                                    holder.noLikes.setTextColor(resources.getColor(R.color.colorPrimary));
                                    usersLikedMessagesRef.child(singleFeedData.getUniqueID()).removeValue();
                                    String likesStr = ds.getValue(String.class);
                                    int noLikes = Integer.valueOf(likesStr);
                                    noLikes--;
                                    String newNoLikes = ""+ noLikes;
                                    numberOfLikesRef.setValue(newNoLikes);
                                    processLike = false;
                                } else {
                                    holder.likeBtn.setBackgroundResource(R.drawable.ic_thumb_up_liked_orange);
                                    holder.noLikes.setTextColor(resources.getColor(R.color.colorAccent));
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

        holder.customRow.setOnClickListener(view -> {
            Feed pressedFeed = feedList.get(position);
            Intent eventInfo = new Intent(context,SingleFeed.class);

            eventInfo.putExtra("pressedFeed",pressedFeed);
            eventInfo.putExtra("userInfo",currentUserInfo);
            context.startActivity(eventInfo);
        });

    }

    @Override
    public int getItemCount() {
        return feedList.size();
    }

    private void startPlaying(String url, ProgressBar progressBar) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
            progressBar.setMax(mediaPlayer.getDuration());
            seekBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
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
            runnable = new Runnable() {
                @Override
                public void run() {
                    changeProgressBar(progressBar, playBtn);
                }
            };
            handler.postDelayed(runnable,1000);
        } else {
            stopPlaying(progressBar);
            playBtn.setBackgroundResource(R.drawable.ic_play_media_blue);
            playBtn.setTag(R.drawable.ic_play_media_blue);
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        RelativeLayout customRow;
        TextView username, info, date, noRecordings, noLikes;
        ImageButton playBtn, likeBtn, recordBtn;
        CircleImageView feedImage;
        ProgressBar progressBar;


        public ViewHolder(View itemView) {
            super(itemView);
            customRow = itemView.findViewById(R.id.customRowLayout_id);
            username = itemView.findViewById(R.id.feed_username_id);
            info = itemView.findViewById(R.id.feed_info_id);
            date = itemView.findViewById(R.id.feed_date_id);
            noRecordings = itemView.findViewById(R.id.noRecordings_id);
            noLikes = itemView.findViewById(R.id.noLikes_id);
            playBtn = itemView.findViewById(R.id.play_button_id);
            likeBtn = itemView.findViewById(R.id.like_button_id);
            recordBtn = itemView.findViewById(R.id.addRecording_button_id);
            feedImage = itemView.findViewById(R.id.feed_image_id);
            progressBar = itemView.findViewById(R.id.feed_audio_progressbar_id);

            handler = new Handler();
            seekBar = new SeekBar(context);
            seekBar.setVisibility(View.INVISIBLE);
            playBtn.setTag(R.drawable.ic_play_media_blue);

        }
    }
}
