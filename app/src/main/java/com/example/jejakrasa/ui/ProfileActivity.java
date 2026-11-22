package com.example.jejakrasa.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jejakrasa.R;
import com.example.jejakrasa.model.SavedSpotsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitials, tvProfileFullName, tvProfileEmail;
    private TextView tvStatSavedCount, tvStatReviewCount;
    private TextView tvBioFirstName, tvBioLastName, tvBioBirthDate;
    private ImageView ivProfileAvatar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SwipeTabHelper swipeTabHelper;

    private String currentFirstName = "";
    private String currentLastName = "";
    private String pendingPhotoBase64 = null;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    prosesUriKeFirebase(uri);
                }
            });

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    prosesBitmapKeFirebase(bitmap);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        swipeTabHelper = new SwipeTabHelper(this, SwipeTabHelper.TAB_PROFILE);

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvStatSavedCount = findViewById(R.id.tvStatSavedCount);
        tvStatReviewCount = findViewById(R.id.tvStatReviewCount);
        tvBioFirstName = findViewById(R.id.tvBioFirstName);
        tvBioLastName = findViewById(R.id.tvBioLastName);
        tvBioBirthDate = findViewById(R.id.tvBioBirthDate);

        FrameLayout frameAvatarContainer = findViewById(R.id.frameAvatarContainer);
        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        Button btnProfileLogout = findViewById(R.id.btnProfileLogout);
        BottomNavigationView bottomNavProfile = findViewById(R.id.bottomNavProfile);

        bottomNavProfile.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        // Buat bingkai foto profil bulat sempurna
        ivProfileAvatar.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        ivProfileAvatar.setClipToOutline(true);

        frameAvatarContainer.setOnClickListener(v -> tampilkanDialogEditProfil());
        btnEditProfile.setOnClickListener(v -> tampilkanDialogEditProfil());

        muatDataProfil();

        btnProfileLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Keluar Akun")
                    .setMessage("Yakin ingin keluar dari akun JejakRasa?")
                    .setPositiveButton("Keluar", (dialog, which) -> {
                        SavedSpotsManager.clearCache();
                        mAuth.signOut();
                        Toast.makeText(this, "Berhasil keluar akun", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        bottomNavProfile.setSelectedItemId(R.id.nav_profile);
        bottomNavProfile.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_PROFILE, SwipeTabHelper.TAB_EXPLORE);
                return true;
            } else if (id == R.id.nav_saved) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_PROFILE, SwipeTabHelper.TAB_SAVED);
                return true;
            } else if (id == R.id.nav_history) {
                SwipeTabHelper.navigateToTab(this, SwipeTabHelper.TAB_PROFILE, SwipeTabHelper.TAB_HISTORY);
                return true;
            }
            return id == R.id.nav_profile;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        swipeTabHelper.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void tampilkanDialogEditProfil() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText etFirst = dialogView.findViewById(R.id.etEditFirstName);
        EditText etLast = dialogView.findViewById(R.id.etEditLastName);
        Button btnGallery = dialogView.findViewById(R.id.btnPickGalleryProfile);
        Button btnCamera = dialogView.findViewById(R.id.btnTakeCameraProfile);

        etFirst.setText(currentFirstName);
        etLast.setText(currentLastName);

        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnCamera.setOnClickListener(v -> cameraLauncher.launch(null));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newFirst = etFirst.getText().toString().trim();
                    String newLast = etLast.getText().toString().trim();
                    if (newFirst.isEmpty()) {
                        Toast.makeText(this, "Nama depan tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    simpanProfilKeFirestore(newFirst, newLast, pendingPhotoBase64);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void prosesUriKeFirebase(Uri uri) {
        try {
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            prosesBitmapKeFirebase(original);
        } catch (IOException e) {
            Toast.makeText(this, "Gagal membaca gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private void prosesBitmapKeFirebase(Bitmap original) {
        Bitmap resized = Bitmap.createScaledBitmap(original, 256, 256, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        pendingPhotoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        ivProfileAvatar.setImageBitmap(resized);
        ivProfileAvatar.setVisibility(View.VISIBLE);
        tvAvatarInitials.setVisibility(View.GONE);

        // Langsung simpan foto baru ke Firestore
        simpanProfilKeFirestore(currentFirstName, currentLastName, pendingPhotoBase64);
    }

    private void simpanProfilKeFirestore(String first, String last, String photoBase64) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        if (!first.isEmpty()) updates.put("firstName", first);
        updates.put("lastName", last);
        if (photoBase64 != null) {
            updates.put("photoBase64", photoBase64);
        }

        db.collection("users").document(user.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profil tersimpan ke Firebase!", Toast.LENGTH_SHORT).show();
                    muatDataProfil();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void muatDataProfil() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvProfileEmail.setText(user.getEmail());

        SavedSpotsManager.loadUserSavedSpots(spots ->
                tvStatSavedCount.setText(String.valueOf(spots.size()))
        );

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String first = doc.getString("firstName");
                        String last = doc.getString("lastName");
                        String birth = doc.getString("birthDate");
                        String photoBase64 = doc.getString("photoBase64");

                        currentFirstName = first != null ? first : "";
                        currentLastName = last != null ? last : "";

                        String fullName = (currentFirstName + " " + currentLastName).trim();
                        if (!fullName.isEmpty()) {
                            tvProfileFullName.setText(fullName);
                            String initials = "";
                            if (!currentFirstName.isEmpty()) initials += currentFirstName.substring(0, 1).toUpperCase();
                            if (!currentLastName.isEmpty()) initials += currentLastName.substring(0, 1).toUpperCase();
                            tvAvatarInitials.setText(initials.isEmpty() ? "JR" : initials);
                        }

                        if (photoBase64 != null && !photoBase64.isEmpty()) {
                            byte[] decoded = Base64.decode(photoBase64, Base64.DEFAULT);
                            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            if (bmp != null) {
                                ivProfileAvatar.setImageBitmap(bmp);
                                ivProfileAvatar.setVisibility(View.VISIBLE);
                                tvAvatarInitials.setVisibility(View.GONE);
                            }
                        }

                        tvBioFirstName.setText("Nama Depan: " + (!currentFirstName.isEmpty() ? currentFirstName : "-"));
                        tvBioLastName.setText("Nama Belakang: " + (!currentLastName.isEmpty() ? currentLastName : "-"));
                        tvBioBirthDate.setText("Tanggal Lahir: " + (birth != null ? birth : "-"));
                    }
                });

        db.collection("reviews").whereEqualTo("uid", user.getUid()).get()
                .addOnSuccessListener(snapshots ->
                        tvStatReviewCount.setText(String.valueOf(snapshots.size()))
                );
    }
}