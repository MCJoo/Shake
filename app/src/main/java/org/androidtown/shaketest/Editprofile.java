package org.androidtown.shaketest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.qrcode.encoder.QRCode;
import com.journeyapps.barcodescanner.ViewfinderView;

import java.util.ArrayList;

public class Editprofile extends Fragment {
    private FragmentManager fragmentManager;
    String displayUserPhoneNumber;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String userName, userPhoneNum, userEmail;
    public static Editprofile newInstance() {
        Bundle args = new Bundle();
        Editprofile fragment = new Editprofile();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ViewGroup mView = (ViewGroup) inflater.inflate(R.layout.activity_editprofile, container, false);
        fragmentManager = getActivity().getSupportFragmentManager();
        Button Customize = (Button)mView.findViewById(R.id.button2);
    //  getPhonenum();

        Customize.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                fragmentManager.beginTransaction().replace(R.id.frameLayout, Customized_main.newInstance()).commit();
                Toast.makeText(getActivity(), "커스터마이징 수정하기가 눌렸습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        return mView;
    }
    private void getPhonenum() {
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        try {
            String phoneNum = telephonyManager.getLine1Number();
            if (phoneNum.startsWith("+82")) {
                phoneNum = phoneNum.replace("+82", "0");
            }
            displayUserPhoneNumber = PhoneNumberUtils.formatNumber(phoneNum);
        } catch (SecurityException e) {
            Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
        }
    }
    private void init() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        userName = mUser.getDisplayName();
        userEmail = mUser.getEmail();
        userPhoneNum = displayUserPhoneNumber;
    }
}
