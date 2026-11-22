package com.example.jejakrasa.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.jejakrasa.R;
import com.example.jejakrasa.model.CulinarySpot;

import java.util.ArrayList;
import java.util.List;

public class CulinarySpotAdapter extends RecyclerView.Adapter<CulinarySpotAdapter.SpotViewHolder> {

    public interface OnSpotClickListener {
        void onSpotClick(CulinarySpot spot);
    }

    private List<CulinarySpot> spotList;
    private final OnSpotClickListener listener;
    private int lastAnimatedPosition = -1;

    public CulinarySpotAdapter(List<CulinarySpot> spotList, OnSpotClickListener listener) {
        this.spotList = new ArrayList<>(spotList);
        this.listener = listener;
    }

    public void updateData(List<CulinarySpot> newList) {
        this.spotList = new ArrayList<>(newList);
        this.lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_culinary_spot, parent, false);
        return new SpotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpotViewHolder holder, int position) {
        CulinarySpot spot = spotList.get(position);
        holder.tvNamaSpot.setText(spot.getNamaTempat());
        holder.tvKategoriSpot.setText(spot.getKategori() != null ? spot.getKategori().toUpperCase() : "KULINER");
        holder.tvBadgeLegendaris.setText("Sejak " + spot.getTahunBerdiri());
        holder.tvBadgeRating.setText("★ " + spot.getRating());
        holder.tvAlamatSpot.setText(spot.getAlamatSingkat() + " • " + spot.getKisaranHarga());

        Glide.with(holder.itemView.getContext())
                .load(spot.getImageUrl())
                .placeholder(R.drawable.bg_placeholder_food)
                .error(R.drawable.bg_placeholder_food)
                .centerCrop()
                .into(holder.ivSpotImage);

        if (position > lastAnimatedPosition) {
            Animation anim = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_fade_slide);
            holder.itemView.startAnimation(anim);
            lastAnimatedPosition = position;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSpotClick(spot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return spotList.size();
    }

    static class SpotViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSpotImage;
        TextView tvBadgeLegendaris, tvBadgeRating, tvKategoriSpot, tvNamaSpot, tvAlamatSpot;

        public SpotViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSpotImage = itemView.findViewById(R.id.ivSpotImage);
            tvBadgeLegendaris = itemView.findViewById(R.id.tvBadgeLegendaris);
            tvBadgeRating = itemView.findViewById(R.id.tvBadgeRating);
            tvKategoriSpot = itemView.findViewById(R.id.tvKategoriSpot);
            tvNamaSpot = itemView.findViewById(R.id.tvNamaSpot);
            tvAlamatSpot = itemView.findViewById(R.id.tvAlamatSpot);
        }
    }
}