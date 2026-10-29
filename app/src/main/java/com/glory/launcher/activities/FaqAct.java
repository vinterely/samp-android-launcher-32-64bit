package com.glory.launcher.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glory.game.R;
import com.glory.launcher.network.Movie;
import com.glory.launcher.network.MovieAdapter;

import java.util.ArrayList;
import java.util.List;

public class FaqAct extends AppCompatActivity {

    RecyclerView recyclerView;
    List<Movie> movieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_help);

        recyclerView = findViewById(R.id.faq_recycler);


        ImageButton btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

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
        onEnterAnimationComplete();

        movieList.add(new Movie("BRILLIANT RP",
                "BRILLIANT ROLE PLAY"));

        movieList.add(new Movie("BRILLIANT RP",
                "BRILLIANT MOBILE â€” Ð£Ð”ÐžÐ‘ÐÐ«Ð™ Ð˜ Ð¡Ð¢ÐÐ‘Ð˜Ð›Ð¬ÐÐ«Ð™ Ð›ÐÐ£ÐÐ§Ð•Ð . ÐŸÐ ÐžÐ¡Ð¢ÐžÐ™ Ð—ÐÐŸÐ£Ð¡Ðš, ÐšÐ ÐÐ¡Ð˜Ð’Ð«Ð™ Ð˜ÐÐ¢Ð•Ð Ð¤Ð•Ð™Ð¡ Ð˜ Ð’Ð«Ð¡ÐžÐšÐÐ¯ ÐÐÐ”ÐÐ–ÐÐžÐ¡Ð¢Ð¬."));

        movieList.add(new Movie("BRILLIANT RP",
                "ÐšÐžÐœÐÐÐ”Ð BRILLIANT RP."));

        movieList.add(new Movie("Brilliant Mobile",
                "Ð’Ð¡Ð, Ð§Ð¢Ðž ÐÐ£Ð–ÐÐž Ð”Ð›Ð¯ Ð¡ÐžÐ’Ð Ð•ÐœÐ•ÐÐÐžÐ“Ðž ÐŸÐ ÐžÐ•ÐšÐ¢Ð â€” Ð’ ÐžÐ”ÐÐžÐœ Ð›ÐÐ£ÐÐ§Ð•Ð Ð•. ÐŸÐ ÐžÐ¡Ð¢Ðž, Ð¡Ð¢Ð˜Ð›Ð¬ÐÐž, ÐÐÐ”ÐÐ–ÐÐž."));
    }
}
