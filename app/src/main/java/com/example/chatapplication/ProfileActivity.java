package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView mDisplayName;
    private TextView mStatus;
    private TextView mTotalFriends;
    private ImageView mImage;
    private Button mSendFriendRequest;
    private Button mDeclineFriendRequest;
    private Toolbar mToolbar;

    //Fire base database references
    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;
    private DatabaseReference mUserRef;

    //Fire base current logged in user
    private FirebaseUser mCurrentUser;

    //Progress Dialog box
    private ProgressDialog mProgress;

    private String mCurrent_state;
    private  String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mToolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("User Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDisplayName = findViewById(R.id.profile_displayName);
        mStatus = findViewById(R.id.profile_status);
        mTotalFriends = findViewById(R.id.profile_totalFriends);
        mSendFriendRequest = findViewById(R.id.profile_sendFriendRequest);
        mDeclineFriendRequest = findViewById(R.id.profile_declineFriendRequestButton);
        mImage = findViewById(R.id.profile_image);

        mCurrent_state = "not_friends";

        mProgress = new ProgressDialog(ProfileActivity.this);
        mProgress.setTitle("Loading User Data");
        mProgress.setMessage("Please wait while we load the data");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        final String user_id = getIntent().getStringExtra("user_id");
        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("Notifications");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();


        // HIDE THE DECLINE FRIEND REQUEST BUTTON
        mDeclineFriendRequest.setVisibility(View.INVISIBLE);
        mDeclineFriendRequest.setEnabled(false);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                // String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();

                mDisplayName.setText(name);
                mStatus.setText(status);
                Picasso.get().load(image).placeholder(R.drawable.avatar).into(mImage);


                // -------------- FRIENDS LIST / REQUEST FEATURE ---------------- //
                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(user_id)) {
                            String request_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();
                            if (request_type.equals("received")){
                                mCurrent_state = "req_received";
                                mSendFriendRequest.setText("ACCEPT FRIEND REQUEST");
                                mDeclineFriendRequest.setVisibility(View.VISIBLE);
                                mDeclineFriendRequest.setEnabled(true);
                            } else if (request_type.equals("sent")) {
                                mCurrent_state = "req_sent";
                                mSendFriendRequest.setText("CANCEL FRIEND REQUEST");
//                                mSendFriendRequest.setBackgroundColor(R.color.colorRed);
                            }
                            mProgress.dismiss();
                        } else {

                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(user_id)){
                                        mCurrent_state = "friends";
                                        mSendFriendRequest.setText("UN FRIEND " + name);
                                    }
                                    mProgress.dismiss();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    mProgress.dismiss();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mSendFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            mSendFriendRequest.setEnabled(false);
            // -------------------- NOT FRIENDS STATE -------------------- //
            if(mCurrent_state.equals("not_friends")) {
                mProgress.setTitle("Send Friend Request");
                mProgress.setMessage("Please wait while we send the friend request");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                DatabaseReference newNotificationRef = mRootRef.child("Notifications").child(user_id).push();
                String newNotificationId = newNotificationRef.getKey();

                HashMap<String, String> notificationData = new HashMap<>();
                notificationData.put("from", mCurrentUser.getUid());
                notificationData.put("type", "request");

               Map requestMap = new HashMap();
               requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/" + "request_type", "sent");
               requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid() + "/" + "request_type", "received");
               requestMap.put("Notifications/" + user_id + "/" + newNotificationId, notificationData);

               mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                   @Override
                   public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError != null) {
                        Toast.makeText(ProfileActivity.this, "Failed sending request", Toast.LENGTH_LONG).show();
                    }
                   mCurrent_state = "req_sent";
                   mSendFriendRequest.setText("CANCEL FRIEND REQUEST");
                   mProgress.dismiss();
                   Toast.makeText(ProfileActivity.this, "Friend Request Sent", Toast.LENGTH_SHORT).show();

                   }
               });
            }

            // -------------------- CANCEL FRIEND REQUEST STATE -------------------- //
            if(mCurrent_state.equals("req_sent")) {
                mProgress.setTitle("Cancel Friend Request");
                mProgress.setMessage("Please wait while we cancel the friend request");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                Map notificationMap = new HashMap();
                notificationMap.put("Notifications/" + user_id + "/", null);
                notificationMap.put("Friend_req/" + mCurrentUser.getUid() + "/", null);
                notificationMap.put("Friend_req/" + user_id + "/", null);

                mRootRef.updateChildren(notificationMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError == null) {
                        mCurrent_state = "not_friends";
                        mSendFriendRequest.setText("SEND FRIEND REQUEST");
                        mSendFriendRequest.setEnabled(true);
                        mProgress.dismiss();
                        Toast.makeText(ProfileActivity.this, "Friend Request Cancelled", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(ProfileActivity.this, "Error: Friend request was not cancelled", Toast.LENGTH_SHORT)
                            .show();
                    }
                    }
                });
            }

            // -------------------- REQ RECEIVED STATE -------------------- //
            if(mCurrent_state.equals("req_received")) {

                mProgress.setTitle("Accept Friend Request");
                mProgress.setMessage("Please wait while we accept the friend request");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                final String currentDate = DateFormat.getDateInstance().format(new Date());

                Map friendsMap = new HashMap();
                friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/date", currentDate);
                friendsMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/date", currentDate);

                friendsMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                friendsMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid(), null);

                mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError == null) {
                        mProgress.dismiss();
                        Toast.makeText(ProfileActivity.this,
                                "Friend Request Accepted", Toast.LENGTH_SHORT).show();
                        mCurrent_state = "friends";
                        mSendFriendRequest.setText("UN FRIEND " + name);
                        mSendFriendRequest.setEnabled(true);
                    }
                    else {
                        mProgress.dismiss();
                        Toast.makeText(ProfileActivity.this,
                                "Error: Failed accepting friend request", Toast.LENGTH_SHORT).show();
                    }
                    }
                });
            }

            // -------------------- UN FRIENDS STATE -------------------- //
            if(mCurrent_state.equals("friends"))  {
                    mProgress.setTitle("Un Friend " + name);
                    mProgress.setMessage("Please wait while we un friend " + name);
                    mProgress.setCanceledOnTouchOutside(false);
                    mProgress.show();

//                    Map friendsMap = new HashMap();
//                    friendsMap.put("Friends/", mCurrentUser.getUid());

                    mFriendDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mFriendDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                        mCurrent_state = "not_friends";
                                        mSendFriendRequest.setText("SEND FRIEND REQUEST");
                                        mSendFriendRequest.setEnabled(true);
                                        mProgress.dismiss();
                                        Toast.makeText(ProfileActivity.this, "Un friend " + name + " successful",
                                            Toast.LENGTH_SHORT).show();
                                        }
                                    });
                            }
                        });
                }
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        mUserRef.child("online").setValue(false);
    }
}
