package org.androidtown.shaketest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    // [START declare_auth]
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseAuth.AuthStateListener mListener;
    // [END declare_auth]
    private GoogleSignInClient mGoogleSignInClient;
    private DatabaseReference mDatabase;
    private SharedPrefManager mSharedPrefManager;
    private String displayUserPhoneNumber;

    // SCREEN_ON_OFF_BROADCAST_RECEIVER
    private BroadCastManager mBroadCastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("메인엑티비티", "접근");


        getPermission();
        mSharedPrefManager = SharedPrefManager.getInstance(this);
        Log.d("MainActivity", "메인엑티비티");
        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
        mListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.d("MainActivity", "구글 인증");
                mUser = mAuth.getCurrentUser();
                if (mUser != null) {
                    mDatabase = FirebaseDatabase.getInstance().getReference().child("myInfo").child(mUser.getUid());
                    if (!mSharedPrefManager.getCheckFirst()) {

                        /* 첫 로그인 */

                        ContactData userData = new ContactData(
                                mUser.getDisplayName(),
                                getPhoneNum(),
                                mUser.getEmail(),
                                1,
                                null
                        );
                        mSharedPrefManager.setUserData(mUser.getUid(), userData);
                        //mDatabase.setValue(mSharedPrefManager.getUserData());
                    }

                    setMyContactList();
                    Log.d("MyReceiver", "메인 엑티비티 온크리트 ");
                    if (mSharedPrefManager.getServiceCheck()) {
                        Log.d("MyReceiver", "동적 등록");
                        startService(new Intent(getApplicationContext(), ShakeService.class));
                        mBroadCastManager = BroadCastManager.getInstance(getApplicationContext());
                    }

                    if (mSharedPrefManager.getCheckFirst() == true)
                        startActivity(new Intent(getApplicationContext(), MainMenu.class));

                    else {
                        startActivity(new Intent(getApplicationContext(), MainMenu.class));
                        startActivity(new Intent(getApplicationContext(), HowToUse.class));
                        mSharedPrefManager.setCheckFirst(true);
                    }

                    finish();
                } else {
                    init();
                    Log.d("MainActivity", "구글 인증 실패");
                    updateUI(mUser);
                }
            }
        };
    }

    private void getPermission() {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Log.d("Permission", "권한 허가");
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Log.d("Permission", "권한 거부");
            }
        };
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setPermissions(android.Manifest.permission.WRITE_CONTACTS,
                        android.Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.NFC,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.BIND_NFC_SERVICE,
                        Manifest.permission.CALL_PHONE
                ).check();
    }

    private void setMyContactList() {
        DatabaseReference listReference = FirebaseDatabase.getInstance().getReference().child("contact_list").child(mUser.getUid());


        listReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                ((ServiceApplication) getApplication()).myContactList = (ArrayList<String>) dataSnapshot.getValue();
                ((ServiceApplication) getApplication()).person = new HashMap<>();

                DatabaseReference infoReference = FirebaseDatabase.getInstance().getReference().child("myInfo");
                infoReference.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.d("메인차일드", dataSnapshot.getKey() + "#" + dataSnapshot.getValue());
                        if (((ServiceApplication) getApplication()).myContactList.contains(dataSnapshot.getKey())) {
                            ((ServiceApplication) getApplication()).person.put(
                                    dataSnapshot.getKey(),
                                    dataSnapshot.getValue(ContactData.class)
                            );
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String getPhoneNum() {
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        try {
            String phoneNum = telephonyManager.getLine1Number();
            if (phoneNum.startsWith("+82")) {
                phoneNum = phoneNum.replace("+82", "0");
            }
            displayUserPhoneNumber = PhoneNumberUtils.formatNumber(phoneNum);
        } catch (SecurityException e) {
            Log.d("getPhoneNum", "error");
        } return displayUserPhoneNumber;
    }

    private void init() {
        // [START config_signin]
        // Configure Google Sign In
        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        myContact();
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuth.addAuthStateListener(mListener);
    }
    // [END on_start_check_user]


    @Override
    protected void onStop() {
        super.onStop();

        if (mListener != null) {
            mAuth.removeAuthStateListener(mListener);
        }
    }

    // [START onactivityresult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // [START_EXCLUDE]
                updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            updateUI(null);
                        }
                        // [START_EXCLUDE]
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]

    // [START signin]
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signin]

    private void myContact() {
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        try {
            String phoneNum = telephonyManager.getLine1Number();
            if (phoneNum.startsWith("+82")) {
                phoneNum = phoneNum.replace("+82", "0");
            }
            displayUserPhoneNumber = PhoneNumberUtils.formatNumber(phoneNum);

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            //callDialog();

        } else {
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_in_button) {
            signIn();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("AppLifeCycle", "MainActivity");
    }
}
