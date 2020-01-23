package com.example.chatapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity {

    //Fire base database reference
    private DatabaseReference mUserDatabase;
    private DatabaseReference mUserRef;
    //Fire base Current user
    private FirebaseUser mCurrentUser;
    // Fire base storage reference
    private StorageReference mImageStorage;
    // Progress Dialog
    private ProgressDialog mProgressDialog;

    // Download URL for image and thumb_nail
    private String thumb_nail_download_url;
    private String download_url;

    //Android Layout
    private CircleImageView mProfileImage;
    private Button mChangeImage;
    private Button mChangeStatus;
    private TextView mDisplayName;
    private TextView mStatus;
    private TextView mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mProfileImage = findViewById(R.id.settings_image);
        mChangeImage = findViewById(R.id.settings_change_image_button);
        mChangeStatus = findViewById(R.id.settings_change_status_button);
        mDisplayName = findViewById(R.id.settings_displayName_text);
        mStatus = findViewById(R.id.settings_status_text);
        mEmail = findViewById(R.id.settings_email_text);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String current_uid = mCurrentUser.getUid();
        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase.keepSynced(true);

        // Fire base set up storage
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                String email = dataSnapshot.child("email").getValue().toString();

                mDisplayName.setText(name);
                mStatus.setText(status);
                mEmail.setText(email);
                if(!image.equals("default")) {
                    //Load the image into the image viewer
                    //

                    Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.avatar).into(mProfileImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(image).placeholder(R.drawable.avatar).into(mProfileImage);
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mChangeStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the value of status
                String statusValue = mStatus.getText().toString();
                Intent changeStatusIntent = new Intent(SettingsActivity.this, StatusActivity.class);
                // Send the value of status along with the intent
                changeStatusIntent.putExtra("status_value", statusValue);
                startActivity(changeStatusIntent);
            }
        });

        mChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
                    .setAspectRatio(1, 1)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(SettingsActivity.this);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUserRef.child("online").setValue(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                //Progress Dialog
                mProgressDialog = new ProgressDialog(SettingsActivity.this);
                mProgressDialog.setTitle("Uploading Profile Photo");
                mProgressDialog.setMessage("Please wait while we upload your profile photo");
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.show();

                Uri resultUri = result.getUri();
                final String current_uid = mCurrentUser.getUid();

                // Compressing image to a thumb_nail
                File image_path = new File(resultUri.getPath());
                Bitmap thumb_nial_bitmap = null;
                try {
                    thumb_nial_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(500)
                            .setQuality(75)
                            .compressToBitmap(image_path);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Uploading Thumb_nail to fire-base storage
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_nial_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] thumb_nail_byte = baos.toByteArray();

                final StorageReference filePath = mImageStorage.child("profile_images").child( current_uid + ".jpeg");
                final StorageReference thumb_nailPath = mImageStorage.child("profile_images").child("thumb_nails").child( current_uid + ".jpeg");

                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                            download_url = uri.toString();

                            // Uploading Thumb_nail to fire-base storage
                            UploadTask uploadTask = thumb_nailPath.putBytes(thumb_nail_byte);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {
                                    if(thumb_task.isSuccessful()) {

                                        thumb_nailPath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                thumb_nail_download_url = uri.toString();

                                                Map update_hashMap = new HashMap();
                                                update_hashMap.put("thumb_image", thumb_nail_download_url);
                                                update_hashMap.put("image", download_url);


                                                mUserDatabase.updateChildren(update_hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()) {
                                                            mProgressDialog.dismiss();
                                                            Toast.makeText(SettingsActivity.this, "Successfully uploaded.", Toast.LENGTH_LONG).show();
                                                        } else {
                                                            mProgressDialog.dismiss();
                                                            Toast.makeText(SettingsActivity.this, "Unknown Error occurred while uploading", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        mProgressDialog.dismiss();
                                        Toast.makeText(SettingsActivity.this, "Error in uploading thumb nail", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                            }
                        });
                    } else {
                        mProgressDialog.dismiss();
                        Toast.makeText(SettingsActivity.this, "Error in uploading", Toast.LENGTH_LONG).show();
                    }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(SettingsActivity.this, "Error: "+ error, Toast.LENGTH_LONG).show();
            }

        }
    }
}
