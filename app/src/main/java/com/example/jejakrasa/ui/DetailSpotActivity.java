package com.example.jejakrasa.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.jejakrasa.R;
import com.example.jejakrasa.model.CulinarySpot;
import com.example.jejakrasa.model.SavedSpotsManager;

public class DetailSpotActivity extends AppCompatActivity {

    public static final String EXTRA_SPOT = "extra_spot";
    private CulinarySpot currentSpot;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_spot);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView ivDetailImage = findViewById(R.id.ivDetailImage);
        TextView tvDetailKategori = findViewById(R.id.tvDetailKategori);
        TextView tvDetailBadgeTahun = findViewById(R.id.tvDetailBadgeTahun);
        TextView tvDetailNama = findViewById(R.id.tvDetailNama);
        TextView tvDetailRatingHarga = findViewById(R.id.tvDetailRatingHarga);
        TextView tvDetailAlamat = findViewById(R.id.tvDetailAlamat);
        TextView tvDetailKisah = findViewById(R.id.tvDetailKisah);
        WebView webViewMiniMap = findViewById(R.id.webViewMiniMap);
        View btnBackDetail = findViewById(R.id.btnBackDetail);
        Button btnOpenMap = findViewById(R.id.btnOpenMap);
        Button btnOpenReview = findViewById(R.id.btnOpenReview);

        currentSpot = (CulinarySpot) getIntent().getSerializableExtra(EXTRA_SPOT);

        if (currentSpot != null) {
            Glide.with(this)
                    .load(currentSpot.getImageUrl())
                    .placeholder(R.drawable.bg_placeholder_food)
                    .error(R.drawable.bg_placeholder_food)
                    .centerCrop()
                    .into(ivDetailImage);

            tvDetailKategori.setText(currentSpot.getKategori().toUpperCase());
            tvDetailNama.setText(currentSpot.getNamaTempat());
            tvDetailRatingHarga.setText("★ " + currentSpot.getRating() + "  •  " + currentSpot.getKisaranHarga());
            tvDetailAlamat.setText(currentSpot.getAlamatSingkat());
            tvDetailKisah.setText(currentSpot.getKisahLegendaris());

            tvDetailBadgeTahun.setText(SavedSpotsManager.isSaved(currentSpot.getId()) ? "★ Tersimpan" : "+ Simpan Spot");
            SavedSpotsManager.loadUserSavedSpots(spots ->
                    tvDetailBadgeTahun.setText(SavedSpotsManager.isSaved(currentSpot.getId()) ? "★ Tersimpan" : "+ Simpan Spot")
            );

            tvDetailBadgeTahun.setOnClickListener(v -> {
                SavedSpotsManager.toggleSave(currentSpot, isSaved -> {
                    tvDetailBadgeTahun.setText(isSaved ? "★ Tersimpan" : "+ Simpan Spot");
                    Toast.makeText(this, isSaved ? "Disimpan ke akunmu" : "Dihapus dari Tersimpan", Toast.LENGTH_SHORT).show();
                });
            });

            muatKotakPetaMini(webViewMiniMap, currentSpot.getNamaTempat() + " " + currentSpot.getAlamatSingkat());
        }

        webViewMiniMap.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });

        btnBackDetail.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        btnOpenMap.setOnClickListener(v -> {
            if (currentSpot != null) {
                Intent mapIntent = new Intent(DetailSpotActivity.this, WebMapActivity.class);
                mapIntent.putExtra(WebMapActivity.EXTRA_MAP_URL, currentSpot.getUrlGoogleMaps());
                mapIntent.putExtra(WebMapActivity.EXTRA_PLACE_NAME, currentSpot.getNamaTempat() + " " + currentSpot.getAlamatSingkat());
                startActivity(mapIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        btnOpenReview.setOnClickListener(v -> {
            if (currentSpot != null) {
                Intent reviewIntent = new Intent(DetailSpotActivity.this, CheckInReviewActivity.class);
                reviewIntent.putExtra(CheckInReviewActivity.EXTRA_PLACE_NAME, currentSpot.getNamaTempat());
                startActivity(reviewIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void muatKotakPetaMini(WebView webView, String lokasiQuery) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Tahan link intent:// agar peta tetap tampil di dalam WebView
                return !url.startsWith("http://") && !url.startsWith("https://");
            }
        });

        // Hapus tanda kutip tunggal agar atribut HTML tidak terpotong
        String cleanQuery = lokasiQuery.replace("'", "").replace("\"", "");
        String embedUrl = "https://maps.google.com/maps?q=" + Uri.encode(cleanQuery) + "&t=&z=16&ie=UTF8&iwloc=&output=embed";
        String htmlIframe = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes\">" +
                "<style>html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;}iframe{border:0;width:100%;height:100%;}</style>" +
                "</head><body>" +
                "<iframe src=\"" + embedUrl + "\" allowfullscreen loading=\"lazy\"></iframe>" +
                "</body></html>";

        webView.loadDataWithBaseURL("https://www.google.com", htmlIframe, "text/html", "UTF-8", null);
    }
}