package com.fungames.privateservercreationtool;

/**
 * Created by Fabian on 24.11.2017.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TexturesFragment extends Fragment {

    public TexturesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_textures, container, false);


        return rootView;
    }

}