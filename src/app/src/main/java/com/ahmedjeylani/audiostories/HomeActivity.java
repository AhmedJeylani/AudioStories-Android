package com.ahmedjeylani.audiostories;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ahmedjeylani.audiostories.ListAdapters.CustomRecycleViewAdapter;
import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.FEED_REF;

public class HomeActivity extends AppCompatActivity {

    private ArrayList<Feed> feedList = new ArrayList<>();
    private RecyclerView recyclerView;
    private Feed pressedFeed;
    private AlertDialog.Builder builder;

    private DatabaseReference databaseRef;
    private CustomRecycleViewAdapter adapter;
    private BaseUser userInfo;
    private CircleImageView navProfileImage;
    private RelativeLayout emptyState;
    private ImageButton addBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //feedListView = (ListView) findViewById(R.id.feed_list__id);
        recyclerView = findViewById(R.id.feed_list__id);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        userInfo = UserCache.loadCache();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = findViewById(R.id.nav_view_id);

        View headerView = navView.getHeaderView(0);
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            switch (id) {
                //User Profile Page
                case R.id.nav_profileBtn_id:
                    Intent profileIntent = new Intent(HomeActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                    return true;

                //Sign Out Btn
                case R.id.nav_signoutBtn_id:
                    FirebaseAuth.getInstance().signOut();
                    finish();
                    Intent signoutIntent = new Intent(HomeActivity.this, LoginActivity.class);
                    startActivity(signoutIntent);
                    return true;

                case R.id.nav_chatroomBtn_id:
                    Intent chatroomIntent = new Intent(HomeActivity.this, ChatroomActivity.class);
                    startActivity(chatroomIntent);
                    return true;

                default:
            }
            return HomeActivity.super.onOptionsItemSelected(item);
        });

        navProfileImage = headerView.findViewById(R.id.nav_profileImage_id);
        TextView navUsername = headerView.findViewById(R.id.nav_username_id);
        TextView navEmail = headerView.findViewById(R.id.nav_email_id);
        TextView createAudioText = findViewById(R.id.create_audio_text_id);
        emptyState = findViewById(R.id.empty_state_id);


        if(currentUser != null) navEmail.setText(currentUser.getEmail());
        Utilities.loadImage(userInfo, navProfileImage);
        navUsername.setText(userInfo.getUsername());

        addBtn = findViewById(R.id.addBtn_id);
        addBtn.setOnClickListener(v -> {
            Intent addIntent = new Intent(HomeActivity.this,CreateAudioStory.class);
            addIntent.putExtra("sentFromHome",true);
            startActivity(addIntent);
        });

        createAudioText.setOnClickListener(v -> {
            Intent addIntent = new Intent(HomeActivity.this,CreateAudioStory.class);
            addIntent.putExtra("sentFromHome",true);
            startActivity(addIntent);
        });

        Utilities.showEmptyState(feedList, null, recyclerView, emptyState, addBtn);
        databaseRef.child(FEED_REF).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                feedList.add(0, feed);

                adapter = new CustomRecycleViewAdapter(HomeActivity.this,feedList, userInfo);
                recyclerView.setAdapter(adapter);
                Utilities.showEmptyState(feedList, null, recyclerView, emptyState, addBtn);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Feed feed = dataSnapshot.getValue(Feed.class);
                int index = -1;
                for(Feed item : feedList) {
                    if(item.getUniqueID().equals(feed.getUniqueID())) {
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
                    if(item.getUniqueID().equals(feed.getUniqueID())) {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseUser tempUser = UserCache.loadCache();
        if (!(tempUser.getImageRef().equals(userInfo.getImageRef()))) {
            userInfo = UserCache.loadCache();
            Utilities.loadImage(userInfo, navProfileImage);
            adapter.notifyDataSetChanged();
        }
    }
}
