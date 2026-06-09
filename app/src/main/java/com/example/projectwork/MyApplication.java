package com.example.projectwork;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", getString(R.string.cloudinary_cloud_name));
        config.put("api_key", getString(R.string.cloudinary_api_key));
        config.put("api_secret", getString(R.string.cloudinary_api_secret));
        MediaManager.init(this, config);
    }
}