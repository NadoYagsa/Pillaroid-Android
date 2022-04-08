package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.data.FavoritesInfo;

import java.util.ArrayList;

public class FavoritesRecyclerAdapter extends RecyclerView.Adapter<FavoritesRecyclerAdapter.FavoritesViewHolder> {
    private Context context;
    private ArrayList<FavoritesInfo> favoritesList;

    public FavoritesRecyclerAdapter(ArrayList<FavoritesInfo> favoritesList) {
        this.favoritesList = favoritesList;
    }

    @NonNull
    @Override
    public FavoritesRecyclerAdapter.FavoritesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_favorites, parent, false);
        FavoritesRecyclerAdapter.FavoritesViewHolder viewHolder = new FavoritesRecyclerAdapter.FavoritesViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesRecyclerAdapter.FavoritesViewHolder holder, int position) {
        holder.tvMedicineName.setText(favoritesList.get(position).getMedicineName());
        
        //TODO: 즐겨찾기 이미지 클릭 시 즐겨찾기 해제됨(favoritesIdx 전달함)
    }

    @Override
    public int getItemCount() { return favoritesList.size(); }

    public class FavoritesViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton ibtStar;
        TextView tvMedicineName;

        FavoritesViewHolder(final View itemView) {
            super(itemView);

            ibtStar = itemView.findViewById(R.id.ibt_item_favorites_star);
            tvMedicineName = itemView.findViewById(R.id.tv_item_favorites_name);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    //TODO: 약의 세부 내용 보여줌
                }
            });
        }
    }
}
