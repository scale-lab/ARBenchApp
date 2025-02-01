package com.example.arbenchapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import com.example.arbenchapp.datatypes.MTLBoxStruct;
import com.example.arbenchapp.datatypes.RunType;
import com.example.arbenchapp.datatypes.Settings;
import com.example.arbenchapp.monitor.HardwareMonitor;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageView;
import android.widget.TextView;

import com.example.arbenchapp.databinding.ActivityMainBinding;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    // Registers a photo picker activity launcher in single-select mode.
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // Callback is invoked after the user selects a media item or closes the
                // photo picker
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: " + uri);
                    try {
                        // BUG: switching tabs messes with image positioning
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        System.out.println("CONV2D RUN .. bitmap config: " + bitmap.getConfig());
                        // testing python interpreter
                        Settings s = new Settings(RunType.CONV2D);
                        MTLBox mtlBox = new MTLBox(s);

                        MTLBoxStruct processed = mtlBox.run(bitmap, this);
                        Bitmap bm = processed.getBitmap();

                        TextView tv = findViewById(R.id.text_home);
                        tv.setText(createDisplayString(processed));
                        ImageView iv = findViewById(R.id.imageView);
                        iv.setImageBitmap(bm);
                        System.out.println("CONV2D RUN .. displayed!!!");
                        ImageView uv = findViewById(R.id.unfilteredView);
                        uv.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null)
//                        .setAnchorView(R.id.fab).show();
                // test open gallery
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Adjust imageView
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        ImageView iv = findViewById(R.id.imageView);
        ConstraintLayout.LayoutParams iv_params = (ConstraintLayout.LayoutParams) iv.getLayoutParams();
        iv_params.bottomMargin = (int) (screenHeight * 0.5);
        iv.setLayoutParams(iv_params);

        // Adjust unfilteredView
        ImageView uv = findViewById(R.id.unfilteredView);
        ConstraintLayout.LayoutParams uv_params = (ConstraintLayout.LayoutParams) uv.getLayoutParams();
        uv_params.topMargin = (int) (screenHeight * 0.5);
        uv.setLayoutParams(uv_params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public String createDisplayString(MTLBoxStruct processed) {
        Double tm = processed.getTime();
        HardwareMonitor.HardwareMetrics metrics = processed.getMetrics();
        String[] messages = getMessages(tm, metrics);
        StringBuilder displayStrBuilt = new StringBuilder();
        for (String m : messages) {
            displayStrBuilt.append(m).append("\n");
        }
        String displayStr = displayStrBuilt.toString();
        if (displayStr.endsWith("\n")) {
            displayStr = displayStr.substring(0, displayStr.length() - 1);
        }
        return displayStr;
    }

    private static String [] getMessages(Double tm, HardwareMonitor.HardwareMetrics metrics) {
        String nullMetricsMessage = "ERR: Returned NULL metrics";
        int decimalPoints = 2;
        String timeDisplay = "Runtime: " + tm + " ms";
        String memoryUsageDisplay = metrics != null ?
                "Memory usage: " + round(metrics.memoryUsageMB, decimalPoints) + " mb" :
                nullMetricsMessage;
        String memoryDeltaDisplay = metrics != null ?
                "Memory delta: " + round(metrics.memoryDeltaMB, decimalPoints) + " mb" :
                nullMetricsMessage;
        String cpuUsagePercentDisplay = metrics != null ?
                "CPU Usage Percent: " + round(metrics.cpuUsagePercent, decimalPoints) + "%" :
                nullMetricsMessage;
        String cpuUsageDeltaDisplay = metrics != null ?
                "CPU Usage Percent Delta: " + round(metrics.cpuUsageDelta, decimalPoints) + "%" :
                nullMetricsMessage;
        return new String[]{
                timeDisplay,
                memoryUsageDisplay,
                memoryDeltaDisplay,
                cpuUsagePercentDisplay,
                cpuUsageDeltaDisplay
        };
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}