package activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gcs.riyadh.R;

import connection.ConnectionManager;
import data.Surveyor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import util.DataCollectionApplication;
import util.Utilities;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etName, etPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();

    }

    /**
     * Initialize activity views
     */
    private void initViews() {
        etName = (EditText) findViewById(R.id.etName);
        etPassword = (EditText) findViewById(R.id.etPassword);
        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        //Call onClick method when this button clicked
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //If internet available invoke Login API using retrofit
        if (Utilities.isNetworkAvailable(this)) {

            //If name empty show error
            if (etName.getText().toString().equals("")) {
                etName.setError(getString(R.string.user_name));

                //If password empty show error
            } else if (etPassword.getText().toString().equals("")) {
                etPassword.setError(getString(R.string.password));

                //Get the device Id
            } else {
                getDeviceId();
            }
        } else {
            Utilities.showToast(this, getString(R.string.no_internet));
        }
    }

    /**
     * Get the phone ID to send it to server
     */
    private void getDeviceId() {
        //If user not accept permission read phone state, request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        } else {
            //Process login scenario
            processLogin(etName.getText().toString(), etPassword.getText().toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //User accepted permission, Process login scenario
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            processLogin(etName.getText().toString(), etPassword.getText().toString());
        } else {
            Toast.makeText(this, getResources().getString(R.string.accept_permission), Toast.LENGTH_SHORT).show();
        }
    }

    private void processLogin(String userName, String password) {
        //Show loading dialog
        Utilities.showLoadingDialog(this);
        //Get device id
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //The device id
        String deviceId = telephonyManager.getDeviceId();
        //If no device id set it with default value
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = "200";
        }

        //Call Login api
        ConnectionManager.getInstance().login(userName, password, deviceId).enqueue(new Callback<Surveyor>() {
            @Override
            public void onResponse(Call<Surveyor> call, Response<Surveyor> response) {
                Utilities.dismissLoadingDialog();
                if (response.isSuccessful()) {
                    //Get surveyor
                    Surveyor surveyor = response.body();
                    //---------Save surveyor info in shared preference
                    DataCollectionApplication.setSurveyorName(surveyor.getSurveyorName());
                    DataCollectionApplication.setSurveyorId(surveyor.getSurveyorId());
                    //---------
                    //Create shortcut if it not created before
                    if (!DataCollectionApplication.isShortCutCreated())
                        addShortcut();

                    //Start map activity
                    Intent intent = new Intent(LoginActivity.this, MapEditorActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Utilities.showToast(LoginActivity.this, getString(R.string.wrong_username_or_password));
                }

            }

            @Override
            public void onFailure(Call<Surveyor> call, Throwable t) {
                Utilities.dismissLoadingDialog();
                Utilities.showToast(LoginActivity.this, getString(R.string.connection_error));
            }
        });
    }

    /**
     * Create shortcut on user phone home screen
     */
    private void addShortcut() {
        Log.d("LoginActivity", "In Add Shortcut");
        Intent shortcutIntent = new Intent(getApplicationContext(), SplashActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
        addIntent.putExtra("duplicate", false);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

}
