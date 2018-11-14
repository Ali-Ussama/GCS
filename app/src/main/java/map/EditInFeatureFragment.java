package map;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.AttachmentManager;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.AttachmentInfo;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureType;
import com.esri.core.map.Field;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import activities.MapEditorActivity;
import activities.VideoActivity;

import com.gcs.riyadh.R;

import util.CollectorMediaPlayer;
import util.Utilities;
import util.ZoomableImageView;

/**
 * * Updated by Ali Ussama on 8/10/2018
 */

public class EditInFeatureFragment extends Fragment {
    public static final int REQUEST_CODE_GALLERY = 1;
    public static final int REQUEST_CODE_TAKE_PICTURE = 2;
    //    public static final int REQUEST_CODE_CROP_IMAGE = 3;
    public static final int REQUEST_CODE_VIDEO = 4;
    private static final String Attachment_Info = "AttachmentInfo";
    private static final String ATTRIBUTES = "Attributes";
    private static final String FEATURE_ID = "FeatureId";
    public static String TEMP_PHOTO_FILE_NAME;
    public static AttributeViewsBuilder listAdapter;
    ArcGISFeatureLayer featureLayer;
    MapEditorActivity editorActivity;
    LinearLayout lnAttachments;
    HorizontalScrollView hsAttachments;
    ScrollView scrollAttributes;
    TextView tvAttachment;
    FeatureLayer featureLayerOffline;
    GeodatabaseFeatureTable featureTable;
    private AttachmentInfo[] attachmentInfo;
    private HashMap<String, Object> attributes;
    private int featureId;
    private EditorFragmentListener mListener;
    private File mFileTemp;
    private MediaRecorder mRecorder = null;
    private CollectorMediaPlayer mPlayer = null;
    private String shapeType;
    private boolean isGcsShape;

    private static final String JPG = "jpg";
    private static final String MP4 = "mp4";


    public static EditInFeatureFragment newInstance(int featureId, HashMap<String, Object> attributes, AttachmentInfo[] attachmentInfos) {
        EditInFeatureFragment fragment = new EditInFeatureFragment();
        Bundle args = new Bundle();
        args.putSerializable(ATTRIBUTES, attributes);
        args.putSerializable(FEATURE_ID, featureId);
        args.putSerializable(Attachment_Info, attachmentInfos);
        fragment.setArguments(args);
        return fragment;
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (context instanceof EditorFragmentListener) {
                mListener = (EditorFragmentListener) context;
                editorActivity = ((MapEditorActivity) context);

                if (editorActivity.shapeToAdd[0].getGeometry() instanceof Point) {
                    featureLayer = editorActivity.pointFeatureLayer;
                    featureLayerOffline = editorActivity.featureLayerPointsOffline;
                    shapeType = MapEditorActivity.POINT;
                } else if (editorActivity.shapeToAdd[0].getGeometry() instanceof Polyline) {
                    featureLayer = editorActivity.lineFeatureLayer;
                    shapeType = MapEditorActivity.LINE;
                    featureLayerOffline = editorActivity.featureLayerLinesOffline;
                } else if (editorActivity.shapeToAdd[0].getGeometry() instanceof Polygon) {
                    featureLayer = editorActivity.polygonFeatureLayer;
                    shapeType = MapEditorActivity.POLYGON;
                    featureLayerOffline = editorActivity.featureLayerPolygonsOffline;
                }


                if (editorActivity.onlineData) {
                    listAdapter = new AttributeViewsBuilder(editorActivity, getFields(featureLayer.getFields()), featureLayer.getTypes(), ColumnNames.E_FEATURETYPE);
                } else {
                    featureTable = ((GeodatabaseFeatureTable) featureLayerOffline.getFeatureTable());
                    listAdapter = new AttributeViewsBuilder(editorActivity, getFields(featureTable.getFields().toArray(new Field[0])), featureTable.getFeatureTypes().toArray(new FeatureType[0]), featureTable.getTypeIdField());
                }
            } else {
                throw new RuntimeException(context.toString() + " must implement EditorFragmentListener");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("test", "On Attach");
    }


    private Field[] getFields(Field[] layerFields) {
        if (shapeType.equals(MapEditorActivity.POINT)) {
            ArrayList<Field> fields = new ArrayList<>();
            for (int i = 0; i < 1; i++) {
                fields.add(null);
            }

            /**-----------------------------Ali Ussama Update-------------------------------------*/

            for (Field field : layerFields) {
                if (ColumnNames.A_FEATURETYPE.equals(field.getName())) {
                    fields.set(0, field);
                } else if (ColumnNames.ENAME.equals(field.getName())) {
                    fields.add(field);
                } else if (ColumnNames.ANAME.equals(field.getName())) {
                    fields.add(field);
                } else if (ColumnNames.COMMENTS.equals(field.getName())) {
                    fields.add(field);
                } else if (ColumnNames.ROMAN_NAME.equals(field.getName())) {
                    Log.i("getFeilds", "ROMAN Name");
                    fields.add(field);
                }
            }
            /**-----------------------------Ali Ussama Update-------------------------------------*/
            return fields.toArray(new Field[0]);
        }
        return layerFields;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            attributes = (HashMap<String, Object>) getArguments().getSerializable(ATTRIBUTES);
            attachmentInfo = (AttachmentInfo[]) getArguments().getSerializable(Attachment_Info);
            featureId = (int) getArguments().getSerializable(FEATURE_ID);

            Integer surveyorId = (Integer) attributes.get(ColumnNames.SURVEYOR_ID);
            isGcsShape = surveyorId == null || surveyorId == 0;

        }

        Log.i("test", "On Create");

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_edit, menu);

        editorActivity.menuItemOverflow.setVisible(false);
        editorActivity.menuItemSearch.setVisible(false);

        if (!editorActivity.onlineData) {
            editorActivity.menuItemSync.setVisible(false);
            editorActivity.menuItemOnline.setVisible(false);
            editorActivity.item_load_previous_offline.setVisible(false);
        } else {
            editorActivity.menuItemOffline.setVisible(false);
            editorActivity.menuItemIndex.setVisible(false);
//            editorActivity.menuItemGCS.setVisible(false);
            editorActivity.menuItemSatellite.setVisible(false);
            editorActivity.menuItemBaseMap.setVisible(false);
        }

        if (!shapeType.equals(MapEditorActivity.POINT)) {
            menu.findItem(R.id.menu_change_location).setVisible(false);
        }


        if (isGcsShape) {
            menu.findItem(R.id.menu_change_location).setVisible(false);
            menu.findItem(R.id.menu_delete).setVisible(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                saveChanges(listAdapter);
                break;
            case R.id.menu_delete:
                onDelete();
                break;
            case R.id.menu_camera:

                if (ActivityCompat.checkSelfPermission(editorActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                } else {
                    takePicture();
                }
                break;

            case R.id.menu_audio:
                if (ActivityCompat.checkSelfPermission(editorActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 2);
                } else {
                    showAudioDialog();
                }
                break;

            case R.id.menu_gallery:
                if (ActivityCompat.checkSelfPermission(editorActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                } else {
                    openGallery();
                }
                break;
            case R.id.menu_video:
                if (ActivityCompat.checkSelfPermission(editorActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
                } else {
                    recordVideo();
                }
                break;
            case R.id.menu_change_location:
                getView().setVisibility(View.GONE);
                editorActivity.changeLocationOnline();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isSpeakButtonLongPressed;
    private AlertDialog audioDialog;

    private void showAudioDialog() {
        if (editorActivity != null) {
            editorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    View view = inflater.inflate(R.layout.dialog_record_audio, null);
                    ImageView icRecordAudio = (ImageView) view.findViewById(R.id.icRecordAudio);
                    icRecordAudio.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Log.d("audio", "In Long Click");
                            if (!isSpeakButtonLongPressed) {
                                Log.d("audio", "Record Now");
                                isSpeakButtonLongPressed = true;
                                startRecording();
                            }
                            return true;
                        }
                    });
                    icRecordAudio.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View pView, MotionEvent pEvent) {

                            pView.onTouchEvent(pEvent);
                            // We're only interested in when the button is released.
                            if (pEvent.getAction() == MotionEvent.ACTION_UP) {
                                // We're only interested in anything if our speak button is currently pressed.
                                if (isSpeakButtonLongPressed) {
                                    // Do something when the button is released.
                                    isSpeakButtonLongPressed = false;
                                    Log.d("audio", "Stop Recording");
                                    if (audioDialog != null) {
                                        audioDialog.dismiss();
                                    }
                                    stopRecording();
                                }
                            }
                            return false;
                        }
                    });
                    builder.setView(view);
                    audioDialog = builder.show();

                }
            });
        }
    }

    private void recordVideo() {
        createFile("Video", MP4);
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Uri contentUri = FileProvider.getUriForFile(getContext(), getString(R.string.app_package_name), mFileTemp);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 15);
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set (1) video quality to high, (0) LOW
        startActivityForResult(takeVideoIntent, REQUEST_CODE_VIDEO);
    }

    private void takePicture() {
        createFile("Image", JPG);
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(getContext(), getString(R.string.app_package_name), mFileTemp);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(cameraIntent, REQUEST_CODE_TAKE_PICTURE);
    }

    private void openGallery() {
        createFile("Image", JPG);
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
    }


    private void createFile(String name, String extension) {
        Date d = new Date();
        TEMP_PHOTO_FILE_NAME = name + "_" + new SimpleDateFormat("dd_MM_yyyy_HH_MM_SS", Locale.ENGLISH).format(d) + "." + extension;
        mFileTemp = new File(editorActivity.getExternalCacheDir(), TEMP_PHOTO_FILE_NAME);
        Log.d(name, TEMP_PHOTO_FILE_NAME);
        Log.d("file createFile", mFileTemp.toString());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(editorActivity, R.string.permission_gallery, Toast.LENGTH_LONG).show();
            } else {
                openGallery();
            }
        } else if (requestCode == 2) {

            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(editorActivity, R.string.permission_record, Toast.LENGTH_LONG).show();
            } else {
                showAudioDialog();
            }

        } else if (requestCode == 3) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(editorActivity, R.string.permission_camera, Toast.LENGTH_LONG).show();
            } else {
                takePicture();
            }

        } else {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(editorActivity, R.string.permission_camera, Toast.LENGTH_LONG).show();
            } else {
                recordVideo();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_edit_in_feature, container, false);


        listAdapter.setFeatureSet(attributes);


        LinearLayout listView = (LinearLayout) view.findViewById(R.id.list_view);
        lnAttachments = (LinearLayout) view.findViewById(R.id.lnAttachments);
        hsAttachments = (HorizontalScrollView) view.findViewById(R.id.hsAttachments);
        scrollAttributes = (ScrollView) view.findViewById(R.id.scrollAttributes);
        tvAttachment = (TextView) view.findViewById(R.id.tvAttachment);

        boolean focused = false;


        for (int i = 0; i < listAdapter.getCount(); i++) {

            View v = listAdapter.getView(i, isGcsShape);

            if (!focused) {
                EditText editText = (EditText) v.findViewById(R.id.field_value_txt);
                if (editText != null) {
                    editText.requestFocus();
                    focused = true;
                }
            }
            listView.addView(v);
        }

        if (attachmentInfo == null || attachmentInfo.length == 0) {
            tvAttachment.setText(R.string.no_attachment);
            hsAttachments.setVisibility(View.GONE);
        } else {
            for (AttachmentInfo anAttachmentInfo : attachmentInfo) {
                addAttachmentFileToView(anAttachmentInfo.getName(), (int) anAttachmentInfo.getId(), null, false);
            }
        }
        return view;
    }

    private void addAttachmentFileToView(final String name, final int id, final File file, boolean scrollToEnd) {

        if (editorActivity != null) {
            int screenWidth = editorActivity.getResources().getDisplayMetrics().widthPixels;

            final RelativeLayout relativeLayout = new RelativeLayout(editorActivity);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (screenWidth * 0.4), (int) (screenWidth * 0.4));
            layoutParams.setMargins((int) (screenWidth * 0.02), 0, (int) (screenWidth * 0.02), 0);
            relativeLayout.setLayoutParams(layoutParams);
            relativeLayout.setBackgroundColor(ContextCompat.getColor(editorActivity, R.color.attachmentBackgroundColor));

            final ImageView ivAttachment = new ImageView(editorActivity);
            ivAttachment.setId(android.R.id.icon);
            RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.4), (int) (screenWidth * 0.3));
            relativeParams.setMargins(0, (int) (screenWidth * 0.01), 0, 0);
            ivAttachment.setLayoutParams(relativeParams);

            if (file == null) {
                ivAttachment.setImageResource(R.drawable.ic_file_download_white_24dp);
            } else {
                Log.d("Here1", file.getAbsolutePath());
                if (Utilities.getFileExt(file.getName()).equals(JPG)) {
                    setPic(editorActivity, ivAttachment, file);
                } else {
                    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND);
                    if (bitmap != null) {
                        BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
                        ivAttachment.setBackground(ob);
                    }
                    ivAttachment.setImageResource(R.drawable.play_circle);
                }

                ivAttachment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Utilities.getFileExt(file.getName()).equals(JPG))
                            showImageInDialog(file);
                        else
                            showVideoInDialog(file);
                    }
                });

                ivAttachment.setScaleType(ImageView.ScaleType.CENTER);
            }


            relativeLayout.addView(ivAttachment);

            final ProgressBar progressBar = new ProgressBar(editorActivity);
            relativeParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.1), (int) (screenWidth * 0.1));
            relativeParams.setMargins(0, (int) (screenWidth * 0.01), 0, 0);
            relativeParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            progressBar.setLayoutParams(relativeParams);
            relativeLayout.addView(progressBar);
            progressBar.setVisibility(View.GONE);

            if (file == null) {

                ivAttachment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadAttachmentFile(progressBar, ivAttachment, name, id, featureId);

                    }
                });

                progressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadAttachmentFile(progressBar, ivAttachment, name, id, featureId);
                    }
                });

            }

            TextView tvAttachment = new TextView(editorActivity);
            relativeParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.4), FrameLayout.LayoutParams.WRAP_CONTENT);
            relativeParams.addRule(RelativeLayout.BELOW, ivAttachment.getId());
            relativeParams.setMargins((int) (screenWidth * 0.01), (int) (screenWidth * 0.01), 0, (int) (screenWidth * 0.01));
            tvAttachment.setLayoutParams(relativeParams);
            relativeLayout.addView(tvAttachment);
            tvAttachment.setText(name);
            tvAttachment.setMaxLines(1);
            tvAttachment.setEllipsize(TextUtils.TruncateAt.END);


            final ImageView ivDeleteAttachment = new ImageView(editorActivity);
            relativeParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.08), (int) (screenWidth * 0.08));
            relativeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            ivDeleteAttachment.setLayoutParams(relativeParams);
            ivDeleteAttachment.setImageResource(R.drawable.close_circle_outline);
            relativeLayout.addView(ivDeleteAttachment);

            ivDeleteAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utilities.showConfirmDialog(editorActivity, getString(R.string.delete_attachment), getString(R.string.are_you_sure), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (mPlayer != null && mPlayer.isPlaying() && file != null && mPlayer.getFileName().equals(file.getName())) {
                                stopPlaying();
                            }
                            deleteAttachment(relativeLayout, id, featureId);
                        }
                    });
                }
            });

            lnAttachments.addView(relativeLayout);

            if (scrollToEnd) {
                hsAttachments.postDelayed(new Runnable() {
                    public void run() {
                        if (editorActivity != null) {
                            hsAttachments.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                            scrollAttributes.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    }
                }, 100);
            }
        }
    }

    public void saveChanges(AttributeViewsBuilder listAdapter) {
        if (mListener != null) {
            mListener.onSave(listAdapter);
        }
    }

    public void onDelete() {

        Utilities.showConfirmDialog(editorActivity, getString(R.string.delete_service), getString(R.string.are_you_sure), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mListener != null) {
                    mListener.onDelete(featureId);
                }
            }
        });
    }

    private void downloadAttachmentFile(final ProgressBar progressBar, final ImageView view, final String attachmentName, final long attachmentId, final int featureId) {

        if (editorActivity != null && editorActivity.onlineData) {
            Log.d("editorActivity : ", editorActivity.getExternalCacheDir().toString());

            AttachmentManager attachmentManager = new AttachmentManager(editorActivity, featureLayer.getUrl(), editorActivity.featureServiceToken, editorActivity.getExternalCacheDir());
            attachmentManager.downloadAttachment(featureId, (int) attachmentId, attachmentName, new AttachmentManager.AttachmentDownloadListener() {
                @Override
                public void onAttachmentDownloading() {
                    Log.d("Attachment", "on Attachment Downloading");
                    if (editorActivity != null) {
                        editorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                                view.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                }

                @Override
                public void onAttachmentDownloaded(final File file) {
                    if (file != null && editorActivity != null) {
                        Log.d("Attachment", "on Attachment Downloaded " + file.getAbsolutePath());
                        editorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (file.exists()) {
                                    progressBar.setVisibility(View.GONE);
                                    view.setVisibility(View.VISIBLE);
                                    view.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (Utilities.getFileExt(file.getName()).equals(JPG)) {
                                                showImageInDialog(file);
                                            }
                                            else {
                                                showVideoInDialog(file);
                                            }
                                        }
                                    });
                                    progressBar.setOnClickListener(null);
                                    if (Utilities.getFileExt(file.getName()).equals(JPG)) {
                                        setPic(editorActivity, view, file);

                                    }
                                    else {
                                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(Uri.fromFile(file).getPath(), MediaStore.Video.Thumbnails.MICRO_KIND);
                                        if (bitmap != null) {
                                            BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
                                            view.setBackground(ob);
                                        }

                                        view.setImageResource(R.drawable.play_circle);
                                    }
                                    view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    view.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    } else {
                        downloadAttachmentFile(progressBar, view, attachmentName, attachmentId, featureId);
                    }
                }

                @Override
                public void onAttachmentDownloadFailed(Exception e) {
                    if (editorActivity != null) {
                        Log.d("Attachment", "on Attachment Download Failed ");
                        e.printStackTrace();
                        editorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.INVISIBLE);
                                view.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        } else {
            featureTable.retrieveAttachment(featureId, attachmentId, new CallbackListener<InputStream>() {
                @Override
                public void onCallback(final InputStream inputStream) {
                    Log.d("Attachment", "on Attachment Downloaded ");

                    if (editorActivity != null) {

                        try {
                            final File file = new File(editorActivity.getExternalCacheDir(), attachmentName);
                            OutputStream output = new FileOutputStream(file);
                            byte[] buffer = new byte[4 * 1024]; // or other buffer size
                            int read;

                            while ((read = inputStream.read(buffer)) != -1) {
                                output.write(buffer, 0, read);
                            }
                            output.flush();
                            output.close();

                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    view.setVisibility(View.VISIBLE);
                                    view.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (Utilities.getFileExt(file.getName()).equals(JPG))
                                                showImageInDialog(file);
//                                            else if (file.getName().contains("Audio"))
//                                                startPlaying(file, view);
                                            else
                                                showVideoInDialog(file);
                                        }
                                    });
                                    progressBar.setOnClickListener(null);
                                    if (Utilities.getFileExt(file.getName()).equals(JPG)) {
//                                        view.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                                        setPic(editorActivity, view, file);
                                    }
//                                    else if (file.getName().contains("Audio")) {
//                                        view.setImageResource(R.drawable.ic_play);
//                                    }
                                    else {
                                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(Uri.fromFile(file).getPath(), MediaStore.Video.Thumbnails.MICRO_KIND);
                                        if (bitmap != null) {
                                            BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
                                            view.setBackground(ob);
                                        }

                                        view.setImageResource(R.drawable.play_circle);
                                    }
                                    view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace(); // handle exception, define IOException and others
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
        }
    }

    private void showImageInDialog(final File file) {
        if (editorActivity != null) {

            editorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Log.d("Attachment", "Show Dialog ");

                    AlertDialog.Builder builder = new AlertDialog.Builder(editorActivity, R.style.Theme_Holo_Dialog_Alert);
                    builder.setTitle(file.getName());
                    View dialogView = LayoutInflater.from(editorActivity).inflate(R.layout.dialog_img_attachment, null, false);

                    ((ZoomableImageView) dialogView).setImageBitmap(decodeScaledBitmapFromSdCard(file.getAbsolutePath(), 400, 300));
                    builder.setView(dialogView);
                    builder.show().getWindow().setBackgroundDrawableResource(R.color.dialogAttachmentBackgroundColor);
                }
            });
        }
    }

    public static Bitmap decodeScaledBitmapFromSdCard(String filePath,
                                                      int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private void showVideoInDialog(final File file) {
        if (editorActivity != null) {

            editorActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Intent i = new Intent(editorActivity, VideoActivity.class);
                    i.putExtra("file", file);
                    startActivity(i);

                }
            });
        }
    }

    private void addAttachmentToFeature(final File file) {
        if (editorActivity != null) {

            Utilities.showLoadingDialog(editorActivity);
            if (editorActivity.onlineData) {
                featureLayer.addAttachment(featureId, file, new CallbackListener<FeatureEditResult>() {
                    @Override
                    public void onCallback(final FeatureEditResult featureEditResult) {
                        Log.d("Attachment", "Done Add Attachments ");
                        if (editorActivity != null) {

                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int attachmentId = (int) featureEditResult.getObjectId();
                                    addAttachmentFileToView(file.getName(), attachmentId, file, true);
                                    Utilities.dismissLoadingDialog();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(final Throwable e) {
                        Log.d("Attachment", "Error In Add Attachments ");
                        if (editorActivity != null) {

                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utilities.dismissLoadingDialog();
                                    Utilities.showToast(editorActivity, e.toString());
                                    Utilities.showToast(editorActivity, getString(R.string.connection_failed));
                                }
                            });
                        }
                    }
                });

            } else {
                try {
                    featureTable.addAttachment(featureId, file, "Service", file.getName(), new CallbackListener<Long>() {
                        @Override
                        public void onCallback(final Long aLong) {
                            if (aLong != -1) {
                                Log.d("Attachment", "Done Add Attachments ");
                                if (editorActivity != null) {
                                    editorActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int attachmentId = aLong.intValue();
                                            addAttachmentFileToView(file.getName(), attachmentId, file, true);
                                            Utilities.dismissLoadingDialog();
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.d("Attachment", "Error In Add Attachments ");
                            if (editorActivity != null) {

                                editorActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Utilities.dismissLoadingDialog();
                                        Utilities.showToast(editorActivity, getString(R.string.attachment_offline_error));
                                    }
                                });
                            }
                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.d("Attachment", "Error In Add Attachments ");
                    if (editorActivity != null) {
                        editorActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.dismissLoadingDialog();
                                Utilities.showToast(editorActivity, getString(R.string.attachment_offline_error));
                            }
                        });
                    }
                }
            }
        }
    }

    private void deleteAttachment(final RelativeLayout relativeLayout, long attachmentId, int featureId) {
        Log.d("attachmentId Hanan", String.valueOf(attachmentId));
        if (editorActivity != null) {

            Utilities.showLoadingDialog(editorActivity);

            if (editorActivity.onlineData) {
                featureLayer.deleteAttachments(featureId, new int[]{(int) attachmentId}, new CallbackListener<FeatureEditResult[]>() {
                    @Override
                    public void onCallback(FeatureEditResult[] featureEditResults) {
                        Log.d("Attachment", "Done Delete Attachments ");
                        if (editorActivity != null) {
                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utilities.dismissLoadingDialog();
                                    lnAttachments.removeView(relativeLayout);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(final Throwable e) {
                        Log.d("Attachment", "Error In delete Attachments ");
                        if (editorActivity != null) {

                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utilities.dismissLoadingDialog();
                                    Utilities.showToast(editorActivity, e.toString());
                                    Utilities.showToast(editorActivity, getString(R.string.connection_failed));
                                }
                            });
                        }
                    }
                });
            } else {
                featureTable.deleteAttachment(featureId, attachmentId, new CallbackListener<Void>() {
                    @Override
                    public void onCallback(Void aVoid) {
                        Log.d("Attachment", "Done Delete Attachments ");
                        if (editorActivity != null) {

                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utilities.dismissLoadingDialog();
                                    lnAttachments.removeView(relativeLayout);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(final Throwable e) {
                        Log.d("Attachment", "Error In delete Attachments ");
                        if (editorActivity != null) {

                            editorActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utilities.dismissLoadingDialog();
                                    Utilities.showToast(editorActivity, e.toString());
                                    Utilities.showToast(editorActivity, getString(R.string.connection_failed));
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((resultCode == Activity.RESULT_OK)) {
            switch (requestCode) {
                case REQUEST_CODE_TAKE_PICTURE:

                    hsAttachments.setVisibility(View.VISIBLE);
                    tvAttachment.setText(getString(R.string.attachments));
                    saveFileToStorage(mFileTemp);
                    addAttachmentToFeature(mFileTemp);
                    break;

                case REQUEST_CODE_GALLERY:
                    if (data != null) {
                        try {
                            InputStream inputStream = editorActivity.getContentResolver().openInputStream(data.getData());
                            FileOutputStream fileOutputStream = new FileOutputStream(mFileTemp);
                            copyStream(inputStream, fileOutputStream);
                            fileOutputStream.close();
                            if (inputStream != null) {
                                inputStream.close();
                            }
//                        startCropImage();
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                                writeBitmapInFile(bitmap);
//                            startCropImage();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        } finally {
//                        String path = data.getStringExtra(CropImage.IMAGE_PATH);
//                        if (path == null) {
//                            return;
//                        }
                            hsAttachments.setVisibility(View.VISIBLE);
                            tvAttachment.setText(getString(R.string.attachments));
                            addAttachmentToFeature(mFileTemp);
                        }
                    }
                    break;

                case REQUEST_CODE_VIDEO:
                    try {
//                        InputStream inputStream = editorActivity.getContentResolver().openInputStream(data.getData());
//                        FileOutputStream fileOutputStream = new FileOutputStream(mFileTemp);
//                        copyStream(inputStream, fileOutputStream);
//                        fileOutputStream.close();
//                        inputStream.close();
//                        mFileTemp = new File(getRealPathFromURI(data.getData()));
//                        Log.d("video",mFileTemp.getPath());
                        checkVideoSize(mFileTemp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

//                case REQUEST_CODE_CROP_IMAGE:
//                    break;

            }
        }
    }

    private void saveFileToStorage(File mFileTemp) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            File folder = new File(Environment.getExternalStorageDirectory(), "GCS App");
            folder.mkdir();
            //TODO Move mFileTemp to this Folder
        }
    }

    private void checkVideoSize(File file) {
        long Filesize = file.length() / 1024;//call function and convert bytes into Kb
        if (Filesize >= 1024) {
            if ((Filesize / 1024) > 30) {
                Utilities.showInfoDialog(editorActivity, getString(R.string.video_size), getString(R.string.change_video_setting));
            } else {
                hsAttachments.setVisibility(View.VISIBLE);
                tvAttachment.setText(getString(R.string.attachments));
                saveFileToStorage(file);
                addAttachmentToFeature(file);
            }
        } else {
            hsAttachments.setVisibility(View.VISIBLE);
            tvAttachment.setText(getString(R.string.attachments));
            saveFileToStorage(file);
            addAttachmentToFeature(file);
        }
    }

    private void writeBitmapInFile(Bitmap bmp) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(mFileTemp);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("test", "Detach");
        mListener = null;
        editorActivity = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("test", "On Save Instance State");
    }

    public interface EditorFragmentListener {
        void onSave(AttributeViewsBuilder listAdapter);

        void onDelete(int featureId);
    }

    private void startRecording() {
        Log.d("audio", "Start Recording...");


        if (mPlayer != null && mPlayer.isPlaying()) {
            stopPlaying();
        }
        MediaPlayer ring = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.starting_record);
        ring.start();

        createFile("Audio", "mp4");
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(String.valueOf(mFileTemp));
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecorder.start();
            }
        }, 3);

    }

    private void stopRecording() {
        Log.d("audio", "Stop Recording...");
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        saveFileToStorage(mFileTemp);
        addAttachmentToFeature(mFileTemp);
    }

    private ImageView prevIvClicked;

    private void startPlaying(File mediaFile, ImageView ivClicked) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mediaFile);

            if (mPlayer != null && mPlayer.isPlaying() && mPlayer.getFileName().equals(mediaFile.getName())) {
                stopPlaying();
                ivClicked.setImageResource(R.drawable.ic_play);
            } else {

                if (mPlayer != null && mPlayer.isPlaying()) {
                    stopPlaying();
                    prevIvClicked.setImageResource(R.drawable.ic_play);
                }

                mPlayer = new CollectorMediaPlayer(mediaFile.getName());
                try {
                    mPlayer.setDataSource(fis.getFD());
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.prepare();
                    mPlayer.start();
                    ivClicked.setImageResource(R.drawable.ic_pause);
                    prevIvClicked = ivClicked;

                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            stopPlaying();
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ignore) {
            }
        }

    }

    private void setPic(Context mContext, ImageView mImageView, File mCurrentPhotoPath) {
        // Get the dimensions of the View

        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int targetW = (int) (screenWidth * 0.3);
        int targetH = (int) (screenWidth * 0.4);

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 2;
        try {
            // Determine how much to scale down the image
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath.getAbsolutePath(), bmOptions);
        mImageView.setImageBitmap(bitmap);

//        Picasso.with(mContext).load(mCurrentPhotoPath).into(mImageView);
    }

    private void stopPlaying() {
        Log.d("audio", "Stop Playing");
        mPlayer.release();
        mPlayer = null;

        if (prevIvClicked != null) {
            prevIvClicked.setImageResource(R.drawable.ic_play);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }


}
