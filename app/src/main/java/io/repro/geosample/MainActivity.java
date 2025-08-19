package io.repro.geosample;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String GEOFENCE_ACTION = "io.repro.android.dev.ACTION_GEOFENCE_EVENT";


    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        int res = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);


        Button startButton = findViewById(R.id.start_geofence_button);
        startButton.setOnClickListener(v -> {
            Log.d(TAG, "Start Geofence button clicked");
            startListenGeofence();
        });

        Button stopButton = findViewById(R.id.stop_geofence_button);
        stopButton.setOnClickListener(v -> {
            Log.d(TAG, "Stop Geofence button clicked");
            stopListenGeofence();
        });
    }



    private void startListenGeofence() {
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        List<Geofence> geofenceList = new ArrayList<>();
        Geofence geofence = new Geofence.Builder()
                .setRequestId("request-id-1")
                .setCircularRegion(35.7146004,139.8625569, 1000f) // radiusはメートル
                .setExpirationDuration(Geofence.NEVER_EXPIRE) // ジオフェンスの有効期限
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        geofenceList.add(geofence);

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // 既にジオフェンス内にいるとき、GEOFENCE_TRANSITION_ENTERをトリガーする必要があることを指示する
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER); // DWELL推奨

        builder.addGeofence(geofence);
        GeofencingRequest geofencingRequest = builder.build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
            return;
        }



        mGeofencingClient
                .addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Geofences added successfully.");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to add geofences.", e);
                    }
                });

    }

    private PendingIntent getGeofencePendingIntent() {

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);

        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return mGeofencePendingIntent;
    }

    private void stopListenGeofence() {
        if (mGeofencingClient == null) {
            return;
        }
        mGeofencingClient
                .removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i(TAG, "Geofences removed successfully.");
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to remove geofences.", e);
                    }
                });
    }
}