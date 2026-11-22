package com.example.jejakrasa.ui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jejakrasa.R;
import com.example.jejakrasa.model.UserLocationManager;

public class WebMapActivity extends AppCompatActivity {

    public static final String EXTRA_MAP_URL = "extra_map_url";
    public static final String EXTRA_PLACE_NAME = "extra_place_name";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_web_map);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnBackMap = findViewById(R.id.btnBackMap);
        TextView tvMapTitle = findViewById(R.id.tvMapTitle);
        ProgressBar progressBarMap = findViewById(R.id.progressBarMap);
        WebView webViewMaps = findViewById(R.id.webViewMaps);

        String placeName = getIntent().getStringExtra(EXTRA_PLACE_NAME);
        String queryLokasi = (placeName != null && !placeName.isEmpty()) ? placeName : "Kuliner Nusantara";
        String cleanDestination = queryLokasi.replace("'", "").replace("\"", "");

        WebSettings webSettings = webViewMaps.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(true);

        webViewMaps.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBarMap.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBarMap.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return !url.startsWith("http://") && !url.startsWith("https://");
            }
        });

        String embedUrl;
        if (UserLocationManager.hasOrigin()) {
            tvMapTitle.setText("Rute ke: " + cleanDestination);
            String cleanOrigin = UserLocationManager.getOriginForMaps().replace("'", "").replace("\"", "");
            embedUrl = "https://maps.google.com/maps?saddr=" + Uri.encode(cleanOrigin)
                    + "&daddr=" + Uri.encode(cleanDestination)
                    + "&output=embed";
        } else {
            tvMapTitle.setText("Peta: " + cleanDestination);
            embedUrl = "https://maps.google.com/maps?q=" + Uri.encode(cleanDestination)
                    + "&t=&z=16&ie=UTF8&iwloc=&output=embed";
        }

        String htmlIframe = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">" +
                "<style>html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;}iframe{border:0;width:100%;height:100%;}</style>" +
                "</head><body>" +
                "<iframe src=\"" + embedUrl + "\" allowfullscreen loading=\"lazy\"></iframe>" +
                "</body></html>";

        webViewMaps.loadDataWithBaseURL("https://www.google.com", htmlIframe, "text/html", "UTF-8", null);

        btnBackMap.setOnClickListener(v -> finish());
    }
}