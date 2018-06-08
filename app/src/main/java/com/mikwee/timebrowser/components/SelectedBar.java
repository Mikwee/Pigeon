package com.mikwee.timebrowser.components;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.adapters.NewFileListAdapter;
import com.mikwee.timebrowser.fragments.BrowserFragment;
import com.mikwee.timebrowser.utils.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class SelectedBar {
    private static final String TAG = SelectedBar.class.getSimpleName();

    private BrowserFragment browserFragment;

    private ActionBar actionBar;
    private Menu menu;

    private LinearLayout layout;

    private List<FileInfo> selectedFiles = new ArrayList<>();

    //----------------------------------------- INIT -----------------------------------------------

    public SelectedBar(BrowserFragment browserFragment, View v) {
        this.browserFragment = browserFragment;
        this.actionBar = ((AppCompatActivity) browserFragment.getActivity()).getSupportActionBar();
        initViews(v);
    }

    //Init icons of out selected bar
    private void initViews(View v) {
        layout = (LinearLayout) v;
        v.findViewById(R.id.selection_info_dir).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        browserFragment.showFileInfo();
                    }
                }
        );
        v.findViewById(R.id.selection_download_dir).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        browserFragment.showDownloadChooser();
                    }
                }
        );
    }

    public void setActionBarMenu(Menu menu) {
        this.menu = menu;
    }

    //---------------------------

    //Reset actionbar and selectionBar
    public void hideFileSelection() {

        RecyclerView recyclerView = browserFragment.mRecyclerView;
        int size = recyclerView.getAdapter().getItemCount();

        //hide checkbox on every item
        for (int i = 0; i < size; i++) {
            NewFileListAdapter.ViewHolderGrid holder =
                    (NewFileListAdapter.ViewHolderGrid) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null)
                holder.hideSelection();
        }


        //reset list
        selectedFiles.clear();


        //Change Action bar appearance
        if (actionBar != null) {

            resetTitle();

            //set all icons to hidden
            setIconVisibility(true);

            //set "x" icon to invisible
            menu.findItem(R.id.action_close_selection).setVisible(false);

        }

        //show selected bar
        layout.setVisibility(View.GONE);

        //Hide ExplorerBar
        browserFragment.explorerBar.toggleExplorerBarVisibility(View.VISIBLE);
    }

    //Set Actionbar and selectionBar
    public void showFileSelection() {

        RecyclerView recyclerView = browserFragment.mRecyclerView;
        int size = recyclerView.getAdapter().getItemCount();

        //Show checkbox on every item
        for (int i = 0; i < size; i++) {
            NewFileListAdapter.ViewHolderGrid holder =
                    (NewFileListAdapter.ViewHolderGrid) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null)
                holder.showSelection();
        }

        //Change Action bar appearance
        if (actionBar != null) {

            updateSelectionTitle();
            //set all icons to hidden
            setIconVisibility(false);
            //set "x" icon to visible
            menu.findItem(R.id.action_close_selection).setVisible(true);

        }

        //show selected bar and hide explorer bar
        layout.setVisibility(View.VISIBLE);

        //Hide ExplorerBar
        browserFragment.explorerBar.toggleExplorerBarVisibility(View.GONE);

    }


    //---------------------------

    public void addSelectedFile(FileInfo file) {
        Log.e(TAG, "added " + file.fileName);

        if (!inSelection())
            showFileSelection();

        selectedFiles.add(file);

        updateSelectionTitle();

        Log.e(TAG, "size " + selectedFiles.size());
    }

    public void removeSelectedFile(FileInfo file) {

        Log.e(TAG, "removed " + file.fileName);

        selectedFiles.remove(file);
        updateSelectionTitle();

        if (selectedFiles.size() == 0)
            hideFileSelection();

        Log.e(TAG, "size " + selectedFiles.size());
    }

    //---------------------------

    private void resetTitle() {
        actionBar.setTitle(browserFragment.getString(R.string.app_name));
    }

    private void updateSelectionTitle() {

        String actionBarTitle = browserFragment.getString(R.string.item_selected);
        if (getSelectedNum() > 1)
            browserFragment.getString(R.string.items_selected);

        actionBar.setTitle(actionBarTitle + " " + getSelectedNum());
    }


    //Hide all action bar icon not needed for item selection
    private void setIconVisibility(boolean visible) {

        menu.findItem(R.id.action_upload).setVisible(visible);
        menu.findItem(R.id.action_sort).setVisible(visible);
        menu.findItem(R.id.action_wakkamole).setVisible(visible);
        menu.findItem(R.id.action_settings).setVisible(visible);
        menu.findItem(R.id.action_credits).setVisible(visible);

    }


    //-----------------------------------------GETTERS SETTERS--------------------------------------


    public boolean inSelection() {
        return selectedFiles.size() > 0;
    }

    private int getSelectedNum() {
        return selectedFiles.size();
    }

    public List<FileInfo> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileInfo> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

}
