package com.mikwee.timebrowser.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.receivers.ConnectivityChangeReceiver;


public class IntermediateFragment extends Fragment {
    //-------Views
    TextView title;
    ImageView image;
    ProgressBar progress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //Inflate the layout
        View v = inflater.inflate(R.layout.fragment_intermediate, container, false);
        //Initialize views
        initViews(v);

        //Retrieve the action passed
        ConnectivityChangeReceiver.INTERMEDIATE_ACTION action = (ConnectivityChangeReceiver.INTERMEDIATE_ACTION) getArguments().get("state");

        //Handle the new connection state
        handleStateChange(action);

        //Return inflated layout
        return v;
    }

    //Initialize the views
    private void initViews(View v) {
        title = (TextView) v.findViewById(R.id.intermediate_title);
        image = (ImageView) v.findViewById(R.id.intermediate_image);
        progress = (ProgressBar) v.findViewById(R.id.intermediate_progress);
    }

    //Display a certain layout based on the network state passed
    public void handleStateChange(ConnectivityChangeReceiver.INTERMEDIATE_ACTION state) {

        switch (state) {
            case CONNECTING:
                title.setText(getString(R.string.intermediate_connecting));
                progress.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                break;

            case DISCONNECTING:
                title.setText(getString(R.string.intermediate_disconnecting));
                progress.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                break;

            case WIFI_BUT_DISCONNECTED:
                title.setText(getString(R.string.intermediate_disconnected));
                progress.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                break;
        }
    }

}
