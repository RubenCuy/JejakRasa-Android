package com.example.jejakrasa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.jejakrasa.MainActivity;
import com.example.jejakrasa.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvForgotPassword;
    private ProgressBar progressLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressLogin = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        tvForgotPassword.setOnClickListener(v -> tampilkanBottomSheetResetPassword());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Masukkan format email yang valid");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Kata sandi minimal 6 karakter");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        user.reload().addOnCompleteListener(reloadTask -> {
                            setLoading(false);
                            if (user.isEmailVerified()) {
                                Toast.makeText(this, "Selamat datang di JejakRasa!", Toast.LENGTH_SHORT).show();
                                goToMain();
                            } else {
                                new AlertDialog.Builder(this)
                                        .setTitle("Email Belum Diverifikasi")
                                        .setMessage("Buka kotak masuk atau folder Spam di " + email
                                                + ", lalu klik tautan dari Firebase terlebih dahulu.")
                                        .setPositiveButton("Kirim Ulang Link", (d, w) -> {
                                            user.sendEmailVerification();
                                            Toast.makeText(this, "Email verifikasi dikirim ulang!", Toast.LENGTH_SHORT).show();
                                            mAuth.signOut();
                                        })
                                        .setNegativeButton("Tutup", (d, w) -> mAuth.signOut())
                                        .show();
                            }
                        });
                    } else {
                        setLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Gagal masuk: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void tampilkanBottomSheetResetPassword() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_reset_password, null);
        bottomSheetDialog.setContentView(sheetView);

        // Pastikan sudut membulat tampil bersih tanpa kotak putih di belakangnya
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        LinearLayout layoutResetForm = sheetView.findViewById(R.id.layoutResetForm);
        LinearLayout layoutResetSuccess = sheetView.findViewById(R.id.layoutResetSuccess);

        EditText etSheetResetEmail = sheetView.findViewById(R.id.etSheetResetEmail);
        TextView tvSheetResetError = sheetView.findViewById(R.id.tvSheetResetError);
        MaterialButton btnSheetCancelReset = sheetView.findViewById(R.id.btnSheetCancelReset);
        MaterialButton btnSheetSendReset = sheetView.findViewById(R.id.btnSheetSendReset);

        TextView tvSuccessEmailTarget = sheetView.findViewById(R.id.tvSuccessEmailTarget);
        MaterialButton btnOpenEmailApp = sheetView.findViewById(R.id.btnOpenEmailApp);
        MaterialButton btnDoneReset = sheetView.findViewById(R.id.btnDoneReset);

        // Otomatis isi email jika user sudah mengetiknya di kolom Login
        String emailAwal = etEmail.getText().toString().trim();
        if (!emailAwal.isEmpty()) {
            etSheetResetEmail.setText(emailAwal);
            etSheetResetEmail.setSelection(emailAwal.length());
        }

        // Sembunyikan pesan error saat user mulai mengetik ulang
        etSheetResetEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvSheetResetError.getVisibility() == View.VISIBLE) {
                    tvSheetResetError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSheetCancelReset.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnSheetSendReset.setOnClickListener(v -> {
            String emailReset = etSheetResetEmail.getText().toString().trim();
            if (emailReset.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailReset).matches()) {
                tvSheetResetError.setText("Masukkan alamat email yang valid (contoh: budi@gmail.com)");
                tvSheetResetError.setVisibility(View.VISIBLE);
                etSheetResetEmail.requestFocus();
                return;
            }

            // State loading langsung di dalam tombol
            tvSheetResetError.setVisibility(View.GONE);
            btnSheetSendReset.setEnabled(false);
            btnSheetCancelReset.setEnabled(false);
            btnSheetSendReset.setText("Mengirim...");

            mAuth.sendPasswordResetEmail(emailReset)
                    .addOnSuccessListener(unused -> {
                        // Transisi mulus ke tampilan sukses di dalam BottomSheet yang sama
                        etEmail.setText(emailReset);
                        tvSuccessEmailTarget.setText(emailReset);
                        layoutResetForm.setVisibility(View.GONE);
                        layoutResetSuccess.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(e -> {
                        btnSheetSendReset.setEnabled(true);
                        btnSheetCancelReset.setEnabled(true);
                        btnSheetSendReset.setText("Kirim Link Reset");
                        tvSheetResetError.setText("Gagal mengirim: " + e.getLocalizedMessage());
                        tvSheetResetError.setVisibility(View.VISIBLE);
                    });
        });

        btnOpenEmailApp.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_MAIN);
            emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(emailIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Buka aplikasi Gmail di perangkatmu untuk mengecek tautan.", Toast.LENGTH_LONG).show();
            }
        });

        btnDoneReset.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void setLoading(boolean isLoading) {
        progressLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnRegister.setEnabled(!isLoading);
        tvForgotPassword.setEnabled(!isLoading);
    }

    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}