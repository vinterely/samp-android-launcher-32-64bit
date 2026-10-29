package com.glory.launcher.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glory.game.R;
import com.glory.launcher.network.Movie;
import com.glory.launcher.network.MovieAdapter;

import java.util.ArrayList;
import java.util.List;

public class Faq extends AppCompatActivity {

    RecyclerView recyclerView;

    List<Movie> movieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.faq);

        recyclerView = findViewById(R.id.recyclerView);

        initData();
        initRecyclerView();
    }

    private void initRecyclerView() {
        MovieAdapter movieAdapter = new MovieAdapter(movieList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(movieAdapter);
    }

    private void initData() {
        movieList = new ArrayList<>();
        movieList.add(new Movie("ÐŸÐžÐžÐž",   "After being held captive in an Afghan cave, billionaire engineer Tony Stark creates a unique weaponized suit of armor to fight evil."));
          }


}
