package com.android.music.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.appthemehelper.util.NavigationViewUtil;
import com.android.music.App;
import com.android.music.R;
import com.android.music.dialogs.ScanMediaFolderChooserDialog;
import com.android.music.glide.SongGlideRequest;
import com.android.music.helper.MusicPlayerRemote;
import com.android.music.helper.SearchQueryHelper;
import com.android.music.loader.AlbumLoader;
import com.android.music.loader.ArtistLoader;
import com.android.music.loader.PlaylistSongLoader;
import com.android.music.model.Song;
import com.android.music.service.MusicService;
import com.android.music.ui.activities.base.AbsSlidingMusicPanelActivity;
import com.android.music.ui.activities.intro.AppIntroActivity;
import com.android.music.ui.fragments.mainactivity.folders.FoldersFragment;
import com.android.music.ui.fragments.mainactivity.library.LibraryFragment;
import com.android.music.util.PreferenceUtil;
import com.android.music.util.Util;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AbsSlidingMusicPanelActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int APP_INTRO_REQUEST = 100;
    public static final int PURCHASE_REQUEST = 101;

    private static final int LIBRARY = 0;
    private static final int FOLDERS = 1;

    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Nullable
    MainActivityFragmentCallbacks currentFragment;

    @Nullable
    private View navigationDrawerHeader;

    private boolean blockRequestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            Util.setStatusBarTranslucent(getWindow());
            drawerLayout.setFitsSystemWindows(false);
            navigationView.setFitsSystemWindows(false);
            //noinspection ConstantConditions
            findViewById(R.id.drawer_content_container).setFitsSystemWindows(false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawerLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                navigationView.dispatchApplyWindowInsets(windowInsets);
                return windowInsets.replaceSystemWindowInsets(0, 0, 0, 0);
            });
        }

        setUpDrawerLayout();

        if (savedInstanceState == null) {
            setMusicChooser(PreferenceUtil.getInstance(this).getLastMusicChooser());
        } else {
            restoreCurrentFragment();
        }

        if (!checkShowIntro()) {
        }
    }

    private void setMusicChooser(int key) {
        if (key == FOLDERS) {
            key = LIBRARY;
        }

        PreferenceUtil.getInstance(this).setLastMusicChooser(key);
        switch (key) {
            case LIBRARY:
                navigationView.setCheckedItem(R.id.nav_library);
                setCurrentFragment(LibraryFragment.newInstance());
                break;
            case FOLDERS:
                navigationView.setCheckedItem(R.id.nav_folders);
                setCurrentFragment(FoldersFragment.newInstance(this));
                break;
        }
    }

    private void setCurrentFragment(@SuppressWarnings("NullableProblems") Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, null).commit();
        currentFragment = (MainActivityFragmentCallbacks) fragment;
    }

    private void restoreCurrentFragment() {
        currentFragment = (MainActivityFragmentCallbacks) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false;
            if (!hasPermissions()) {
                requestPermissions();
            }
        }
    }

    @Override
    protected void requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions();
    }

    @Override
    protected View createContentView() {
        @SuppressLint("InflateParams")
        View contentView = getLayoutInflater().inflate(R.layout.activity_main_drawer_layout, null);
        ViewGroup drawerContent = ButterKnife.findById(contentView, R.id.drawer_content_container);
        drawerContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    private void setUpNavigationView() {
        int accentColor = ThemeStore.accentColor(this);
        NavigationViewUtil.setItemIconColors(navigationView, ATHUtil.resolveColor(this, R.attr.iconColor, ThemeStore.textColorSecondary(this)), accentColor);
        NavigationViewUtil.setItemTextColors(navigationView, ThemeStore.textColorPrimary(this), accentColor);

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            drawerLayout.closeDrawers();
            switch (menuItem.getItemId()) {
                case R.id.nav_library:
                    new Handler().postDelayed(() -> setMusicChooser(LIBRARY), 200);
                    break;
                case R.id.nav_folders:
                    new Handler().postDelayed(() -> setMusicChooser(FOLDERS), 200);
                case R.id.action_scan:
                    new Handler().postDelayed(() -> {
                        ScanMediaFolderChooserDialog dialog = ScanMediaFolderChooserDialog.create();
                        dialog.show(getSupportFragmentManager(), "SCAN_MEDIA_FOLDER_CHOOSER");
                    }, 200);
                    break;
                case R.id.nav_settings:
                    new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)), 200);
                    break;
                case R.id.nav_about:
                    new Handler().postDelayed(() -> startActivity(new Intent(MainActivity.this, AboutActivity.class)), 200);
                    break;
            }
            return true;
        });
    }

    private void setUpDrawerLayout() {
        setUpNavigationView();
    }

    private void updateNavigationDrawerHeader() {
        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            Song song = MusicPlayerRemote.getCurrentSong();
            if (navigationDrawerHeader == null) {
                navigationDrawerHeader = navigationView.inflateHeaderView(R.layout.navigation_drawer_header);
                //noinspection ConstantConditions
                navigationDrawerHeader.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    if (getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        expandPanel();
                    }
                });
            }
            ((TextView) navigationDrawerHeader.findViewById(R.id.title)).setText(song.title);
            ((TextView) navigationDrawerHeader.findViewById(R.id.text)).setText(song.artistName);
            SongGlideRequest.Builder.from(Glide.with(this), song)
                    .checkIgnoreMediaStore(this).build()
                    .into(((ImageView) navigationDrawerHeader.findViewById(R.id.image)));
        } else {
            if (navigationDrawerHeader != null) {
                navigationView.removeHeaderView(navigationDrawerHeader);
                navigationDrawerHeader = null;
            }
        }
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        updateNavigationDrawerHeader();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        updateNavigationDrawerHeader();
        handlePlaybackIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView);
            } else {
                drawerLayout.openDrawer(navigationView);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleBackPress() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
            return true;
        }
        return super.handleBackPress() || (currentFragment != null && currentFragment.handleBackPress());
    }

    private void handlePlaybackIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (intent.getAction() != null && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            final ArrayList<Song> songs = SearchQueryHelper.getSongs(this, intent.getExtras());
            if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
                MusicPlayerRemote.openAndShuffleQueue(songs, true);
            } else {
                MusicPlayerRemote.openQueue(songs, 0, true);
            }
            handled = true;
        }

        if (uri != null && uri.toString().length() > 0) {
            MusicPlayerRemote.playFromUri(uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "playlistId", "playlist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                ArrayList<Song> songs = new ArrayList<>();
                songs.addAll(PlaylistSongLoader.getPlaylistSongList(this, id));
                MusicPlayerRemote.openQueue(songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "albumId", "album");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(AlbumLoader.getAlbum(this, id).songs, position, true);
                handled = true;
            }
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
            final int id = (int) parseIdFromIntent(intent, "artistId", "artist");
            if (id >= 0) {
                int position = intent.getIntExtra("position", 0);
                MusicPlayerRemote.openQueue(ArtistLoader.getArtist(this, id).getSongs(), position, true);
                handled = true;
            }
        }
        if (handled) {
            setIntent(new Intent());
        }
    }

    private long parseIdFromIntent(@NonNull Intent intent, String longKey,
                                   String stringKey) {
        long id = intent.getLongExtra(longKey, -1);
        if (id < 0) {
            String idString = intent.getStringExtra(stringKey);
            if (idString != null) {
                try {
                    id = Long.parseLong(idString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return id;
    }

    @Override
    public void onPanelExpanded(View view) {
        super.onPanelExpanded(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void onPanelCollapsed(View view) {
        super.onPanelCollapsed(view);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private boolean checkShowIntro() {
        if (!PreferenceUtil.getInstance(this).introShown()) {
            PreferenceUtil.getInstance(this).setIntroShown();
            blockRequestPermissions = true;
            new Handler().postDelayed(() -> startActivityForResult(new Intent(MainActivity.this, AppIntroActivity.class), APP_INTRO_REQUEST), 50);
            return true;
        }
        return false;
    }

    public interface MainActivityFragmentCallbacks {
        boolean handleBackPress();
    }
}