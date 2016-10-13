package com.aych.chatsnap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by HowardHuang on 7/1/2015.
 */
public class InboxFragment extends ListFragment{


    @Bind(R.id.cameraIcon) ImageView mCameraIcon;

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int TAKE_PHOTO_REQUEST = 0;
    public static final int TAKE_VIDEO_REQUEST = 1;
    public static final int PICK_PHOTO_REQUEST = 2;
    public static final int PICK_VIDEO_REQUEST = 3;

    public static final int MEDIA_TYPE_IMAGE = 4;
    public static final int MEDIA_TYPE_VIDEO = 5;

    public static final int FILE_SIZE_LIMIT = 1024*1024*10; //10 MB, 1024 bytes in KB, 1024 KB in MB

    private Uri mMediaUri;
    private List<ParseObject> mMessages;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
                case 0: // Take picture
                    //THIS INTENT WILL GO TO THE CAMERA
                    Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                    if(mMediaUri == null){
                        //display an error
                        Toast.makeText(getListView().getContext(),
                                "There was a problem accessing your devices external storage",
                                Toast.LENGTH_LONG).show();
                    }
                    else {
                        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                        startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST);
                    }
                    break;
                case 1: // Take video
                    Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
                    if(mMediaUri == null){
                        //display an error
                        Toast.makeText(getListView().getContext(),
                                "There was a problem accessing your devices external storage",
                                Toast.LENGTH_LONG).show();
                    }
                    else{
                        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                        //limits to 10 seconds
                        videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
                        //low quality videos, 0 is low, 1 is high
                        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                        startActivityForResult(videoIntent, TAKE_VIDEO_REQUEST);
                    }
                    break;
                case 2: // Choose picture
                    Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    //limits it to photos
                    choosePhotoIntent.setType("image/*");
                    startActivityForResult(choosePhotoIntent, PICK_PHOTO_REQUEST);
                    break;
                case 3: // Choose video
                    Intent chooseVideoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    //limits it to videos
                    chooseVideoIntent.setType("video/*");
                    Toast.makeText(getListView().getContext(),
                            getString(R.string.video_file_size_warning),
                            Toast.LENGTH_LONG).show();
                    startActivityForResult(chooseVideoIntent, PICK_VIDEO_REQUEST);
                    break;

            }
        }
    };

    private Uri getOutputMediaFileUri(int mediaType) {
        if (isExternalStorageAvailable()){
            //get the URI

            //1. get the external storage directory
            String appName = getListView().getContext().getString(R.string.app_name);
            File mediaStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    appName);

            //2. create our own subdirectory
            if (!mediaStorageDir.exists()){
                if (!mediaStorageDir.mkdirs()){
                    //couldnt make the dirs
                    Log.e(TAG, "Failed to create directory.");
                    return null;
                }
            }

            //3. create a file name
            //4. create the file

            File mediaFile;
            Date now = new Date();
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now);

            String path = mediaStorageDir.getPath() + File.separator;
            if(mediaType == MEDIA_TYPE_IMAGE){
                mediaFile = new File(path + "IMG_" + timestamp + ".jpg");
            }
            else if(mediaType == MEDIA_TYPE_VIDEO){
                mediaFile = new File (path + "VID_" + timestamp + ".mp4");
            }
            else{
                return null;
            }

            Log.d(TAG, "File: " + Uri.fromFile(mediaFile));
            //5. return the file's URI
            return Uri.fromFile(mediaFile);
        }
        else{
            return null;
        }
    }

    private boolean isExternalStorageAvailable(){
        String state = Environment.getExternalStorageState();
        if(state.equals(Environment.MEDIA_MOUNTED)){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inbox, container, false);
        ButterKnife.bind(this, rootView);

        mSwipeRefreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);

        mCameraIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getListView().getContext());
                builder.setItems(R.array.camera_choices, mDialogListener);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == MainActivity.RESULT_OK){
            //add to gallery
            //request code is the IDs that we selected earlier
            if(requestCode == PICK_PHOTO_REQUEST || requestCode == PICK_VIDEO_REQUEST){
                if (data == null){
                    Toast.makeText(getListView().getContext(),
                            getString(R.string.general_error),
                            Toast.LENGTH_LONG).show();
                }
                else{
                    mMediaUri = data.getData();
                }

                Log.i(TAG, "Media URI: " + mMediaUri);
                if(requestCode == PICK_VIDEO_REQUEST){
                    //make sure the file is less then 10 MB
                    int fileSize = 0;
                    InputStream inputStream = null;
                    try {
                        inputStream = getListView().getContext().
                                getContentResolver().
                                openInputStream(mMediaUri);
                        fileSize = inputStream.available();
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    finally{
                        try {
                            inputStream.close();
                        }
                        catch (IOException e) {
                            //intentionally blank, we just want to close the input stream to prevent:
                            //MEMORY LEAKS!
                            e.printStackTrace();
                        }
                    }
                    if (fileSize >= FILE_SIZE_LIMIT){
                        Toast.makeText(getListView().getContext(),
                                getString(R.string.error_file_size_too_large),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
            else {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mMediaUri);
                getListView().getContext().sendBroadcast(mediaScanIntent);
            }

            Intent recipientsIntent = new Intent(getListView().getContext(), RecipientsActivity.class);
            //passing in the uri to the intent
            recipientsIntent.setData(mMediaUri);

            String fileType;
            if (requestCode == PICK_PHOTO_REQUEST || requestCode == TAKE_PHOTO_REQUEST){
                fileType = ParseConstants.TYPE_IMAGE;
            }
            else{
                fileType = ParseConstants.TYPE_VIDEO;
            }

            recipientsIntent.putExtra(ParseConstants.KEY_FILE_TYPE, fileType);
            startActivity(recipientsIntent);
        }
        else if(resultCode != MainActivity.RESULT_CANCELED){
            Toast.makeText(getListView().getContext(),
                    getString(R.string.general_error),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(ParseConstants.CLASS_MESSAGES);
        //Where the ID for the recipients ID is the currentUsers.objectId
        query.whereEqualTo(ParseConstants.KEY_RECIPIENTS_IDS, ParseUser.getCurrentUser().getObjectId());
        query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messages, ParseException e) {
                if (e == null){
                    //messages found from Parse.com
                    mMessages = messages;
                    String[] usernames = new String[mMessages.size()];
                    int i = 0;
                    for (ParseObject message : mMessages) {
                        //for each message in all of the messages
                        usernames[i] = message.getString(ParseConstants.KEY_SENDER_NAME);
                        i++;
                    }

                    //This is for a very simple list
                    /*
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getListView().getContext(),
                            android.R.layout.simple_list_item_1,
                            usernames);*/

                    //This is for a custom one
                    if(getListView().getAdapter() == null) {
                        //create the adapter if it hasnt been created yet
                        MessageAdapter adapter = new MessageAdapter(
                                getListView().getContext(), mMessages);
                        setListAdapter(adapter);
                    }
                    else{
                        //otherwise refill it
                        ((MessageAdapter) getListView().getAdapter()).refill(mMessages);
                    }
                }
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        ParseObject message = mMessages.get(position);
        String messageType = message.getString(ParseConstants.KEY_FILE_TYPE);
        ParseFile file = message.getParseFile(ParseConstants.KEY_FILE);
        Uri fileUri = Uri.parse(file.getUrl());

        if (messageType.equals(ParseConstants.TYPE_IMAGE)){
            //view image
            Intent intent = new Intent(getListView().getContext(), ViewImageActivity.class);
            intent.setData(fileUri);
            startActivity(intent);
        }
        else {
            //view video
            Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
            intent.setDataAndType(fileUri, "video/*");
            startActivity(intent);
        }
        //Delete it
        List<String> ids = message.getList(ParseConstants.KEY_RECIPIENTS_IDS);

        if(ids.size() == 1){
            //last recipient, delete the whole thing
            message.deleteInBackground();
        }
        else{
            //remove the recipient - and save
            ids.remove(ParseUser.getCurrentUser().getObjectId());

            ArrayList<String> idsToRemove = new ArrayList<String>();
            idsToRemove.add(ParseUser.getCurrentUser().getObjectId());

            message.removeAll(ParseConstants.KEY_RECIPIENTS_IDS, idsToRemove);
            message.saveInBackground();
        }
    }

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

        }
    };
}
