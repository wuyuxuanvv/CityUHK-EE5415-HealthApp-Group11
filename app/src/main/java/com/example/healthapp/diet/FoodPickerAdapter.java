package com.example.healthapp.diet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthapp.R;
import com.example.healthapp.model.Food;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Three-column picker for the food library in the add-meal dialog. */
final class FoodPickerAdapter extends RecyclerView.Adapter<FoodPickerAdapter.FoodViewHolder> {

    interface OnFoodSelectedListener {
        void onFoodSelected(Food food);
    }

    private final Context context;
    private final OnFoodSelectedListener listener;
    private final List<Food> foods = new ArrayList<>();
    private int selectedPosition = RecyclerView.NO_POSITION;

    FoodPickerAdapter(Context context, OnFoodSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    void replaceFoods(List<Food> foods) {
        this.foods.clear();
        this.foods.addAll(foods);
        selectedPosition = this.foods.isEmpty() ? RecyclerView.NO_POSITION : 0;
        notifyDataSetChanged();
    }

    Food getSelectedFood() {
        return selectedPosition == RecyclerView.NO_POSITION ? null : foods.get(selectedPosition);
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_picker, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = foods.get(position);
        boolean selected = position == selectedPosition;
        holder.card.setCardBackgroundColor(ContextCompat.getColor(context,
                selected ? R.color.health_primary_container : R.color.health_surface));
        holder.card.setStrokeColor(ContextCompat.getColor(context, R.color.health_outline));
        holder.card.setChecked(selected);
        holder.image.setImageBitmap(FoodArtworkFactory.artworkFor(context, food));
        holder.name.setText(food.name);
        holder.calories.setText(context.getString(R.string.diet_food_calories_per_100g,
                String.format(Locale.getDefault(), "%,.0f", food.caloriesPer100g)));
        holder.card.setOnClickListener(v -> select(position));
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    private void select(int position) {
        if (position == selectedPosition) {
            return;
        }
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        notifyItemChanged(selectedPosition);
        listener.onFoodSelected(foods.get(selectedPosition));
    }

    static final class FoodViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView image;
        final TextView name;
        final TextView calories;

        FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            image = itemView.findViewById(R.id.image_food_picker);
            name = itemView.findViewById(R.id.text_food_picker_name);
            calories = itemView.findViewById(R.id.text_food_picker_calories);
        }
    }
}
