package com.ahmedjeylani.audiostories.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ahmedjeylani.audiostories.Models.ChatData;
import com.ahmedjeylani.audiostories.R;

import java.util.ArrayList;

public class CustomMessageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<ChatData> chatList;
    private String currentUser;
    private static final int CURRENT_USER = 0;
    private static final int OTHER_USER = 1;

    public CustomMessageAdapter(Context c, ArrayList<ChatData> cd, String currentUser) {
        this.context = c;
        this.chatList = cd;
        this.currentUser = currentUser;
    }

    @Override
    public int getCount() {
        return chatList.size();
    }

    @Override
    public Object getItem(int position) {
        return chatList.get(position);
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

        final ChatData singleChatData = (ChatData) this.getItem(position);

        if (convertView == null) {


            if(getItemViewType(position) == CURRENT_USER) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_chat_currentuser, parent, false);
            } else if(getItemViewType(position) == OTHER_USER) {

                convertView = LayoutInflater.from(context).inflate(R.layout.item_chat_otheruser, parent, false);
            }

        }

        if(getItemViewType(position) == CURRENT_USER) {
            TextView userMessage = convertView.findViewById(R.id.currentuser_chatmsg_id);
            TextView userTime = convertView.findViewById(R.id.currentuser_chatdate_id);
            userMessage.setText(singleChatData.getMessage());
            userTime.setText(singleChatData.getMessageTime());


        } else if(getItemViewType(position) == OTHER_USER) {

            TextView userUN = convertView.findViewById(R.id.otheruser_chatusername_id);
            TextView userMessage = convertView.findViewById(R.id.otheruser_chatmessage_id);
            TextView userTime = convertView.findViewById(R.id.otheruser_chatdate_id);

            userUN.setText(singleChatData.getUsername());
            userMessage.setText(singleChatData.getMessage());
            userTime.setText(singleChatData.getMessageTime());

        }

        notifyDataSetChanged();
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {

        ChatData singleChatData = chatList.get(position);

        if(currentUser.equalsIgnoreCase(singleChatData.getUsername())) {
            return CURRENT_USER;
        } else {
            return OTHER_USER;
        }
    }
    //Checking a certain attribute
    @Override
    public int getViewTypeCount() {
        return 2;
    }

}