package com.fungames.privateservercreationtool;

/**
 * Created by Fabian on 24.11.2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class CardStatsFragment extends Fragment {
    private ArrayList<CardStatsItem> cardStatsItems = new ArrayList<>();

    public CardStatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_cardstats, container, false);
        return rootView;
    }

    public void setCardStatsItems(ArrayList<CardStatsItem> cardStatsItems) {
        this.cardStatsItems = cardStatsItems;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView csvList = (ListView) getView().findViewById(R.id.csvList);
        csvList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Adapter adapter = adapterView.getAdapter();
                CardStatsItem item = (CardStatsItem) adapter.getItem(i);
                Toast.makeText(getContext(), item.getFilePath(), Toast.LENGTH_SHORT).show();
            }
        });
        ArrayAdapter<CardStatsItem> adapter = new ArrayAdapter<CardStatsItem>(csvList.getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, cardStatsItems.toArray(new CardStatsItem[0]));
        csvList.setAdapter(adapter);
    }

}
