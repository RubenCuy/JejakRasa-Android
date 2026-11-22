package com.example.jejakrasa.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jejakrasa.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CheckInReviewActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_NAME = "extra_place_name";
    private ImageView ivPreviewPhoto;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Void> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), (Bitmap bitmap) -> {
                if (bitmap != null) {
                    ivPreviewPhoto.setImageBitmap(bitmap);
                    Toast.makeText(this, "Foto berhasil diambil!", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    takePictureLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Izin kamera ditolak oleh pengguna", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), (Uri uri) -> {
                if (uri != null) {
                    ivPreviewPhoto.setImageURI(uri);
                    Toast.makeText(this, "Foto dipilih dari galeri!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_check_in_review);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        TextView tvReviewPlaceName = findViewById(R.id.tvReviewPlaceName);
        RatingBar rbFoodRating = findViewById(R.id.rbFoodRating);
        EditText etReviewComment = findViewById(R.id.etReviewComment);
        ivPreviewPhoto = findViewById(R.id.ivPreviewPhoto);
        Button btnBackReview = findViewById(R.id.btnBackReview);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnPickGallery = findViewById(R.id.btnPickGallery);
        Button btnSubmitReview = findViewById(R.id.btnSubmitReview);

        String placeName = getIntent().getStringExtra(EXTRA_PLACE_NAME);
        if (placeName != null && !placeName.isEmpty()) {
            tvReviewPlaceName.setText(placeName);
        }

        btnBackReview.setOnClickListener(v -> finish());

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null);
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnPickGallery.setOnClickListener(v -> pickGalleryLauncher.launch("image/*"));

        btnSubmitReview.setOnClickListener(v -> {
            String comment = etReviewComment.getText().toString().trim();
            float rating = rbFoodRating.getRating();
            if (comment.isEmpty()) {
                etReviewComment.setError("Tuliskan ulasan singkat terlebih dahulu");
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmitReview.setEnabled(false);
            String dateNow = new SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(new Date());

            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("uid", user.getUid());
            reviewData.put("placeName", tvReviewPlaceName.getText().toString());
            reviewData.put("rating", (double) rating);
            reviewData.put("comment", comment);
            reviewData.put("dateText", dateNow);
            reviewData.put("timestamp", System.currentTimeMillis());

            db.collection("reviews").add(reviewData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Ulasan tersimpan ke Riwayat! ★ " + rating, Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitReview.setEnabled(true);
                        Toast.makeText(this, "Gagal menyimpan ulasan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}