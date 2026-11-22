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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.jejakrasa.R;
import com.example.jejakrasa.model.SavedSpotsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvAvatarInitials, tvProfileFullName, tvProfileEmail;
    private TextView tvStatSavedCount, tvStatReviewCount;
    private TextView tvBioFirstName, tvBioLastName, tvBioBirthDate;
    private ImageView ivProfileAvatar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Sembunyikan menu bar lama di dalam halaman
        View oldBottomNav = view.findViewById(R.id.bottomNavProfile);
        if (oldBottomNav != null) oldBottomNav.setVisibility(View.GONE);

        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials);
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvProfileFullName = view.findViewById(R.id.tvProfileFullName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvStatSavedCount = view.findViewById(R.id.tvStatSavedCount);
        tvStatReviewCount = view.findViewById(R.id.tvStatReviewCount);
        tvBioFirstName = view.findViewById(R.id.tvBioFirstName);
        tvBioLastName = view.findViewById(R.id.tvBioLastName);
        tvBioBirthDate = view.findViewById(R.id.tvBioBirthDate);

        FrameLayout frameAvatarContainer = view.findViewById(R.id.frameAvatarContainer);
        Button btnEditProfile = view.findViewById(R.id.btnEditProfile);
        Button btnProfileLogout = view.findViewById(R.id.btnProfileLogout);

        // Beri jarak bawah ekstra agar tombol Logout tidak tertutup menu mengambang
        if (btnProfileLogout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) btnProfileLogout.getLayoutParams();
            params.bottomMargin = 260;
            btnProfileLogout.setLayoutParams(params);
        }

        ivProfileAvatar.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });
        ivProfileAvatar.setClipToOutline(true);

        frameAvatarContainer.setOnClickListener(v -> tampilkanDialogEditProfil());
        btnEditProfile.setOnClickListener(v -> tampilkanDialogEditProfil());

        btnProfileLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Keluar Akun")
                    .setMessage("Yakin ingin keluar dari akun JejakRasa?")
                    .setPositiveButton("Keluar", (dialog, which) -> {
                        SavedSpotsManager.clearCache();
                        mAuth.signOut();
                        Toast.makeText(requireContext(), "Berhasil keluar akun", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        muatDataProfil();
    }

    private void tampilkanDialogEditProfil() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etFirst = dialogView.findViewById(R.id.etEditFirstName);
        EditText etLast = dialogView.findViewById(R.id.etEditLastName);
        Button btnGallery = dialogView.findViewById(R.id.btnPickGalleryProfile);
        Button btnCamera = dialogView.findViewById(R.id.btnTakeCameraProfile);

        etFirst.setText(currentFirstName);
        etLast.setText(currentLastName);

        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnCamera.setOnClickListener(v -> cameraLauncher.launch(null));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newFirst = etFirst.getText().toString().trim();
                    String newLast = etLast.getText().toString().trim();
                    if (newFirst.isEmpty()) {
                        Toast.makeText(requireContext(), "Nama depan tidak boleh kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    simpanProfilKeFirestore(newFirst, newLast, pendingPhotoBase64);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void prosesUriKeFirebase(Uri uri) {
        try {
            Bitmap original = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            prosesBitmapKeFirebase(original);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Gagal membaca gambar", Toast.LENGTH_SHORT).show();
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
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Profil tersimpan ke Firebase!", Toast.LENGTH_SHORT).show();
                        muatDataProfil();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void muatDataProfil() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvProfileEmail.setText(user.getEmail());

        SavedSpotsManager.loadUserSavedSpots(spots -> {
            if (isAdded()) {
                tvStatSavedCount.setText(String.valueOf(spots.size()));
            }
        });

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
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

        db.collection("reviews")
                .whereEqualTo("uid", user.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isAdded()) {
                        tvStatReviewCount.setText(String.valueOf(snapshots.size()));
                    }
                });
    }
}