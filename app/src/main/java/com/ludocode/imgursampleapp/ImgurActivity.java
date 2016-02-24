package com.ludocode.imgursampleapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

public class ImgurActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ImgurActivity";

    // For now the list of galleries and subreddits are hardcoded. We
    // could instead add them procedurally or allow the user to input
    // a subreddit.
    private static final String galleryViral = "https://api.imgur.com/3/gallery/hot/viral/";
    private static final String galleryMemes = "https://api.imgur.com/3/g/memes/viral/";
    private static final String redditAww = "https://api.imgur.com/3/gallery/r/aww/time/";
    private static final String redditLEGO = "https://api.imgur.com/3/gallery/r/LEGO/time/";
    private static final String redditAquariums = "https://api.imgur.com/3/gallery/r/Aquariums/time/";
    private static final String redditMinecraft = "https://api.imgur.com/3/gallery/r/Minecraft/time/";

    private ImgurAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private StaggeredGridLayoutManager mStaggeredGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgur_viewer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mStaggeredGrid = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(mStaggeredGrid);

        mAdapter = new ImgurAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.loadGallery(galleryViral);

        FloatingActionButton refresh = (FloatingActionButton) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.refresh();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_gallery) {
            mAdapter.loadGallery(galleryViral);
        } else if (id == R.id.nav_memes) {
            mAdapter.loadGallery(galleryMemes);
        } else if (id == R.id.nav_reddit_aww) {
            mAdapter.loadGallery(redditAww);
        } else if (id == R.id.nav_reddit_lego) {
            mAdapter.loadGallery(redditLEGO);
        } else if (id == R.id.nav_reddit_aquariums) {
            mAdapter.loadGallery(redditAquariums);
        } else if (id == R.id.nav_reddit_minecraft) {
            mAdapter.loadGallery(redditMinecraft);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onDataReset() {
        // StaggeredGridLayoutManager has a bug where it incorrectly retains
        // cached spans when resetting the data. We let it think it has been
        // detached as a workaround which correctly scrolls to top.
        //     http://stackoverflow.com/a/34444654
        mStaggeredGrid.onDetachedFromWindow(mRecyclerView, null);
    }
}
