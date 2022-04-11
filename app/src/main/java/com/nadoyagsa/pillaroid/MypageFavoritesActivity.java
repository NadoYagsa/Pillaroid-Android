package com.nadoyagsa.pillaroid;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.adapter.FavoritesRecyclerAdapter;
import com.nadoyagsa.pillaroid.data.FavoritesInfo;

import java.util.ArrayList;
import java.util.Objects;

public class MypageFavoritesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage_favorites);

        Toolbar toolbar = findViewById(R.id.tb_favorites_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_mypage_favorites, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        ArrayList<FavoritesInfo> favoritesInfos = new ArrayList<>();
        favoritesInfos.add(new FavoritesInfo(1L, "타이레놀정 160mg"));
        favoritesInfos.add(new FavoritesInfo(2L, "인사돌플러스정"));

        RecyclerView rvFavorites = findViewById(R.id.rv_favorites_list);
        LinearLayoutManager favoritesManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        rvFavorites.setLayoutManager(favoritesManager);
        FavoritesRecyclerAdapter favoritesAdapter = new FavoritesRecyclerAdapter(favoritesInfos);
        rvFavorites.setAdapter(favoritesAdapter);
        DividerItemDecoration devider = new DividerItemDecoration(this, 1);
        devider.setDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divide_bar, null)));
        rvFavorites.addItemDecoration(devider);
    }
}
