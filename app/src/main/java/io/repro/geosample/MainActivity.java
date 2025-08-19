package io.repro.geosample;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;

    // 権限リクエストの結果を処理するランチャー
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // 1. まずはFINE_LOCATIONが許可されたかチェック
                if (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    Log.d(TAG, "ACCESS_FINE_LOCATION granted.");
                    // 2. 次にBACKGROUND_LOCATIONが必要か、そして許可されたかチェック
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "ACCESS_BACKGROUND_LOCATION granted.");
                            // 両方許可されたのでジオフェンスを開始
                            addGeofences();
                        } else {
                            // BACKGROUNDが拒否された場合
                            Log.w(TAG, "ACCESS_BACKGROUND_LOCATION not granted.");
                            // なぜ必要なのかを説明するダイアログなどを表示するのが親切
                            new AlertDialog.Builder(this)
                                    .setTitle("注意")
                                    .setMessage("バックグラウンドでの位置情報へのアクセスが許可されませんでした。ジオフェンス機能は利用できません。")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    } else {
                        // Android 10 (Q) 未満ではBACKGROUND権限は不要
                        addGeofences();
                    }
                } else {
                    // FINE_LOCATIONが拒否された場合
                    Log.w(TAG, "ACCESS_FINE_LOCATION not granted.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mGeofencingClient = LocationServices.getGeofencingClient(this);

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
        // Step 1: まずはFINE_LOCATIONの権限を確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // FINE_LOCATIONがなければリクエスト
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            return;
        }

        // Step 2: Android 10以上の場合、次にBACKGROUND_LOCATIONの権限を確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // BACKGROUND_LOCATIONがなければ、理由を説明してリクエスト
                new AlertDialog.Builder(this)
                        .setTitle("バックグラウンドでの位置情報利用について")
                        .setMessage("このアプリは、ジオフェンス機能のためにバックグラウンドで位置情報を継続的に利用します。「設定」画面で、位置情報の権限を「常に許可」に設定してください。")
                        .setPositiveButton("設定画面へ", (dialog, which) -> {
                            // BACKGROUND_LOCATION権限をリクエスト
                            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION});
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
                return;
            }
        }

        // Step 3: すべての権限が揃っていれば、ジオフェンスを開始
        Log.d(TAG, "All permissions are already granted. Starting geofence.");
        addGeofences();
    }

    private void addGeofences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Tried to add geofences without FINE_LOCATION permission.");
            return; // 念のためのチェック
        }

        List<Geofence> geofenceList = new ArrayList<>();
        Geofence geofence = new Geofence.Builder()
                .setRequestId("request-id-1")
                .setCircularRegion(35.7146004, 139.8625569, 1000f) // radiusはメートル
                .setExpirationDuration(Geofence.NEVER_EXPIRE) // ジオフェンスの有効期限
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        geofenceList.add(geofence);

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // DWELL推奨
                .addGeofences(geofenceList)
                .build();

        mGeofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
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

        // Android 12 (API 31) 以降では、FLAG_IMMUTABLE または FLAG_MUTABLE の指定が必須
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
        return mGeofencePendingIntent;
    }

    private void stopListenGeofence() {
        if (mGeofencingClient != null) {
            mGeofencingClient.removeGeofences(getGeofencePendingIntent())
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
}