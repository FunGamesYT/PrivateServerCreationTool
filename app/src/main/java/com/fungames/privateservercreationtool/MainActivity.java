package com.fungames.privateservercreationtool;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fungames.privateservercreationtool.ScLib.Textures;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final int FILE_OPEN_DIALOG = 123;
    private String[] mNavigationDrawerItemTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    Toolbar toolbar;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    android.support.v7.app.ActionBarDrawerToggle mDrawerToggle;
    private String selectedApkPath;
    private String selectedApkFileName;
    private ApkFragment apkFragment;
    private CardStatsFragment cardStatsFragment;
    private TexturesFragment texturesFragment;
    private Decompress decompress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.apkFragment = new ApkFragment();
        this.cardStatsFragment = new CardStatsFragment();
        this.texturesFragment = new TexturesFragment();

        mTitle = mDrawerTitle = getTitle();
        mNavigationDrawerItemTitles = getResources().getStringArray(R.array.navigation_drawer_items_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        setupToolbar();

        DataModel[] drawerItem = new DataModel[3];

        drawerItem[0] = new DataModel(R.drawable.connect, "Apk");
        drawerItem[1] = new DataModel(R.drawable.fixtures, "Card Stats");
        drawerItem[2] = new DataModel(R.drawable.table, "Textures");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);
        decompress = new Decompress(this);
        decompress.readFromSharedPreference();

        DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(this, R.layout.list_view_item_row, drawerItem);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        setupDrawerToggle();
    }

    private void selectItem(int position) {

        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = apkFragment;
                break;
            case 1:
                fragment = cardStatsFragment;
                if(decompress != null) {
                    cardStatsFragment.setCardStatsItems(decompress.getCardStatsItems());
                }
                else {
                    cardStatsFragment.setCardStatsItems(null);
                }
                break;
            case 2:
                fragment = texturesFragment;
                if(decompress != null) {
                    texturesFragment.setTexturesItems(decompress.getTexturesItems());
                }
                else {
                    texturesFragment.setTexturesItems(null);
                }
                break;
            default:
                break;
        }
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            setTitle(mNavigationDrawerItemTitles[position]);
            mDrawerLayout.closeDrawer(mDrawerList);
        } else {
            Log.e("MainActivity", "Error in creating fragment");
        }
        if (position == 1) {
        }
    }

    public class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public void selectapk(View v) {
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("application/vnd.android.package-archive"); // intent type to filter application based on your requirement
        startActivityForResult(fileIntent, FILE_OPEN_DIALOG);

    }

    public void decrypt(View v){
        ProgressBar decryptProgressBar = (ProgressBar) findViewById(R.id.decryptProgressBar);
        decryptProgressBar.setVisibility(ProgressBar.VISIBLE);
        TextView currentFile = (TextView) findViewById(R.id.currentFile);
        decompress.execute(selectedApkPath, selectedApkFileName);
    }

    public void compress(View v){
        File srcFile = new File(Environment.getExternalStorageDirectory(), ".PSCT/");
        File[] files = srcFile.listFiles();
        if (files != null) {
            List<FileInfo> dirList = new ArrayList<>();
            for (File file : files) {
                if (file.isDirectory()) {
                    dirList.add(new FileInfo(file));
                }
            }
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.directory_list);
            dialog.setTitle("Which apk you want to create?");
            ListView apkList = (ListView) dialog.findViewById(R.id.dirList);
            ArrayAdapter<FileInfo> adapter = new ArrayAdapter<FileInfo>(this,
                    android.R.layout.simple_list_item_2, android.R.id.text1, dirList.toArray(new FileInfo[0]));
            apkList.setAdapter(adapter);
            dialog.show();
            apkList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Adapter adapter = adapterView.getAdapter();
                    FileInfo item = (FileInfo) adapter.getItem(i);
                    ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setMessage("Creating " + item.getFileName());
                    progressDialog.setTitle(item.getFileName());
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgressNumberFormat(null);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    Compress compress = new Compress();
                    compress.execute(item, progressDialog);
                    dialog.dismiss();
                }
            });
        } else {
            // no files were found or this is not a directory
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_OPEN_DIALOG && resultCode == RESULT_OK) {
            selectedApkPath = FileUtils.getPath(this, data.getData());
            String[] pathElement = selectedApkPath.split("/");
            selectedApkFileName = pathElement[pathElement.length - 1];
            TextView selectedFile = (TextView) findViewById(R.id.selectedFile);
            selectedFile.setText(selectedApkFileName);
            Button decompressButton = (Button) findViewById(R.id.decompressButton);
            decompressButton.setEnabled(true);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    void setupDrawerToggle() {
        mDrawerToggle = new android.support.v7.app.ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
        //This is necessary to change the icon of the Drawer Toggle upon state change.
        mDrawerToggle.syncState();
    }



}
