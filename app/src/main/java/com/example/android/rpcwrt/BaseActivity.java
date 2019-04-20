package com.example.android.rpcwrt;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.Stream;

public class BaseActivity extends AppCompatActivity {

    public static final String LUCI_RPC_PATH = "/cgi-bin/luci/rpc/";
    public static final String OUI_PATH = "oui.txt";

    protected ProgressBar progressBar;
    protected Button retryButton;
    protected Button logOutButton;
    protected JSONRPC2Session session;
    protected JSONRPC2Request request;
    protected JSONRPC2Response response;

    protected static String baseUrl = "";
    protected static String token = "";
    protected static Boolean isLoggedIn = false;
    protected static SparseArray<String> oui;

    private Toolbar mToolbar;

    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;

    private boolean mToolbarInitialized;

    private int mItemToOpenWhenDrawerCloses = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                    "the end of your onCreate method");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        getFragmentManager().addOnBackStackChangedListener(backStackChangedListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getFragmentManager().removeOnBackStackChangedListener(backStackChangedListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        // If the drawer is open, back will close it
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                if (LoginActivity.class.isAssignableFrom(getClass()))
                    System.exit(0);
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce=false;
                }
            }, 2000);
        }
    }

    private final FragmentManager.OnBackStackChangedListener backStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    updateDrawerToggle();
                }
            };

    protected void updateDrawerToggle() {
        if (mDrawerToggle == null) {
            return;
        }
        boolean isRoot = getFragmentManager().getBackStackEntryCount() == 0;
        mDrawerToggle.setDrawerIndicatorEnabled(isRoot);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }
        if (isRoot) {
            mDrawerToggle.syncState();
        }
    }

    protected void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " +
                    "'toolbar'");
        }

//        mToolbar.inflateMenu(R.menu.main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView == null) {
                throw new IllegalStateException("Layout requires a NavigationView " +
                        "with id 'nav_view'");
            }

            // Create an ActionBarDrawerToggle that will handle opening/closing of the drawer:
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                    mToolbar, R.string.open_content_drawer, R.string.close_content_drawer);
            mDrawerLayout.setDrawerListener(drawerListener);
            populateDrawerItems(navigationView);
            setSupportActionBar(mToolbar);
            updateDrawerToggle();
        } else {
            setSupportActionBar(mToolbar);
        }

        mToolbarInitialized = true;
    }

    private final DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerOpened(drawerView);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.app_name);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerClosed(drawerView);
            if (mItemToOpenWhenDrawerCloses >= 0) {
                Bundle extras = ActivityOptions.makeCustomAnimation(
                        BaseActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();
                Class activityClass = null;
                switch (mItemToOpenWhenDrawerCloses) {
                    case R.id.navigation_overview:
                        activityClass = OverviewActivity.class;
                        break;
                    case R.id.navigation_syslog:
                        activityClass = SystemLogActivity.class;
                        break;
                    case R.id.navigation_kernellog:
                        activityClass = KernelLogActivity.class;
                        break;
                    case R.id.navigation_wifi:
                        activityClass = WiFiActivity.class;
                        break;
                    case R.id.navigation_neighbor:
                        activityClass = NeighborActivity.class;
                        break;
                    case R.id.navigation_opkg:
                        activityClass = OpkgListActivity.class;
                        break;
                    case R.id.navigation_reboot:
                        activityClass = RebootActivity.class;
                        break;
                    case R.id.navigation_login:
                        activityClass = LoginActivity.class;
                        break;
                }
                if (activityClass != null) {
                    startActivity(new Intent(BaseActivity.this, activityClass), extras);
                    finish();
                }
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerStateChanged(newState);
        }
    };

    protected void populateDrawerItems(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        if (!isLoggedIn) {
                            Toast.makeText(BaseActivity.this, "Please log in", Toast.LENGTH_SHORT).show();
                            mDrawerLayout.closeDrawers();
                            return false;
                        }
                        menuItem.setChecked(true);
                        mItemToOpenWhenDrawerCloses = menuItem.getItemId();
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
        if (OverviewActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_overview);
        } else if (SystemLogActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_syslog);
        } else if (KernelLogActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_kernellog);
        } else if (NeighborActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_neighbor);
        } else if (WiFiActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_wifi);
        } else if (OpkgListActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_opkg);
        } else if (RebootActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_reboot);
        } else if (LoginActivity.class.isAssignableFrom(getClass())) {
            navigationView.setCheckedItem(R.id.navigation_login);
        }
    }

    protected void doLogout() {
        // Invalidate session, do not clear URL
        token = "";
        isLoggedIn = false;
        // Toast.makeText(BaseActivity.this, "Logging out...", Toast.LENGTH_SHORT).show();
        // Go back to Login page
        startActivity(new Intent(BaseActivity.this, LoginActivity.class));
        finish();
    }

    protected void showFailButtons () {
        retryButton.setVisibility(View.VISIBLE);
        logOutButton.setVisibility(View.VISIBLE);
    }

    protected void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    protected void hideFailButtons() {
        retryButton.setVisibility(View.GONE);
        logOutButton.setVisibility(View.GONE);
    }

    protected void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    protected void printFailMessage() {
        Toast.makeText(BaseActivity.this, "Invalid response,\n Maybe connection problem, missing RPC packages or session expired",Toast.LENGTH_SHORT).show();
    }

    protected static void initializeOUI(Activity activity) {
        if (oui != null) return;
        oui = new SparseArray<>();
        try {
            DataInputStream textFileStream = new DataInputStream(activity.getAssets().open(OUI_PATH));
            Scanner scanner = new Scanner(textFileStream);

            while (scanner.hasNextLine()) {
                String[] columns = scanner.nextLine().split("\t");
                oui.put(Integer.parseInt(columns[0], 16), columns[1]);
            }

            textFileStream.close();
        } catch (IOException e) {
            // The file is missing, exit program
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Nullable
    protected static String queryOUI(String mac) {
        String s = mac.substring(0, 8);
        s = s.replace(":", "");
        return oui.get(Integer.parseInt(s, 16));
    }
}
