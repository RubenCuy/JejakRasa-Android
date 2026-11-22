package com.example.jejakrasa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jejakrasa.R;
import com.example.jejakrasa.adapter.CulinarySpotAdapter;
import com.example.jejakrasa.model.SavedSpotsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SavedFragment extends Fragment {

    private RecyclerView rvSavedSpots;
    private TextView tvEmptySaved, tvUserEmail;
    private CulinarySpotAdapter adapter;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_saved_spots, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Sembunyikan menu bar lama di dalam halaman karena sudah memakai menu bar mengambang
        View oldBottomNav = view.findViewById(R.id.bottomNavSaved);
        if (oldBottomNav != null) oldBottomNav.setVisibility(View.GONE);

        rvSavedSpots = view.findViewById(R.id.rvSavedSpots);
        tvEmptySaved = view.findViewById(R.id.tvEmptySaved);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        rvSavedSpots.setPadding(0, 16, 0, 240);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            tvUserEmail.setText("Akun: " + currentUser.getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Keluar Akun")
                    .setMessage("Yakin ingin keluar dari akun ini?")
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

        rvSavedSpots.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CulinarySpotAdapter(SavedSpotsManager.getCachedSpots(), spot -> {
            Intent intent = new Intent(requireContext(), DetailSpotActivity.class);
            intent.putExtra(DetailSpotActivity.EXTRA_SPOT, spot);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        rvSavedSpots.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        SavedSpotsManager.loadUserSavedSpots(spots -> {
            if (!isAdded()) return;
            adapter.updateData(spots);
            tvEmptySaved.setVisibility(spots.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}