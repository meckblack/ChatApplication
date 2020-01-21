package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import org.w3c.dom.Text;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mCreateAccountBtn;
    private FirebaseAuth mAuth;
    private Toolbar mToolbar;
    private ProgressDialog mRegProgress;
    private DatabaseReference mDatabase;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        mToolbar = findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRegProgress = new ProgressDialog(this);

        // Fire Base Authentication Instance
        mAuth = FirebaseAuth.getInstance();

        mDisplayName = findViewById(R.id.reg_display_name);
        mEmail = findViewById(R.id.login_email);
        mPassword = findViewById(R.id.login_password);
        mCreateAccountBtn = findViewById(R.id.login_button);

        mCreateAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            String display_name = mDisplayName.getEditText().getText().toString();
            String email = mEmail.getEditText().getText().toString();
            String password = mPassword.getEditText().getText().toString();

            if(!TextUtils.isEmpty(display_name) || !TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)){
                mRegProgress.setTitle("Registering User");
                mRegProgress.setMessage("Please wait while we create your account!!!");
                mRegProgress.setCanceledOnTouchOutside(false);
                mRegProgress.show();
                register_user(display_name, email, password);
            } else {
                Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_LONG).show();
            }

            }
        });
    }

    private void register_user(final String display_name, final String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                final String uId = current_user.getUid();
                mDatabase =  FirebaseDatabase.getInstance().getReference().child("Users").child(uId);
                String device_token = FirebaseInstanceId.getInstance().getToken();

                HashMap<String, String> userMap = new HashMap<>();
                userMap.put("name", display_name);
                userMap.put("status", "hi there I am using Chat application.");
                userMap.put("image", "default");
                userMap.put("email", email);
                userMap.put("thumb_image", "default");
                userMap.put("device_token", device_token);


                mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        mRegProgress.dismiss();
                        Log.d("Sign in Error", "createUserWithEmail:success");
                        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainIntent);
                        finish();
                    }
                    }
                });

            } else {
                mRegProgress.hide();
                // If registration fails, display a message to the user.
                Log.w("Sign in Error", "createUserWithEmail:failure", task.getException());
                String error;
                try{
                    throw task.getException();

                } catch (FirebaseAuthWeakPasswordException e){
                    error = "Registration Failed: Password must be more than 6 characters";
                } catch (FirebaseAuthInvalidCredentialsException e){
                    error = "Registration Failed: Invalid email";
                } catch (FirebaseAuthUserCollisionException e){
                    error = "Registration Failed: Email already exists";
                } catch (Exception e) {
                    error = "Registration Failed: Unknown error";
                    e.printStackTrace();
                }
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
            }
        });
    }
}
