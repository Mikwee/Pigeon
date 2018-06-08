package com.mikwee.timebrowser.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.activities.SmbActivity;
import com.mikwee.timebrowser.utils.HostInfo;

import java.util.ArrayList;

/**
 * Created by mikwe on 06/10/2017.
 */

public class HostListAdapter extends RecyclerView.Adapter<HostListAdapter.ViewHolder> {
    private static final String TAG = "HostListAdapter";

    private ArrayList<HostInfo> mDataSet;

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView ip;
        TextView mac;
        TextView passwordRequired;

        Context c;

        HostInfo host;
        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity();
                }
            });

            c = v.getContext();
            name = (TextView) v.findViewById(R.id.name);
            ip = (TextView) v.findViewById(R.id.ip);
            mac = (TextView) v.findViewById(R.id.mac);
            passwordRequired = (TextView) v.findViewById(R.id.pass_required);
        }

        void startActivity(){
            Intent i = new Intent(c, SmbActivity.class);
            i.putExtra("host", host);
            c.startActivity(i);
        }

        public void setInfo(HostInfo hi) {
            host= hi;
            name.setText(hi.getName());
            ip.setText(hi.getIp());
            mac.setText(hi.getMac());
            if (hi.isPasswordRequired()) {
                passwordRequired.setText(c.getString(R.string.requires_pass_true));
                passwordRequired.setBackgroundColor(c.getResources().getColor(R.color.colorNegative));
            } else {
                passwordRequired.setText(c.getString(R.string.requires_pass_false));
                passwordRequired.setBackgroundColor(c.getResources().getColor(R.color.colorPositive));
            }
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used by RecyclerView.
     */
    public HostListAdapter(ArrayList<HostInfo> dataSet) {
        mDataSet = dataSet;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.search_result_item, viewGroup, false);

        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.setInfo(mDataSet.get(position));
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}