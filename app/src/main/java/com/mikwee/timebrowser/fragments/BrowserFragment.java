package com.mikwee.timebrowser.fragments;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikwee.timebrowser.R;
import com.mikwee.timebrowser.activities.CreditsActivity;
import com.mikwee.timebrowser.activities.SettingsActivity;
import com.mikwee.timebrowser.adapters.NewFileListAdapter;
import com.mikwee.timebrowser.components.ExplorerBar;
import com.mikwee.timebrowser.components.SelectedBar;
import com.mikwee.timebrowser.smb.BrowserThread;
import com.mikwee.timebrowser.smb.BrowserThread.SmbAction;
import com.mikwee.timebrowser.smb.SmbThread;
import com.mikwee.timebrowser.utils.FileInfo;
import com.mikwee.timebrowser.utils.FileInfoExtended;
import com.mikwee.timebrowser.utils.Utils;
import com.mikwee.timebrowser.utils.Wakkamole;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jcifs.Config;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class BrowserFragment extends Fragment implements FileChooserDialogFragment.DialogChooserCallback {
    public static String TAG = BrowserFragment.class.getSimpleName();

    private String selectedItemUrl;

    private static final int downloadID = 763;
    private static final int uploadID = 489;


    //--------------Views
    public RecyclerView mRecyclerView;
    //Loading 
    private View loadingCover;
    private ProgressBar loadingProgress;
    //Empty dir
    private TextView emptyDir;

    //----------------SelectionBar

    public SelectedBar selectedBar;
    private List<FileInfo> storedSelected = new ArrayList<>();

    //----------------ExplorerBar
    public ExplorerBar explorerBar;
    private ArrayList<String> directoryNames = new ArrayList<>();

    //-----------------Credentials
    private String username = "guest", password = "";
    private NtlmPasswordAuthentication auth;

    public String currentPath;
    public String futurePath = "";

    //Where we receive messages from thread
    private Hand2 handler;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_browser, container, false);

        //Initialize Views
        initViews(v);

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Setup stuff for notification


        //Create new Handler "tunnel"
        handler = new Hand2(this);

        //Config for jcifs library
        setupJcifs();

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        //Has custom actionBar
        setHasOptionsMenu(true);

    }

    @Override
    public void onStart() {
        super.onStart();

        //If fragment first time
        if (getArguments() != null && currentPath == null) {

            //get passed info
            username = getArguments().getString("username");
            password = getArguments().getString("password");
            currentPath = getArguments().getString("url");

            if (!password.equals(""))
                Config.setProperty("jcifs.smb.client.useExtendedSecurity", "false");

            //Create auth instance
            auth = new NtlmPasswordAuthentication(username + ":" + password);

        } else { //we are repopulating fragment

            selectedBar.setSelectedFiles(storedSelected);
            explorerBar.setDirectoryNames(directoryNames);

            //recreate explorerBar
            explorerBar.recreateExplorerBar();

        }

        //brows to location
        brows(currentPath);
    }

    @Override  //Inflate our actionbar menu
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_action_bar_browser, menu);
        selectedBar.setActionBarMenu(menu);

        //if we were in selection mode
        if (selectedBar.inSelection())
            //Show selectionBar and Actionbar
            selectedBar.showFileSelection();
    }

    @Override  //When action bar item is clicked
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {


            case R.id.action_upload: // Show Upload chooser
                showUploadDialog();
                return true;

            case R.id.action_sort: // Show Sort chooser
                showSortDialog();
                return true;

            case R.id.action_wakkamole: //Load and show AD
                new Wakkamole(getContext());
                return true;

            case R.id.action_settings: //Settings activity
                Intent iSettings = new Intent(getContext(), SettingsActivity.class);
                getContext().startActivity(iSettings);
                return true;

            case R.id.action_credits: //Cretits activity
                Intent iCredits = new Intent(getContext(), CreditsActivity.class);
                getContext().startActivity(iCredits);
                return true;

            case R.id.action_close_selection: //Only visible when item is selected
                selectedBar.hideFileSelection();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }  //todo look

    @Override
    public void onSaveInstanceState(Bundle outState) {
        this.storedSelected = selectedBar.getSelectedFiles();
        this.directoryNames = explorerBar.getDirectoryNames();
        super.onSaveInstanceState(outState);
    }

    //----------------------------------------------INIT STUFF--------------------------------------

    //Initialize the layout views
    private void initViews(View v) {

        //-----------------------ItemSelected actionBar

        selectedBar = new SelectedBar(this, v.findViewById(R.id.selected_bar));

        //-----------------------RecyclerView setup
        mRecyclerView = (RecyclerView) v.findViewById(R.id.dir_list);

        //Decide columns based on orientation
        int columns = getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 3;
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), columns));

        //Create temporary empty adapter
        NewFileListAdapter mAdapter = new NewFileListAdapter(new FileInfo[0], this, NewFileListAdapter.LayoutType.LAYOUT_GRID);
        mRecyclerView.setAdapter(mAdapter);


        //------------------------Loading
        loadingCover = v.findViewById(R.id.bg_loading);
        loadingProgress = (ProgressBar) v.findViewById(R.id.infinite_pb);

        //-----------------------Empty directory
        emptyDir = (TextView) v.findViewById(R.id.empty);


        //----------------------Explorer bar

        explorerBar = new ExplorerBar(this, v);
    }

    //Configure JCIFS library
    private void setupJcifs() {
        //set configs of jcifs library
        Config.setProperty("jcifs.smb.client.ssnLimit", "1");
        Config.setProperty("jcifs.smb.client.connTimeout", "3000");
        Config.setProperty("jcifs.smb.client.responseTimeout", "24000");
        Config.setProperty("jcifs.smb.client.soTimeout", "24000");
    }

    //----------------------------------------------ACTIONS ON FILE OR DIRECTORIES------------------

    //Navigate to a specific URL
    public void brows(String url) {

        //Show loading cover and indicator
        showLoading();

        //set the target url
        futurePath = url;

        //Start SmbThread to retrieve files
        new BrowserThread.Builder(getContext())
                .setHandler(handler).setUrl(url)
                .setAction(SmbAction.BROWS)
                .setAuth(auth)
                .build().start();

        Log.e(TAG, "Browsing to: " + futurePath);
    }


    public void showFileInfo(FileInfo file) {

        String url = file.fileFullPath;

        //show info dialog
        DialogFragment dialogFragment = new InfoDialogFragment();
        dialogFragment.show(getFragmentManager(), InfoDialogFragment.TAG);

        //Start SmbThread to retrieve file info
        new BrowserThread.Builder(getContext())
                .setHandler(handler).setUrl(url)
                .setAction(SmbAction.FILE_INFO)
                .setAuth(auth)
                .build().start();

    }

    public void showFileInfo() {
        showFileInfo(selectedBar.getSelectedFiles().get(0));
    }

    //Prompts user to choose a download destination
    public void showDownloadChooser() {

        //Create Dialog fragment
        FileChooserDialogFragment dialog = new FileChooserDialogFragment();
        //set context
        dialog.setTargetFragment(this, 0);
        //Set action to dialog fragment
        Bundle b = new Bundle();
        b.putSerializable(FileChooserDialogFragment.Mode.class.getSimpleName(), FileChooserDialogFragment.Mode.DOWNLOAD_CHOOSER);
        dialog.setArguments(b);

        //show the created dialog
        dialog.show(getFragmentManager(), FileChooserDialogFragment.TAG);
    }






    public void openFile(String url) {
        //show prorgessBar
        showLoading();

        //open file
        SmbThread st = new SmbThread(new Hand(this), url, SmbThread.SmbAction.OPEN_FILE, getContext());
        st.setUsernamePassword(username, password);
        st.setTargetUrl(Utils.getInternalTempDir(getContext()));
        st.start();
    }


    public void uploadFiles(String[] urls) {
        Log.e(TAG, "uploading to:" + currentPath);

        SmbThread st = new SmbThread(new Hand(this), currentPath, SmbThread.SmbAction.UPLOAD_FILES, getContext());
        st.setUsernamePassword(username, password);
        st.setUploadFromUrls(urls);
        st.start();

    }

    public void deleteFile(String url) {
        //save the file url to delete
        selectedItemUrl = url;
        //alert user to make sure he wants to delete
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(getActivity());
        adBuilder.setTitle("Are you sure?")
                .setPositiveButton("yes, delete", new DialogInterface.OnClickListener() { //to
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //user is sure, let's delete
                        SmbThread st = new SmbThread(handler, selectedItemUrl, SmbThread.SmbAction.DELETE_FILE, getContext());
                        st.setUsernamePassword(username, password);
                        st.start();


                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //close dialog, user changed mind
                        dialog.cancel();
                    }
                });

        adBuilder.create().show();

    }

    public void downloadFile(String fromUrl, String toUrl) {
        SmbThread st = new SmbThread(handler, fromUrl, SmbThread.SmbAction.DOWNLOAD_FILE, getContext());
        st.setUsernamePassword(username, password);
        st.setTargetUrl(toUrl);
        st.start();
    }


    //--------------------------------------------UTILS---------------------------------------------

    //Go back to the directory with index newI
    public boolean goBackTo(int newI) {
        //Current depth
        int oldI = Utils.getDepth(currentPath);
        String parent = currentPath;

        try {
            while (oldI > newI) {
                parent = new SmbFile(parent).getParent();
                oldI--;
            }
        } catch (MalformedURLException e) {
            return false;
        }

        //if we are at root directory there is no need to go back
        if ((parent == null) || parent.equals("smb://"))
            return false;

        Log.e(TAG, "parent " + parent);

        //browse to previous
        brows(parent);
        return true;


    }

    //------------------------------------------ LOADING OVERLAY -----------------------------------

    //shows loading cover and indicator
    public void showLoading() {
        loadingCover.setAlpha(0f);
        loadingProgress.setAlpha(0f);
        loadingCover.setVisibility(View.VISIBLE);
        loadingProgress.setVisibility(View.VISIBLE);

        loadingCover.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);

        loadingProgress.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);
    }

    //hide loading cover and indicator
    public void hideLoading() {
        loadingCover.setVisibility(View.INVISIBLE);
        loadingProgress.setVisibility(View.INVISIBLE);
    }

    //------------------------------------------  ------------------------------------


    //------------------------------------------  ------------------------------------


    public NtlmPasswordAuthentication getAuth() {
        return this.auth;
    }


    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Pick how to sort");

        builder.setItems(Utils.getSortMethods(getContext()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utils.Sort sm;
                switch (which) {
                    case 0:
                        sm = Utils.Sort.SIZE_ASCENDING;
                        break;
                    case 1:
                        sm = Utils.Sort.SIZE_DESCENDING;
                        break;
                    case 2:
                    default:
                        sm = Utils.Sort.FOLDER_FIRST;
                        break;
                    case 3:
                        sm = Utils.Sort.FILES_FIRST;
                        break;
                }
                ((NewFileListAdapter) mRecyclerView.getAdapter()).sortBy(sm);
            }
        });
        builder.show();

    }

    private void showUploadDialog() {

        //let the user choose what to upload
        FileChooserDialogFragment dialog = new FileChooserDialogFragment();
        dialog.setTargetFragment(this, 0);

        Bundle b = new Bundle();
        b.putSerializable("action", FileChooserDialogFragment.Mode.UPLOAD_CHOOSER);
        dialog.setArguments(b);

        dialog.show(getFragmentManager(), "uploadChooser");
    }

    //----------------------------------Dialog Chooser Callback methods

    @Override
    public void downloadPathChosen(String targetUrl) {
        //Alert user
        Toast.makeText(getContext(),
                getString(R.string.dialog_chooser_download_started),
                Toast.LENGTH_LONG).show();

        //Start SmbThread to Download files
        new BrowserThread.Builder(getContext())
                .setHandler(handler)
                .setAuth(auth)
                .setAction(SmbAction.DOWNLOAD_FILES)
                .setUrl(targetUrl)
                .setExtraFiles(selectedBar.getSelectedFiles())
                .build().start();
    }

    @Override
    public void uploadFilesChosen(String[] filePaths) {
        uploadFiles(filePaths);
    }


    //----------------------------------HANDLER

    //Handle messages and updates from SmbThread
    private static class Hand2 extends Handler {

        private final WeakReference<BrowserFragment> fragment;

        Hand2(BrowserFragment bf) {
            fragment = new WeakReference<>(bf);
        }

        //Handles the message passed from thread
        public void handleMessage(Message msg) {
            //Get fragment reference
            BrowserFragment bf = fragment.get();

            if (bf == null)
                return;

            //Get the action
            SmbAction action = SmbAction.values()[msg.what];

            switch (action) {

                case BROWS: //----------------------------------------------------------------------

                    //Update explorer bar
                    bf.explorerBar.updateExplorerBar();
                    bf.currentPath = bf.futurePath;

                    FileInfo[] files = (FileInfo[]) msg.obj;

                    // There are no files in file_folder
                    if (files == null || files.length == 0) {
                        bf.emptyDir.setVisibility(View.VISIBLE);
                        bf.mRecyclerView.setVisibility(View.GONE);
                        bf.hideLoading();
                        return;
                    }

                    bf.emptyDir.setVisibility(View.GONE);
                    bf.mRecyclerView.setVisibility(View.VISIBLE);

                    //Sort directory files
                    Arrays.sort(files, FileInfo.DirectoryFirstComparator);

                    //Update list in adapter
                    ((NewFileListAdapter) bf.mRecyclerView.getAdapter()).updateFilesList(files);

                    //Close loading indicator
                    bf.hideLoading();
                    break;

                case FILE_INFO: //------------------------------------------------------------------
                    //find fragment with fragment manager
                    InfoDialogFragment frag =
                            (InfoDialogFragment) bf.getFragmentManager()
                                    .findFragmentByTag(InfoDialogFragment.TAG);

                    //refresh layout
                    frag.refreshLayout((FileInfoExtended) msg.obj);
                    break;


                case OPEN_FILE: //------------------------------------------------------------------
                    File f = (File) msg.obj;

                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    //find the right MIME TYPE
                    String ext = fileExt(f.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);

                    newIntent.setDataAndType(Uri.fromFile(f), mimeType);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    //hide progress indicator
                    bf.hideLoading();

                    try {
                        //try to open the right application to view file
                        bf.startActivity(newIntent);

                    } catch (ActivityNotFoundException e) {

                        //alert user no app to open file was found
                        Toast.makeText(bf.getContext(), bf.getContext().getString(R.string.bf_no_handler), Toast.LENGTH_LONG).show();
                    }
                    break;

/*
                case START_UPLOAD:
                    Toast.makeText(bf.getContext(), bf.getContext().getString(R.string.bf_upload_started), Toast.LENGTH_SHORT).show();
                    break;
*/

                case UPDATE_PB:
                    int progPB = (int) msg.obj;
                    bf.loadingProgress.setProgress(progPB);
                    break;

                case FILE_DELETED:
                    //notify user
                    Toast.makeText(bf.getContext(), bf.getString(R.string.bf_delete_success), Toast.LENGTH_SHORT).show();
                    //update file list
                    bf.brows(bf.currentPath);
                    break;

                case ERROR:
                    Toast.makeText(bf.getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    bf.hideLoading();
                    break;

            }
            super.handleMessage(msg);
        }

        private String fileExt(String url) {
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }

            if (url.lastIndexOf(".") == -1) {
                return null;
            } else {
                String ext = url.substring(url.lastIndexOf(".") + 1);

                if (ext.contains("%")) {
                    ext = ext.substring(0, ext.indexOf("%"));
                }
                if (ext.contains("/")) {
                    ext = ext.substring(0, ext.indexOf("/"));
                }
                Log.e("TAg", "ext=" + ext);
                return ext.toLowerCase();

            }
        }

    }

    //Handle messages and updates from SmbThread
    private static class Hand extends Handler {

        private final WeakReference<BrowserFragment> mFragment;

        Hand(BrowserFragment bf) {
            mFragment = new WeakReference<>(bf);
        }

        //Handles the message passed from thread
        public void handleMessage(Message msg) {
            //Get reference
            BrowserFragment bf = mFragment.get();

            if (bf == null)
                return;

            //Get the action
            SmbThread.SmbAction action = SmbThread.SmbAction.values()[msg.what];

            switch (action) {

                case BROWS: //We successfully browsed to a new locationA
                    Log.e(TAG, "browsed succesfully");
                    //Update explorer bar
                    bf.explorerBar.updateExplorerBar();

                    bf.currentPath = bf.futurePath;

                    FileInfo[] newF = (FileInfo[]) msg.obj;

                    // There are no files in file_folder
                    if (newF == null || newF.length == 0) {
                        bf.emptyDir.setVisibility(View.VISIBLE);
                        bf.mRecyclerView.setVisibility(View.GONE);
                        bf.hideLoading();
                        return;
                    }

                    bf.emptyDir.setVisibility(View.GONE);
                    bf.mRecyclerView.setVisibility(View.VISIBLE);

                    //Sort directory files
                    Arrays.sort(newF, FileInfo.DirectoryFirstComparator);
                    //Update list in adapter
                    ((NewFileListAdapter) bf.mRecyclerView.getAdapter()).updateFilesList(newF);

                    //Close loading indicator
                    bf.hideLoading();
                    break;

                case OPEN_FILE:
                    File f = (File) msg.obj;

                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                    //find the right MIME TYPE
                    String ext = fileExt(f.getName());
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);

                    newIntent.setDataAndType(Uri.fromFile(f), mimeType);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    //hide progress indicator
                    bf.hideLoading();

                    try {
                        //try to open the right application to view file
                        bf.startActivity(newIntent);

                    } catch (ActivityNotFoundException e) {

                        //alert user no app to open file was found
                        Toast.makeText(bf.getContext(), bf.getContext().getString(R.string.bf_no_handler), Toast.LENGTH_LONG).show();
                    }
                    break;

                case INFO_FILE:
                    //find fragmet by fragment manager
                    InfoDialogFragment frag = (InfoDialogFragment) bf.getFragmentManager().findFragmentByTag("infoDialog");
                    //refresh layout
                    frag.refreshLayout((FileInfoExtended) msg.obj);
                    break;
                /*
                case START_UPLOAD:
                    Toast.makeText(bf.getContext(), bf.getContext().getString(R.string.bf_upload_started), Toast.LENGTH_SHORT).show();
                    break;

                case START_DOWNLOAD:
                    Toast.makeText(bf.getContext(), bf.getContext().getString(R.string.bf_download_started), Toast.LENGTH_SHORT).show();
                    break;
*/
                case UPDATE_PB:
                    int progPB = (int) msg.obj;
                    bf.loadingProgress.setProgress(progPB);
                    break;
/*                case UPDATE_UPLOAD_NOTIFICATION:
                    int progUpload = (int) msg.obj;
                    //If upload ==100 than we are finished
                    if (progUpload == 100) {
                        bf.mBuilder
                                .setContentText(bf.getString(R.string.bf_upload_complete))
                                .setProgress(0, 0, false); // Removes the progress bar

                        Toast.makeText(bf.getContext(), bf.getString(R.string.bf_upload_complete), Toast.LENGTH_SHORT).show();
                    } else {
                        // We are still uploading
                        bf.mBuilder.setContentTitle(bf.getString(R.string.bf_notif_upload_title))
                                .setContentText(bf.getString(R.string.bf_notif_upload_content))
                                .setSmallIcon(R.drawable.ic_upload_notif);
                        bf.mBuilder.setProgress(100, progUpload, false);
                        //todo set pending intent on click
                    }
                    bf.mNotifyManager.notify(uploadID, bf.mBuilder.build());
                    break;

                case UPDATE_DOWNLOAD_NOTIFICATION:
                    int progDownload = (int) msg.obj;
                    //If we finished downloading
                    if (progDownload == 100) {
                        bf.mBuilder
                                .setContentText(bf.getString(R.string.bf_download_complete))
                                .setProgress(0, 0, false); // Removes the progress bar
                        Toast.makeText(bf.getContext(), bf.getString(R.string.bf_download_complete), Toast.LENGTH_SHORT).show();
                    } else {
                        //else we are still downloading
                        bf.mBuilder.setContentTitle(bf.getString(R.string.bf_notif_download_title))
                                .setContentText(bf.getString(R.string.bf_notif_download_content))
                                .setSmallIcon(R.drawable.ic_download_notif);
                        bf.mBuilder.setProgress(100, progDownload, false);
                    }
                    //Update notification
                    bf.mNotifyManager.notify(downloadID, bf.mBuilder.build());
                    break;

*/
                case FILE_DELETED:
                    //notify user
                    Toast.makeText(bf.getContext(), bf.getString(R.string.bf_delete_success), Toast.LENGTH_SHORT).show();
                    //update file list
                    bf.brows(bf.currentPath);
                    break;

                case ERROR:
                    Toast.makeText(bf.getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    bf.hideLoading();
                    break;

            }
            super.handleMessage(msg);
        }

        private String fileExt(String url) {
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }

            if (url.lastIndexOf(".") == -1) {
                return null;
            } else {
                String ext = url.substring(url.lastIndexOf(".") + 1);

                if (ext.contains("%")) {
                    ext = ext.substring(0, ext.indexOf("%"));
                }
                if (ext.contains("/")) {
                    ext = ext.substring(0, ext.indexOf("/"));
                }
                Log.e("TAg", "ext=" + ext);
                return ext.toLowerCase();

            }
        }

    }


}


