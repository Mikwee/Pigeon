package com.mikwee.timebrowser.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.activities.MainActivity;
import com.mikwee.timebrowser.adapters.HostListAdapter;
import com.mikwee.timebrowser.utils.HostInfo;

import java.util.ArrayList;
import java.util.Arrays;


public class ScanResultFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();


    private RecyclerView mRecyclerView;
    private MainActivity activity;
    private TextView noMachinesTitle;
    private TextView machinesFoundTitle;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        activity = (MainActivity) getActivity();
        View v = inflater.inflate(R.layout.fragment_search_result, container, false);

        initViews(v);

        //Keep fragment over configuration changes
        setRetainInstance(true);

        // Inflate the layout for this fragment
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        //if fragment started with arguments to start scan
        if (getArguments() != null && getArguments().getSerializable("hosts") != null) {
            HostInfo[] ah = (HostInfo[]) getArguments().getSerializable("hosts");
            addHosts(new ArrayList<>(Arrays.asList(ah)));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the current article selection in case we need to recreate the fragment
    }

    //initialize host list layout views
    private void initViews(View v) {
        Button againButton = (Button) v.findViewById(R.id.button_again);
        againButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.loadScan();
            }
        });

        mRecyclerView = (RecyclerView) v.findViewById(R.id.hosts_list);

        int orientation = activity.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        } else {
            mRecyclerView.setLayoutManager(new GridLayoutManager(activity, 2));
        }

        noMachinesTitle = (TextView) v.findViewById(R.id.no_machines);
        machinesFoundTitle = (TextView) v.findViewById(R.id.search_title);
    }

    //Pass in a list of hosts and add them to the recyclerview
    public void addHosts(ArrayList<HostInfo> hosts) {
        if (hosts == null || hosts.size() == 0) {
            noMachinesTitle.setVisibility(View.VISIBLE);
            machinesFoundTitle.setVisibility(View.GONE);
        } else {
            HostListAdapter mAdapter = new HostListAdapter(hosts);
            mRecyclerView.setAdapter(mAdapter);
        }

    }


}
