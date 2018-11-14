package map;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geodatabase.GeodatabaseFeatureTableEditErrors;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusCallback;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusInfo;
import com.esri.core.tasks.geodatabase.GeodatabaseSyncTask;
import com.esri.core.tasks.geodatabase.SyncGeodatabaseParameters;
import com.esri.core.tasks.tilecache.ExportTileCacheParameters;
import com.esri.core.tasks.tilecache.ExportTileCacheStatus;
import com.esri.core.tasks.tilecache.ExportTileCacheTask;
import com.gcs.riyadh.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

import activities.MapEditorActivity;
import util.DataCollectionApplication;
import util.Utilities;

import static android.content.Context.MODE_PRIVATE;
/**
 * * Updated by Ali Ussama on 8/10/2018
 */
public class GeoDatabaseUtil {
    private static long t;


    private static final String TAG = "GeoDatabaseUtil";
    private static final String ROOT_GEO_DATABASE_PATH = "/farsi/OfflineEditor"; //Geo Database Tables (Service Layer)
    private static final String ROOT_BASE_MAP_PATH = "/farsi/BaseMap"; //Map Tiles (Base Map Images)
    private static GeodatabaseSyncTask gdbTask;

    private static boolean error = false;
    public static void goOnline(final MapEditorActivity activity, final MapView mapView, final int localDatabaseNumber) {

        if (Utilities.isNetworkAvailable(activity) && !activity.onlineData) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {

                        case DialogInterface.BUTTON_POSITIVE:

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    synchronize(activity, mapView, true, localDatabaseNumber);
                                }
                            }).start();

                            break;

                        case DialogInterface.BUTTON_NEGATIVE:

                            preOnline(activity, mapView);
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(activity.getString(R.string.sync_before_online))
                    .setPositiveButton(activity.getString(R.string.yes), dialogClickListener)
                    .setNegativeButton(activity.getString(R.string.no), dialogClickListener)
                    .show();

        } else {
            Utilities.showToast(activity, activity.getString(R.string.no_internet));
        }
    }

    private static void preOnline(final MapEditorActivity activity, final MapView mapView) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {

                    case DialogInterface.BUTTON_POSITIVE:
                        deleteLocalGeoDatabase(activity);
                        finishGoingOnline(activity, mapView);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        finishGoingOnline(activity, mapView);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getString(R.string.delete_current_offline_map))
                .setPositiveButton(activity.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(activity.getString(R.string.no), dialogClickListener)
                .show();

    }

    public static void finishGoingOnline(final MapEditorActivity activity, final MapView mapView) {

        Log.i(TAG, "Going online ....");

        activity.onlineData = true;
        activity.menuItemOnline.setVisible(false);
        activity.menuItemSync.setVisible(false);
        activity.menuItemOffline.setVisible(true);
        activity.menuItemIndex.setVisible(true);
        activity.menuItemSearch.setVisible(true);
        activity.item_load_previous_offline.setVisible(false);

        if (!GeoDatabaseUtil.isGeoDatabaseLocal()) {
            activity.menuItemLoad.setVisible(false);
        } else {
            activity.menuItemLoad.setVisible(true);
        }

        for (Layer layer : mapView.getLayers()) {
            if ((layer.getName() != null && layer.getName().matches("index")) || layer instanceof ArcGISFeatureLayer || layer instanceof ArcGISTiledMapServiceLayer || layer instanceof ArcGISDynamicMapServiceLayer)
                mapView.removeLayer(layer);
        }

        activity.initMapOnlineLayers();

        if (!MapEditorActivity.isInitialized) {
            activity.mapInitialized();
        } else {
            activity.clearAllGraphicLayers();
            activity.refreshPOI();
        }

        if (activity.offlineGraphicLayer != null) {
            activity.offlineGraphicLayer.removeAll();
        }

        MapEditorActivity.LAYER_SR = SpatialReference.create(MapEditorActivity.SPATIAL_REFERENCE_CODE);

        activity.mapLayersUpdated();

        Log.i(TAG, "Finish Going online");

    }

    public static void downloadData(final MapEditorActivity activity) {
        downloadGeoDatabase(activity, activity.getMapView());
    }

    private static void downloadGeoDatabase(final MapEditorActivity activity, final MapView mapView) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utilities.showLoadingDialog(activity);
            }
        });

        Log.i(TAG, "Getting Feature Service Info...");

        gdbTask = new GeodatabaseSyncTask(activity.getResources().getString(R.string.gcs_feature_server), MapEditorActivity.featureServiceToken);

        gdbTask.fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {
            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "Error In Getting Feature Service Info" + " time = " + System.currentTimeMillis());
                e.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorInDownloadDialog(activity, mapView);
                        Utilities.dismissLoadingDialog();
                    }
                });
            }

            @Override
            public void onCallback(FeatureServiceInfo fsInfo) {
                if (fsInfo.isSyncEnabled()) {
                    t = System.currentTimeMillis();
                    Log.i(TAG, "Feature Service Is Sync Enable" + " time = " + System.currentTimeMillis());

                    if (fsInfo.getUrl().equals(activity.getResources().getString(R.string.gcs_feature_server))) {
                        Log.i(TAG, "Feature Service :" + fsInfo.getUrl() + " time = " + System.currentTimeMillis());

                        createGeoDatabaseOffline(gdbTask, activity, mapView, fsInfo);
                    }
                } else {
                    Log.i(TAG, "Feature Service Is not Sync Enable" + " time = " + System.currentTimeMillis());
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                            Utilities.dismissLoadingDialog();
                        }
                    });
                }
            }
        });
    }

    private static void createGeoDatabaseOffline(GeodatabaseSyncTask geodatabaseSyncTask, final MapEditorActivity activity, final MapView mapView, FeatureServiceInfo fsInfo) {
        try {
            Log.i(TAG, "Downloading...");
            GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters(fsInfo, mapView.getExtent(), mapView.getSpatialReference(), null, true);

            params.setOutSpatialRef(fsInfo.getSpatialReference());
            CallbackListener<String> gdbResponseCallback = new CallbackListener<String>() {
                @Override
                public void onCallback(String path) {
                    try {

                        Log.i(TAG, "Offline Database Created Successfully" + " time = " + ((System.currentTimeMillis() - t) / 1000));

                        addLocalLayers(mapView, activity, DataCollectionApplication.getDatabaseNumber());
                        DataCollectionApplication.setLocalDatabaseTitle(MapEditorActivity.localDatabaseTitle);
                        DataCollectionApplication.incrementDatabaseNumber();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable e) {

                    Log.i(TAG, "Error in creating offline database" + " time = " + System.currentTimeMillis());

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showErrorInDownloadDialog(activity, mapView);
                            Utilities.dismissLoadingDialog();
                        }
                    });

                    e.printStackTrace();
                }
            };

            GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {

                @Override
                public void statusUpdated(GeodatabaseStatusInfo status) {
                    Log.i(TAG, "Database Offline Status: " + status.getStatus().toString() + " time = " + System.currentTimeMillis());
                }

            };

            String databasePath = activity.getFilesDir().getPath() + GeoDatabaseUtil.ROOT_GEO_DATABASE_PATH + DataCollectionApplication.getDatabaseNumber() + "/offlinedata.geodatabase";
            geodatabaseSyncTask.generateGeodatabase(params, databasePath, false, statusCallback, gdbResponseCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load Geo Database (Features Layer and Raster Layer as a BaseMap) into MapView
     *
     * @param mapView  to add Layers into it
     * @param activity a context reference from
     *                 MapEditorActivity to access it's vars and Resources
     */
    public static void addLocalLayers(final MapView mapView, final MapEditorActivity activity, final int databaseNumber) {

        activity.onlineData = false;

        Log.i(TAG, "Removing all the features layers from map");

        for (Layer layer : mapView.getLayers()) {
            Log.i(TAG, "addLocalLayers(): Layer " + layer.getName());
            if ((layer.getName() != null && layer.getName().matches("index")) || layer instanceof ArcGISFeatureLayer || layer instanceof ArcGISTiledMapServiceLayer || layer instanceof ArcGISDynamicMapServiceLayer) {
                Log.i(TAG, "addLocalLayers(): Layer " + layer.getName() + " has been deleted");
                mapView.removeLayer(layer);
            }
        }

        Log.i(TAG, "addLocalLayers(): Add features layers from Local Geo Database");

        Geodatabase geodatabase;
        try {

            String databasePath = activity.getFilesDir().getPath() + GeoDatabaseUtil.ROOT_GEO_DATABASE_PATH + databaseNumber + "/offlinedata.geodatabase";

            geodatabase = new Geodatabase(databasePath);

            for (GeodatabaseFeatureTable gdbFeatureTable : geodatabase.getGeodatabaseTables()) {
                if (gdbFeatureTable.hasGeometry()) {
                    Log.i(TAG, "addLocalLayers(): gdb Feature Table has geometry");
                    if (gdbFeatureTable.getFeatureServiceLayerName().equals(activity.getString(R.string.point_feature_layer_name))) {
                        activity.featureLayerPointsOffline = new FeatureLayer(gdbFeatureTable);
                        activity.featureTablePoints = ((GeodatabaseFeatureTable) activity.featureLayerPointsOffline.getFeatureTable());
                        Log.i(TAG, "addLocalLayers(): LayerName is GeoNames");
                        Log.i(TAG, "addLocalLayers(): LayerName is " + activity.featureTablePoints.getDescription());

                    } else if (gdbFeatureTable.getFeatureServiceLayerName().equals(activity.getString(R.string.line_feature_layer_name))) {
                        activity.featureLayerLinesOffline = new FeatureLayer(gdbFeatureTable);
                        activity.featureTableLines = ((GeodatabaseFeatureTable) activity.featureLayerLinesOffline.getFeatureTable());
                        Log.i(TAG, "addLocalLayers(): LayerName is NameExtendLine");
                    } else if (gdbFeatureTable.getFeatureServiceLayerName().equals(activity.getString(R.string.polygon_feature_layer_name))) {
                        activity.featureLayerPolygonsOffline = new FeatureLayer(gdbFeatureTable);
                        activity.featureTablePolygons = ((GeodatabaseFeatureTable) activity.featureLayerPolygonsOffline.getFeatureTable());
                        Log.i(TAG, "addLocalLayers(): LayerName is NameExtendArea");
                    } else if (gdbFeatureTable.getFeatureServiceLayerName().toLowerCase().startsWith("index")) {
                        Log.i(TAG, "addLocalLayers(): LayerName starts with \"index\" ");

                        mapView.addLayer(new FeatureLayer(gdbFeatureTable));

                    }
                }
            }

            mapView.setExtent(activity.featureTableLines.getExtent());


            SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.GREEN, 3, SimpleLineSymbol.STYLE.SOLID);
            if (activity.offlineGraphicLayer == null)
                activity.offlineGraphicLayer = new GraphicsLayer();

            mapView.addLayer(activity.offlineGraphicLayer);
            activity.offlineGraphicLayer.removeAll();
            activity.offlineGraphicLayer.addGraphic(new Graphic(activity.featureTableLines.getExtent(), lineSymbol));


            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        MapEditorActivity.LAYER_SR = null;
                        MapEditorActivity.currentOfflineVersion = databaseNumber;
                        if (activity.isInDrawMood)
                            activity.endDrawOnMap();
                        activity.item_load_previous_offline.setVisible(true);
                        activity.menuItemOffline.setVisible(false);
                        activity.menuItemIndex.setVisible(false);
//                        activity.menuItemGCS.setVisible(false);
                        activity.menuItemSatellite.setVisible(false);
                        activity.menuItemBaseMap.setVisible(false);
                        activity.menuItemSync.setVisible(true);
                        activity.menuItemOnline.setVisible(true);
                        activity.menuItemSearch.setVisible(false);
                        if (!GeoDatabaseUtil.isGeoDatabaseLocal()) {
                            activity.menuItemLoad.setVisible(false);
                        } else {
                            activity.menuItemLoad.setVisible(true);
                        }

                        if (!MapEditorActivity.isInitialized) {
                            activity.mapInitialized();
                        } else {
                            Log.i(TAG, "addLocalLayers(): Map Editor not Initialized");
                            activity.clearAllGraphicLayers();
                            activity.refreshPOI();
                        }

                        Utilities.dismissLoadingDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (FileNotFoundException e) {
            Log.i(TAG, "Error in adding feature layers from Local Geo Database");
            e.printStackTrace();
        }
    }


    public static void synchronize(final MapEditorActivity activity, final MapView mapView, final boolean goOnline, final int localDatabaseNumber) {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utilities.showLoadingDialog(activity);
            }
        });

        if (gdbTask == null) {
            gdbTask = new GeodatabaseSyncTask(activity.getResources().getString(R.string.gcs_feature_server), MapEditorActivity.featureServiceToken);
            gdbTask.fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    Log.i(TAG, "Error in upload and synchronize local geo database to the server");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                            Utilities.dismissLoadingDialog();
                        }
                    });
                }

                @Override
                public void onCallback(FeatureServiceInfo objs) {
                    if (objs.isSyncEnabled()) {
                        doSyncAllInOne(activity, mapView, goOnline, localDatabaseNumber);
                    } else {
                        Log.i(TAG, "Feature Service Not Sync Enable");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                                Utilities.dismissLoadingDialog();
                            }
                        });

                    }
                }
            });
        } else {
            doSyncAllInOne(activity, mapView, goOnline, localDatabaseNumber);
        }
    }

    private static void doSyncAllInOne(final MapEditorActivity activity, final MapView mapView, final boolean goOnline, int localDatabaseNumber) {
        try {

            Log.d(TAG, "Getting Local Geo database " + localDatabaseNumber);

            String databasePath = activity.getFilesDir().getPath() + GeoDatabaseUtil.ROOT_GEO_DATABASE_PATH + localDatabaseNumber + "/offlinedata.geodatabase";

            Geodatabase gdb = new Geodatabase(databasePath);

            SyncGeodatabaseParameters syncParams = gdb.getSyncParameters();

            CallbackListener<Map<Integer, GeodatabaseFeatureTableEditErrors>> syncResponseCallback = new CallbackListener<Map<Integer, GeodatabaseFeatureTableEditErrors>>() {
                @Override
                public void onCallback(Map<Integer, GeodatabaseFeatureTableEditErrors> objs) {
                    if (objs != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                                Utilities.dismissLoadingDialog();
                            }
                        });

                    } else {
                        Log.i(TAG, "Sync Completed Without Errors");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utilities.showToast(activity, activity.getResources().getString(R.string.sync_completed));
                                Utilities.dismissLoadingDialog();
                                if (goOnline) {
                                    preOnline(activity, mapView);
                                } else {
                                    activity.refreshPOI();
                                }
                            }
                        });
                    }
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    Log.i(TAG, "Error in Syncing");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                            Utilities.dismissLoadingDialog();
                        }
                    });

                }

            };

            GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
                @Override
                public void statusUpdated(GeodatabaseStatusInfo status) {
                    Log.i(TAG, "Syncing status " + status.getStatus().toString());
                    if (status.getStatus().getValue().matches(GeodatabaseStatusInfo.Status.COMPLETED.getValue())) {
                        //Remove shared preferences database
                        //of the features which has been added offline
                        SharedPreferences mPreferences = activity.getSharedPreferences(activity.getString(R.string.New_Drawed_Featur_ids), MODE_PRIVATE);
                        SharedPreferences.Editor editor = mPreferences.edit();
                        editor.clear();
                        editor.apply();
                        Log.i(TAG, "SharedPreference Has been cleared");
                    }
                }
            };

            gdbTask.syncGeodatabase(syncParams, gdb, statusCallback, syncResponseCallback);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Error in Syncing ");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                    Utilities.dismissLoadingDialog();
                }
            });

        }
    }

    public static boolean isGeoDatabaseLocal() {
        ArrayList<String> databaseTitles = DataCollectionApplication.getOfflineDatabasesTitle();
        for (String title : databaseTitles) {
            if (title != null)
                return true;
        }
        DataCollectionApplication.resetDatabaseNumber();
        return false;
    }

    private static void createTileCache(ExportTileCacheParameters params, final ExportTileCacheTask exportTileCacheTask, final String tileCachePath, final MapEditorActivity activity, final MapView mapView) {

        exportTileCacheTask.estimateTileCacheSize(params, new CallbackListener<Long>() {
            @Override
            public void onError(Throwable e) {
                error = true;
                e.printStackTrace();
                Log.d(TAG, "Error In Estimate Tile Cache Size");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                        Utilities.dismissLoadingDialog();
                    }
                });
            }

            @Override
            public void onCallback(Long size) {
                Log.d(TAG, "Tile Cache Size: " + size);
            }
        });

        CallbackListener<ExportTileCacheStatus> statusListener = new CallbackListener<ExportTileCacheStatus>() {

            @Override
            public void onError(Throwable e) {
                DataCollectionApplication.resetLanguage(activity);
                error = true;
                e.printStackTrace();
                Log.d(TAG, "Error In Export Tile Cache Status");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utilities.showToast(activity, activity.getResources().getString(R.string.connection_failed));
                        Utilities.dismissLoadingDialog();
                    }
                });

            }

            @Override
            public void onCallback(ExportTileCacheStatus objs) {
                Log.d(TAG, "Tile Cache Status: " + objs.getStatus().toString());
            }
        };


        CallbackListener<String> downLoadCallback = new CallbackListener<String>() {

            @Override
            public void onError(Throwable e) {
                DataCollectionApplication.resetLanguage(activity);
                error = true;
                e.printStackTrace();
                Log.d(TAG, "Error In generate Tile Cache");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorInDownloadDialog(activity, mapView);
                        Utilities.dismissLoadingDialog();
                    }
                });

            }

            @Override
            public void onCallback(String baseMapPath) {
                if (!error) {
                    DataCollectionApplication.resetLanguage(activity);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utilities.dismissLoadingDialog();
                        }
                    });
                    Log.i(TAG, "Base Map Downloaded Successfully");
                    addLocalLayers(mapView, activity, DataCollectionApplication.getDatabaseNumber());
                    DataCollectionApplication.setLocalDatabaseTitle(MapEditorActivity.localDatabaseTitle);
                    DataCollectionApplication.incrementDatabaseNumber();
                }
            }
        };

        exportTileCacheTask.generateTileCache(params, statusListener, downLoadCallback, tileCachePath);
    }

    private static void showErrorInDownloadDialog(final MapEditorActivity context, final MapView mapView) {
        errorInMapDownloadDismiss(context);
        context.errorInMapDownloadDialog = new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.dialog_connection_failed_title))
                .setMessage(context.getResources().getString(R.string.connection_failed))
                .setCancelable(false)
                .setPositiveButton(context.getResources().getString(R.string.try_again), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Utilities.isNetworkAvailable(context)) {
                            context.goOffline();
                        } else {
                            showErrorInDownloadDialog(context, mapView);
                        }
                    }
                }).setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!Utilities.isNetworkAvailable(context)) {
                            if (GeoDatabaseUtil.isGeoDatabaseLocal()) {
                                context.showOfflineMapsList(context, mapView);
                                Log.i("isGeoLocationDatabase", "one");
                            } else {
                                Log.i("isGeoLocationDatabase", "two");
                                MapEditorActivity.showNoOfflineMapDialog(context, mapView);
                            }
                        } else {
                            finishGoingOnline(context, mapView);
                        }
                    }
                })
                .show();

    }

    private static void errorInMapDownloadDismiss(final MapEditorActivity context) {

        if (context.errorInMapDownloadDialog != null && context.errorInMapDownloadDialog.isShowing())
            context.errorInMapDownloadDialog.dismiss();

    }


    private static void deleteLocalGeoDatabase(MapEditorActivity activity) {
        File fileGeo = new File(activity.getFilesDir().getPath() + GeoDatabaseUtil.ROOT_GEO_DATABASE_PATH + MapEditorActivity.currentOfflineVersion);
        File fileBaseMap = new File(activity.getFilesDir() + ROOT_BASE_MAP_PATH + MapEditorActivity.currentOfflineVersion);
        DataCollectionApplication.setLocalDatabaseTitle(null, MapEditorActivity.currentOfflineVersion);
        try {
            if (fileGeo.exists()) {
                fileGeo.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i(TAG, "deleteLocalGeoDatabase");

        try {

            if (fileBaseMap.exists()) {
                fileBaseMap.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * ------------------------------Ali Ussama Update----------------------------------------------
     */


    /**
     * Loads Raster file (Extension -> .TIF ) and adds it to a new RasterLayer. The RasterLayer is then added
     * to the map as an operational layer. Map viewpoint is then set based on the Raster's geometry.
     */

    public static void loadRaster(MapView mapView, Activity activity) {
        try {

            Log.i(TAG, "LoadRaster is called");
            final int REQUEST_CHOOSER = 1;
            // Create the ACTION_GET_CONTENT Intent
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            activity.startActivityForResult(Intent.createChooser(intent, "choose your map"), REQUEST_CHOOSER);

        } catch (Exception e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * ------------------------------Ali Ussama Update----------------------------------------------
     */
}
