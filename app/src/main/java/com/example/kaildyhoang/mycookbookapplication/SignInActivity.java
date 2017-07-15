
package com.example.kaildyhoang.mycookbookapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kaildyhoang.mycookbookapplication.models.User;
import com.example.kaildyhoang.mycookbookapplication.view.MainActivity_View;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText _edtTEmail, _edtTPassword;
    private ProgressDialog progressDialog;
    private ImageView _imgVLogo;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private LoginButton loginButton;
    private String _dateNow;
    private DatabaseReference mDatabaseRef;
    private CallbackManager callbackManager;
    private static final String TAG = "SignInActivityWithEMAI";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_sign_in);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton)findViewById(R.id.buttonSignInFacebookSI);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
           @Override
           public void onSuccess(LoginResult loginResult) {
              Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
           }

           @Override
           public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
           }

            @Override
           public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
       });
        //EditText
        _edtTEmail = (EditText) findViewById(R.id.editTextEmailSI);
        _edtTPassword = (EditText) findViewById(R.id.editTextPasswordSI);

        //Button
        findViewById(R.id.buttonSignUpSI).setOnClickListener(this);
        findViewById(R.id.buttonSignInSI).setOnClickListener(this);
        findViewById(R.id.buttonSignInFacebookSI).setOnClickListener(this);
        findViewById(R.id.buttonSignInGmailSI).setOnClickListener(this);

        //ImageView
        _imgVLogo = (ImageView) findViewById(R.id.imageViewLogo);
        //Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
              FirebaseUser user = mAuth.getCurrentUser();
               if (user != null){
                   updateUI();
                   //goMainScreen();
             }

           }
        };
        //Initialize the progressDialog
        progressDialog = new ProgressDialog(this);
    }
    private void goMainScreen() {

        Intent intent = new Intent(this,MainActivity_View.class);
        startActivity(intent);
    }


    private void handleFacebookAccessToken(AccessToken accessToken) {
        Log.d(TAG, "handleFacebookAccessToken:" + accessToken);
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(TAG, "signInWithCredential:success: " + task.isSuccessful());
                updateUI();
                if (!task.isSuccessful()){
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    Toast.makeText(SignInActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }


    @Override
    protected void onStop() {
        super.onStop();
            mAuth.removeAuthStateListener(mAuthListener);
    }
  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    private void updateUI() {
        hideProgressDialog();

        FirebaseUser userfb = mAuth.getCurrentUser();
        if (userfb != null) {
            _dateNow = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())+"";
            User user = new User(
                    userfb.getDisplayName(),
                    userfb.getEmail(),
                    userfb.getPhotoUrl().toString(),
                    _dateNow
            );
            mDatabaseRef.child("users").child(userfb.getUid()).setValue(user);
            goMainScreen();
        } else {
            Toast.makeText(this, null, Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onClick(View v){
        int i = v.getId();
        if(i == R.id.buttonSignInSI){
            doSignIn(_edtTEmail.getText().toString(),_edtTPassword.getText().toString());
        }else if(i == R.id.buttonSignUpSI){
            startActivity(new Intent(this,SignUpActivity.class));
        }else if(i == R.id.buttonSignInFacebookSI){

        }else if(i == R.id.buttonSignInGmailSI){

        }
    }

    private void doSignIn(String email,String password){
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressDialog();

        //[Start Sign in]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG,"SignInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            Toast.makeText(SignInActivity.this,"Welcome." + user.getEmail(),Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(),MainActivity_View.class));
                        }else {
                            Log.w(TAG,"SignInWithEmail:failure",task.getException());
                            Toast.makeText(SignInActivity.this,"SignInWithEmail failed.",Toast.LENGTH_SHORT).show();
                        }
                        if(!task.isSuccessful()){
                            Toast.makeText(SignInActivity.this,"Account not be exist, Please create new account.",Toast.LENGTH_SHORT).show();
                        }

                        hideProgressDialog();
                    }
                });
    }



    private boolean validateForm (){
        boolean valid = true;

        String email = _edtTEmail.getText().toString();
        if(TextUtils.isEmpty(email)){
            _edtTEmail.setError("Require.");
            valid = false;
        }else{
            _edtTEmail.setError(null);
        }

        String password = _edtTPassword.getText().toString();
        if(TextUtils.isEmpty(password)){
            _edtTPassword.setError("Require.");
            valid = false;
        }else{
            _edtTPassword.setError(null);
        }

        return  valid;
    }


    private void showProgressDialog(){
        progressDialog.setMessage("Waiting...");
        progressDialog.show();
    }
    private void hideProgressDialog(){
        progressDialog.dismiss();
    }
}
