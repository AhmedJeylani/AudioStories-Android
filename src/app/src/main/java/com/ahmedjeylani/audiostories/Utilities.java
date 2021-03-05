package com.ahmedjeylani.audiostories;

import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.ahmedjeylani.audiostories.Models.BaseUser;
import com.ahmedjeylani.audiostories.Models.Feed;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class Utilities {
    //This makes the child of the event and its value for example "Location = Uxbridge"
    public static void addChildAndValue(DatabaseReference ref, String child, String value) {
        ref.getRef().child(child).setValue(value);
    }

    public static boolean isFieldEmpty(EditText editText) {
        return TextUtils.isEmpty(editText.getText().toString());
    }

    public static String getCurrentDateAndTimeFeed() {
        Date currentDate = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yy", Locale.ENGLISH);

        return simpleDateFormat.format(currentDate);
    }

    public static String getCurrentDateAndTimeFile() {
        Date currentDate = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM_dd_yy-HHmmss", Locale.ENGLISH);

        return simpleDateFormat.format(currentDate);
    }

    public static boolean validateEmail(EditText emailInput) {
        String userEmail = emailInput.getText().toString();
        if (isFieldEmpty(emailInput)) {
            emailInput.setError("Email can't be empty");
            emailInput.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return false;
        } else {
            emailInput.setError(null);
            return true;
        }
    }

    public static void loadImage(BaseUser userInfo, CircleImageView profileImage) {
        if (userInfo.getImageRef() != null && !TextUtils.isEmpty(userInfo.getImageRef())) {
            Picasso.get().load(userInfo.getImageRef()).placeholder(R.drawable.profile_image_placeholder).into(profileImage);
        }
    }
    
    public static void showEmptyState(ArrayList<Feed> list, ListView listView, RecyclerView recyclerView, RelativeLayout emptyState, ImageButton addBtn) {
        if(list.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            addBtn.setVisibility(View.INVISIBLE);

            if (listView != null)
                listView.setVisibility(View.INVISIBLE);
            else
                recyclerView.setVisibility(View.INVISIBLE);

        } else {
            emptyState.setVisibility(View.INVISIBLE);
            addBtn.setVisibility(View.VISIBLE);

            if (listView != null)
                listView.setVisibility(View.VISIBLE);
            else
                recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
