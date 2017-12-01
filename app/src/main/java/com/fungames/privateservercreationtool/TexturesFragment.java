package com.fungames.privateservercreationtool;

/**
 * Created by Fabian on 24.11.2017.
 */

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fungames.privateservercreationtool.ScLib.TextureException;
import com.fungames.privateservercreationtool.ScLib.Textures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class TexturesFragment extends Fragment {

    private ArrayList<FileInfo> texturesItems;

    public TexturesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_textures, container, false);


        return rootView;
    }

    public void setTexturesItems(ArrayList<FileInfo> cardStatsItems) {
        this.texturesItems = cardStatsItems;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView texturesList = (ListView) getView().findViewById(R.id.texturesList);
        texturesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                FileInfo item = (FileInfo) adapter.getItem(i);
                File scFile = new File(item.getFilePath());
                try {
                    FileInputStream inStream = new FileInputStream(scFile);
                    String suffix = "";
                    do {
                        Bitmap bitmap = Textures.getBitmapBySc(inStream);
                        if (bitmap == null) {
                            break;
                        }
                        File targetFile = new File(Environment.getExternalStorageDirectory(), "ExtractedPNGs/" + item.getFileName() + suffix + ".png");
                        FileOutputStream out = new FileOutputStream(targetFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.close();
                        suffix += "_";
                    } while (inStream.available() > 0);
                    inStream.close();
                    Toast.makeText(getContext(), item.getFilePath(), Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    Log.e("Textures", String.format("Failed to read file '%s': %s", item.getFilePath(), e.getMessage()));
                    // TODO create toast in error case
                } catch (TextureException e) {
                    Log.e("Textures", String.format("Failed to read texture '%s': %s", item.getFilePath(), e.getMessage()));
                    // TODO create toast in error case
                } catch (IOException e) {
                    Log.e("Textures", String.format("Failed to read file '%s': %s", item.getFilePath(), e.getMessage()));
                    // TODO create toast in error case
                }
            }
        });
        if (texturesItems != null) {

            ArrayAdapter<FileInfo> adapter = new ArrayAdapter<FileInfo>(texturesList.getContext(),
                    android.R.layout.simple_list_item_1, android.R.id.text1, texturesItems.toArray(new FileInfo[0]));
            texturesList.setAdapter(adapter);
        }
        else {
            View relativeLayout =  getView().findViewById(R.id.rltextures);
            TextView valueTV = new TextView(getView().getContext());
            valueTV.setText("Please select an apk before you edit textures");
            valueTV.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT));
            valueTV.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            ((RelativeLayout) relativeLayout).addView(valueTV);
        }
    }
}
