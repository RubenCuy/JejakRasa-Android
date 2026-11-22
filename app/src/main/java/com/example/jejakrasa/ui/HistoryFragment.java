package com.example.jejakrasa.ui;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvReviewHistory;
    private TextView tvEmptyHistory;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<ReviewItem> reviewList = new ArrayList<>();
    private HistoryAdapter adapter;

    static class ReviewItem {
        String id;
        String placeName;
        double rating;
        String comment;
        String dateText;
        long timestamp;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_review_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Sembunyikan menu bar lama di dalam halaman
        View oldBottomNav = view.findViewById(R.id.bottomNavHistory);
        if (oldBottomNav != null) oldBottomNav.setVisibility(View.GONE);

        rvReviewHistory = view.findViewById(R.id.rvReviewHistory);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);

        rvReviewHistory.setPadding(0, 16, 0, 240);
        rvReviewHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter();
        rvReviewHistory.setAdapter(adapter);

        muatRiwayatUlasan();
    }

    private void muatRiwayatUlasan() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("reviews")
                .whereEqualTo("uid", user.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded() || error != null || snapshots == null) return;

                    reviewList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ReviewItem item = new ReviewItem();
                        item.id = doc.getId();
                        item.placeName = doc.getString("placeName");
                        Double r = doc.getDouble("rating");
                        item.rating = r != null ? r : 5.0;
                        item.comment = doc.getString("comment");
                        item.dateText = doc.getString("dateText");
                        Long ts = doc.getLong("timestamp");
                        item.timestamp = ts != null ? ts : 0L;
                        reviewList.add(item);
                    }

                    Collections.sort(reviewList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                    adapter.notifyDataSetChanged();
                    tvEmptyHistory.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReviewItem item = reviewList.get(position);
            holder.tvPlace.setText(item.placeName != null ? item.placeName : "Tempat Kuliner");
            holder.tvRating.setText("★ " + item.rating);
            holder.tvDate.setText(item.dateText != null ? item.dateText : "");
            holder.tvComment.setText(item.comment != null ? item.comment : "");

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Hapus Riwayat Ulasan")
                        .setMessage("Hapus ulasan untuk " + item.placeName + "?")
                        .setPositiveButton("Hapus", (d, w) -> {
                            db.collection("reviews").document(item.id).delete()
                                    .addOnSuccessListener(unused -> {
                                        if (isAdded()) {
                                            Toast.makeText(requireContext(), "Ulasan dihapus", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return reviewList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPlace, tvRating, tvDate, tvComment;
            Button btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPlace = itemView.findViewById(R.id.tvHistoryPlaceName);
                tvRating = itemView.findViewById(R.id.tvHistoryRating);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvComment = itemView.findViewById(R.id.tvHistoryComment);
                btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
            }
        }
    }
}