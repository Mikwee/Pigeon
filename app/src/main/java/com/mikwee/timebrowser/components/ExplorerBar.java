package com.mikwee.timebrowser.components;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.fragments.BrowserFragment;
import com.mikwee.timebrowser.utils.Utils;

import java.util.ArrayList;

public class ExplorerBar {
    public static final String TAG = ExplorerBar.class.getSimpleName();
    private BrowserFragment bf;

    private LinearLayout.LayoutParams explorerParams;
    private LinearLayout explorerLinearL;
    private HorizontalScrollView explorerScroll;
    private ImageView backDir;


    private ArrayList<String> dirNames = new ArrayList<>();


    public ExplorerBar(BrowserFragment fragment, View v) {
        this.bf = fragment;

        //Create layout params
        explorerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        explorerParams.gravity = Gravity.CENTER;

        initViews(v);
    }


    private void initViews(View v) {

        //Set On click listener to back arrow
        backDir = (ImageView) v.findViewById(R.id.back_dir);
        backDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bf.goBackTo(Utils.getDepth(bf.currentPath) - 1);
            }
        });

        explorerLinearL = (LinearLayout) v.findViewById(R.id.explorer_bar_ll);
        explorerScroll = (HorizontalScrollView) v.findViewById(R.id.horizontal_explorer);

    }


    //decides to move foreward or backward
    public void updateExplorerBar() {

        int oldLength = Utils.getDepth(bf.currentPath);
        int newLength = Utils.getDepth(bf.futurePath);

        if (newLength > oldLength)
            forewordExplorerBar();//moving foreword directories
        else if (newLength < oldLength)
            backExplorerBar(newLength); //moving backward directory
        else
            recreateExplorerBar(); //Recreating current directory
    }

    //navigating inside a directory
    private void forewordExplorerBar() {
        //get name of new directory
        String name = bf.futurePath.replace(bf.currentPath, "").replace("/", "");
        //add to saved list
        dirNames.add(name);
        //add to layout
        addExplorerTV(dirNames.size() - 1, name);
    }

    //navigating to a previous directory with index
    private void backExplorerBar(int newI) {

        int pathDepth = dirNames.size();

        Log.e(TAG, "from " + pathDepth + " to " + newI);

        //remove all views in front of
        while (pathDepth > newI) {
            //If there is a child that could be removed
            if (pathDepth <= (explorerLinearL.getChildCount())) {
                explorerLinearL.removeViewAt(pathDepth - 1);
                dirNames.remove(pathDepth - 1);
            }
            pathDepth--;
        }

    }

    //Add a textview to explorerbar
    private void addExplorerTV(int position, String name) {

        //Create textView
        TextView tv = new TextView(bf.getContext());
        tv.setLayoutParams(explorerParams);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(6, 10, 6, 10);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int newI = explorerLinearL.indexOfChild(v);
                bf.goBackTo(newI + 1);
            }
        });
        tv.setId(position);
        tv.setText(name);

        //add TextView to scrollbar
        explorerLinearL.addView(tv);

        //add drawable arrow if it is not the first element
        if (position != 0)
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.dir_to_dir, 0, 0, 0);

        tv.setCompoundDrawablePadding(10);


        //Add listener to scroll after a new child is added
        explorerScroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                explorerScroll.removeOnLayoutChangeListener(this);
                explorerScroll.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    //recreate list of directories
    public void recreateExplorerBar() {

        explorerLinearL.removeAllViews();

        for (int i = 0; i < dirNames.size(); i++) {
            addExplorerTV(i, dirNames.get(i));
        }
    }

    //toggle visibility of explorer bat items
    public void toggleExplorerBarVisibility(int visibility) {
        //back button image
        backDir.setVisibility(visibility);
        //horizontal scrollView
        explorerScroll.setVisibility(visibility);
    }


    public ArrayList<String> getDirectoryNames() {
        return dirNames;
    }

    public void setDirectoryNames(ArrayList<String> dirNames) {
        this.dirNames = dirNames;
    }


}
