package com.ahmedjeylani.audiostories;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedjeylani.audiostories.ListAdapters.CustomRecycleViewAdapter;
import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.AUDIO_STORY_STORAGE_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.FEED_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.STORIES_REF;

public class ProfileActivity extends AppCompatActivity {

    private ArrayList<Feed> feedList = new ArrayList<>();
    private RecyclerView recyclerView;

    private Feed pressedFeed;
    private DatabaseReference databaseRef;
    private CustomRecycleViewAdapter adapter;
    private BaseUser userInfo;
    private CircleImageView panelProfileImage;
    private RelativeLayout emptyState;
    private ImageButton addBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check in Firebase if user has stories posts
        // if user has stories posted show profile activity with stories
        setContentView(R.layout.activity_profile);
        //else show empty profile activity page
        //setContentView(R.layout.activity_profile_empty);

        getSupportActionBar().setTitle(R.string.profile_activity);

        recyclerView = findViewById(R.id.feed_list__id);
        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userInfo = UserCache.loadCache();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        emptyState = findViewById(R.id.empty_state_id);
        panelProfileImage = findViewById(R.id.show_profile_image_id);
        TextView panelUsername = findViewById(R.id.nav_username_id);
        TextView panelEmail = findViewById(R.id.nav_email_id);
        TextView createAudioText = findViewById(R.id.create_audio_text_id);

        if(currentUser != null) panelEmail.setText(currentUser.getEmail());
        Utilities.loadImage(userInfo, panelProfileImage);
        panelUsername.setText(userInfo.getUsername());

        addBtn = findViewById(R.id.addBtn_id);
        addBtn.setOnClickListener(v -> {
            Intent addIntent = new Intent(ProfileActivity.this,CreateAudioStory.class);
            addIntent.putExtra("sentFromHome",true);
            startActivity(addIntent);
        });

        createAudioText.setOnClickListener(v -> {
            Intent addIntent = new Intent(ProfileActivity.this,CreateAudioStory.class);
            addIntent.putExtra("sentFromHome",true);
            startActivity(addIntent);
        });

        Utilities.showEmptyState(feedList, null, recyclerView, emptyState, addBtn);
        databaseRef.child(FEED_REF).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                if(feed.getCreatorID().equals(userInfo.uniqueID)) {
                    feedList.add(0, feed);
                }
                adapter = new CustomRecycleViewAdapter(ProfileActivity.this,feedList, userInfo);
                recyclerView.setAdapter(adapter);
                Utilities.showEmptyState(feedList, null, recyclerView, emptyState, addBtn);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                int index = -1;
                for(Feed item : feedList) {
                    if(item.getUniqueID().equals(feed.getUniqueID()) && feed.getCreatorID().equals(userInfo.getUniqueID())) {
                        index = feedList.indexOf(item);
                    }
                }

                if(index != -1) {
                    feedList.remove(index);
                    feedList.add(index, feed);
                    adapter.notifyItemChanged(index);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                int index = -1;
                for(Feed item : feedList) {
                    if(item.getUniqueID().equals(feed.getUniqueID()) && feed.getCreatorID().equals(userInfo.getUniqueID())) {
                        index = feedList.indexOf(item);
                    }
                }

                if(index != -1) {
                    feedList.remove(index);
                    Utilities.showEmptyState(feedList, null, recyclerView, emptyState, addBtn);
                    adapter.notifyItemRemoved(index);
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    final ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            if (direction == ItemTouchHelper.LEFT) {
                MaterialAlertDialogBuilder alertMsgOnDelete = new MaterialAlertDialogBuilder(ProfileActivity.this);
                alertMsgOnDelete.setTitle("Warning")
                        .setMessage("This will permanently delete this story. Are you sure you want to continue?")
                        .setPositiveButton("Yes", (dialog, id) -> {
                            Feed deleteFeed = feedList.get(position);
                            feedList.remove(position);
                            Utilities.showEmptyState(feedList, null, recyclerView, emptyState, addBtn);
                            adapter.notifyItemRemoved(position);
                            databaseRef.child(FEED_REF).child(deleteFeed.getUniqueID()).setValue(null);
                            databaseRef.child(STORIES_REF).child(deleteFeed.getUniqueID()).setValue(null);
                            StorageReference audioStorageRef = FirebaseStorage.getInstance().getReference().child(AUDIO_STORY_STORAGE_REF);
                            audioStorageRef.child(deleteFeed.getFileName()).delete().addOnSuccessListener(aVoid ->
                                    Toast.makeText(ProfileActivity.this, "Deleted Story", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> {
                            adapter.notifyItemChanged(position);
                        });
                AlertDialog alert = alertMsgOnDelete.create();
                alert.show();
            }

        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(ProfileActivity.this, R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.ic_delete_white_24dp)
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds edit icon to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_profile_menu, menu);
        return true;
    }

    public void clickEditPage(MenuItem m) {
        Intent visitEditPage = new Intent(ProfileActivity.this, EditProfileActivity.class);
        visitEditPage.putExtra("userInfo", userInfo);
        startActivity(visitEditPage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseUser tempUser = UserCache.loadCache();
        if (!(tempUser.getImageRef().equals(userInfo.getImageRef()))) {
            userInfo = UserCache.loadCache();
            Utilities.loadImage(userInfo, panelProfileImage);
            adapter.notifyDataSetChanged();
        }
    }
}
