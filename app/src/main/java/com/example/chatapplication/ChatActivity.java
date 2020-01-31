package com.example.chatapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.annotations.NonNull;

public class ChatActivity extends AppCompatActivity {

    private String mChatUser;
    private Toolbar mChatToolbar;
    private DatabaseReference mRootRef;
    private TextView mChatDisplayName;
    private TextView mChatLastSeenView;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;
    private CircleImageView mChatProfileImage;

    private ImageButton mChatAddBtn;
    private ImageButton mChatSendBtn;
    private EditText mChatMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatToolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = inflater.inflate(R.layout.chat_app_bar, null);
        actionBar.setCustomView(actionBarView);

        mChatLastSeenView = findViewById(R.id.chat_app_bar_online);
        mChatDisplayName = findViewById(R.id.chat_app_bar_display_name);
        mChatProfileImage = findViewById(R.id.chat_app_bar_image);

        mChatAddBtn = findViewById(R.id.chat_add_button);
        mChatSendBtn = findViewById(R.id.chat_send_button);
        mChatMessageView = findViewById(R.id.chat_Message);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mChatUser = getIntent().getStringExtra("user_id");
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        String user_name = getIntent().getStringExtra("user_name");

        // SETS THE DISPLAY NAME, LAST SEEN AND IMAGE
        mChatDisplayName.setText(user_name);
        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("image").toString();

                if (online.equals("true")) {
                    mChatLastSeenView.setText("Online");
                } else {
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastTime = Long.parseLong(online);
                    String lastSeenTime = getTimeAgo.getTimeAgo(lastTime, getApplicationContext());
                    mChatLastSeenView.setText(lastSeenTime);
                    Picasso.get().load(image).placeholder(R.drawable.avatar).into(mChatProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        // END OF ACTION

        // THIS CREATES THE CHAT LOG IN THE DATABASE
        mRootRef.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(mChatUser)) {
                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);
                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUserId, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError databaseError) {

            }
        });
        // END OF ACTION

        // WHAT HAPPENS WHEN YOU CLICK ON THE SEND ICON BUTTON.
        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        // END OF ACTION


    }

    private void sendMessage() {
        String message = mChatMessageView.getText().toString();

        if (!TextUtils.isEmpty(message)) {
            String current_user_ref = "messages/" + mCurrentUserId + "/" + mChatUser;
            String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mRootRef.child("messages").child(mCurrentUserId)
                    .child(mChatUser).push();
            String push_id = user_message_push.getKey();
            Map messageMap = new HashMap();
            messageMap.put("messages", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                }
                }
            });
        }
    }
}
