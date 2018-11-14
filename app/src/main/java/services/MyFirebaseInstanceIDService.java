package services;


import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceIdService;

import util.DataCollectionApplication;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "Firebase";

    @Override
    public void onTokenRefresh() {
        DataCollectionApplication.setIsSentToServer(false);
        Log.d(TAG, "Refreshed token");
    }
}