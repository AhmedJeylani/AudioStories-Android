package com.ahmedjeylani.audiostories;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.ahmedjeylani.audiostories.ListAdapters.CustomMessageAdapter;
import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.ChatData;
import com.ahmedjeylani.audiostories.Services.UserCache;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.CHATROOM_REF;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.IMAGE_REF_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.MESSAGE_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.MESSAGE_TIME_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.UNIQUEID_KEY_NAME;
import static com.ahmedjeylani.audiostories.Database.DatabaseStringReferences.USERNAME_KEY_NAME;


public class ChatroomActivity extends AppCompatActivity {

    private EditText inputMessage;
    private TextView chatConvo;

    private String tempKey;
    private String un;

    private DatabaseReference messageRef, chatRoomRef;//, chatroomUsersRef;
    private BaseUser userInfo;

    private ArrayList<ChatData> chatList = new ArrayList<>();
    private ListView customChatList;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        userInfo = UserCache.loadCache();
        un = userInfo.getUsername();

        customChatList = findViewById(R.id.chat_list_id);
        setTitle("Chatroom");


        ImageButton sendMessageBtn = findViewById(R.id.send_message_button_id);
        inputMessage = findViewById(R.id.user_msg_id);

        chatRoomRef = FirebaseDatabase.getInstance().getReference().child(CHATROOM_REF);

        sendMessageBtn.setOnClickListener(v -> {

            if(!TextUtils.isEmpty(inputMessage.getText().toString())) {
                //This is for the random keys

                Date currentDate = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy,  HH:mm", Locale.ENGLISH);
                String currentDateTimeString = simpleDateFormat.format(currentDate);
                tempKey = chatRoomRef.push().getKey();
                //TODO CHECK CODE THAT IS COMMENTED BELOW AND ABOVE!!!
                //This will put all the previous chats in the map?????
                //chatRoomRef.updateChildren(map);

                messageRef = chatRoomRef.child(tempKey);

                Map<String,Object> map2 = new HashMap<String, Object>();
                map2.put(UNIQUEID_KEY_NAME,userInfo.getUniqueID());
                map2.put(USERNAME_KEY_NAME,un);
                map2.put(MESSAGE_KEY_NAME,inputMessage.getText().toString());
                map2.put(MESSAGE_TIME_KEY_NAME,currentDateTimeString);
                map2.put(IMAGE_REF_KEY_NAME,userInfo.getImageRef());


                messageRef.updateChildren(map2);
                inputMessage.getText().clear();

            }


        });


        chatRoomRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                updateChatConvo(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                updateChatConvo(dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void updateChatConvo(DataSnapshot ds) {

        Iterator i = ds.getChildren().iterator();

        while(i.hasNext()) {

            //i.next gets the first child value then the next child value
            //i.hasNext Loops till there are no more next child values!
            String chatImgRef = (String)((DataSnapshot)i.next()).getValue(); //IMG Ref
            String chatMsg = (String) ((DataSnapshot) i.next()).getValue();
            String chatDateTime = (String)((DataSnapshot)i.next()).getValue(); //Date and time
            String chatUID = (String) ((DataSnapshot) i.next()).getValue();
            String chatUN = (String) ((DataSnapshot) i.next()).getValue();

            Log.d("D-----------------", "UID" + chatUID);
            Log.d("D-----------------", "UN" + chatUN);
            Log.d("D-----------------", "MSG"+ chatMsg);
            Log.d("D-----------------", "Date" + chatDateTime);
            Log.d("D-----------------", "IMG Ref" + chatImgRef);


            ChatData userMessage = new ChatData(chatUID, chatUN, chatMsg,chatDateTime,chatImgRef);
            chatList.add(userMessage);

            CustomMessageAdapter adapter = new CustomMessageAdapter(ChatroomActivity.this,chatList,un);
            customChatList.setAdapter(adapter);

            //This puts the string together into one
            //chatConvo.append(chatUN+" : "+chatMsg+" \n");



        }
    }

}
