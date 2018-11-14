package activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.gcs.riyadh.R;

import java.lang.ref.WeakReference;

import util.DataCollectionApplication;
import util.Utilities;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //If user not logged in before open login activity
                if (DataCollectionApplication.getSurveyorId() == -1) {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    //Open Map Editor activity
                    Intent intent = new Intent(SplashActivity.this, MapEditorActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, 3000);
    }

}
