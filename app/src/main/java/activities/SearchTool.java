package activities;

/**
 * Created by Eslam El-hoseiny on 9/28/2016.
 */

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.EsriSecurityException;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.ArrayList;

import adapter.CitiesAdapter;
import adapter.DistrictsAdapter;
import data.City;
import com.gcs.riyadh.R;
import util.CustomAutoComplete;
import util.DataCollectionApplication;
import util.Utilities;

public class SearchTool implements Callback {

    private static SearchTool searchTool;
    GraphicsLayer graphicLayer;
    Context context;
    CustomAutoComplete tvCity, tvDistrict;
    ArrayList<City> cities;
    CitiesAdapter searchAdapterCity;
    DistrictsAdapter districtsAdapter;
    MapView mapView;
    SpatialReference spatialReference;
    ArrayList<Feature> districts;
    AsyncQueryTaskForDistrict asyncQueryTaskForDistrict;
    AsyncQueryTaskForCities asyncCities;

    private SearchTool(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        spatialReference = mapView.getSpatialReference();
        graphicLayer = new GraphicsLayer();
        mapView.addLayer(graphicLayer);
        initAsyncTasks();
        if (Utilities.isNetworkAvailable(context)) {
            if (!asyncCities.getStatus().equals(AsyncTask.Status.RUNNING)) {
                if (asyncCities.getStatus().equals(AsyncTask.Status.FINISHED))
                    asyncCities = new AsyncQueryTaskForCities();
                asyncCities.execute(DataCollectionApplication.CitiesLayer);
            } else {
                Utilities.showLoadingDialog(context);
            }
        }
    }

    public static SearchTool getInstance(Context context, MapView mapView) {

        if (searchTool == null) {
            searchTool = new SearchTool(context, mapView);
        }

        return searchTool;
    }

    private void initAsyncTasks() {
        asyncCities = new AsyncQueryTaskForCities();
        asyncQueryTaskForDistrict = new AsyncQueryTaskForDistrict();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        View v = LayoutInflater.from(context).inflate(R.layout.actionbar_search, null);
        tvCity = (CustomAutoComplete) v.findViewById(R.id.search_box);
        tvDistrict = (CustomAutoComplete) v.findViewById(R.id.search_box_option);
        mode.setCustomView(v);
        if (cities != null && cities.size() > 0) {
            searchAdapterCity = new CitiesAdapter(context, R.layout.item_district, new ArrayList<>(cities));
            tvCity.setAdapter(searchAdapterCity);
            searchByDistrict();
        }

        ((MapEditorActivity)context).tvLatLong.setVisibility(View.GONE);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
//        mapView.removeLayer(graphicLayer);
        MapEditorActivity.SearchCase = 0;
        if (tvCity != null && tvCity.isPopupShowing())
            tvCity.dismissDropDown();
        if (tvDistrict != null && tvDistrict.isPopupShowing())
            tvDistrict.dismissDropDown();

        if (asyncQueryTaskForDistrict.getStatus() == AsyncTask.Status.RUNNING) {
            asyncQueryTaskForDistrict.cancel(true);
            Log.d("Test", "Async District Canceled");
        }

        ((MapEditorActivity)context).tvLatLong.setVisibility(View.VISIBLE);
        graphicLayer.removeAll();

    }

    private void searchByDistrict() {
        tvDistrict.setError(null);
        tvCity.setOnEditorActionListener(null);
        tvDistrict.setOnEditorActionListener(null);
        tvDistrict.setAdapter(null);

        tvDistrict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvDistrict.showDropDown();
            }
        });
        tvCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvCity.showDropDown();
            }
        });

        tvDistrict.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    tvDistrict.showDropDown();
            }
        });
        tvCity.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    tvCity.showDropDown();
            }
        });

        if (cities != null) {
            searchAdapterCity = new CitiesAdapter(context, R.layout.item_district, new ArrayList<>(cities));
            tvCity.setAdapter(searchAdapterCity);
        }

        tvDistrict.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                tvDistrict.setText((String) districtsAdapter.getItem(position).getAttributeValue("ANAME"));

                mapView.setExtent(districtsAdapter.getItem(position).getGeometry());

                SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.RED, 3, SimpleLineSymbol.STYLE.SOLID);
                if (graphicLayer != null) {
                    graphicLayer.removeAll();
                    graphicLayer.addGraphic(new Graphic(districtsAdapter.getItem(position).getGeometry(), lineSymbol));
                }

                Utilities.hideKeyBoard(context);
            }
        });

        tvCity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                tvCity.setText((String) searchAdapterCity.getItem(position).getAname());

                Utilities.zoomToCity((Point) searchAdapterCity.getItem(position).getGeometry(), mapView);

                int CITY_ID = searchAdapterCity.getItem(position).getCityId();

                tvDistrict.setAdapter(null);
                tvDistrict.setText("");

                getDistrictsByCityId(CITY_ID);

            }
        });

        tvCity.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvDistrict.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        tvCity.performClick();
        Utilities.hideKeyBoard(context);
    }


    private void getDistrictsByCityId(int cityId) {
        if (Utilities.isNetworkAvailable(context)) {
            asyncQueryTaskForDistrict = new AsyncQueryTaskForDistrict();
            asyncQueryTaskForDistrict.execute(context.getResources().getString(R.string.BaseMap) + DataCollectionApplication.DistrictsLayer, "CITY_ID  = " + cityId);
        }
    }

    private City convertFeatureToCity(Feature feature, boolean b) {
        if (b) {
            City city = new City();
            city.setAname((String) feature.getAttributeValue("CITY_NAME_AR"));
            city.setEname(((String) feature.getAttributeValue("CITY_NAME_EN")));
            city.setCityId((int) feature.getAttributeValue("CITY_ID"));
            city.setGeometry(feature.getGeometry());
            return city;
        } else {
            City city = new City();
            city.setAname((String) feature.getAttributeValue("ANAME"));
            city.setEname(((String) feature.getAttributeValue("ENAME")));
            city.setCityId((int) ((double) feature.getAttributeValue("CITY_ID")));
            city.setGeometry(feature.getGeometry());
            return city;
        }
    }

    private class AsyncQueryTaskForDistrict extends AsyncTask<String, Void, FeatureResult> {

        @Override
        protected void onPreExecute() {
            Utilities.showLoadingDialog(context);
        }

        @Override
        protected FeatureResult doInBackground(String... queryArray) {
            String url = queryArray[0];
            QueryParameters qParameters = new QueryParameters();
            String whereClause = queryArray[1];
            qParameters.setOutFields(new String[]{"CITY_ID", "OBJECTID", "ENAME", "ANAME"});
            qParameters.setReturnGeometry(true);
            qParameters.setWhere(whereClause);
            qParameters.setOutSpatialReference(spatialReference);
            QueryTask qTask = null;
            try {
                qTask = new QueryTask(url, MapEditorActivity.mapServiceToken);
            } catch (EsriSecurityException e) {
                e.printStackTrace();
            }


            try {
                FeatureResult results = qTask.execute(qParameters);
                return results;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(FeatureResult results) {

            Utilities.dismissLoadingDialog();
            if (results != null) {
                districts = new ArrayList<Feature>();
                for (Object element : results) {
                    if (element instanceof Feature) {
                        try {
                            final Feature feature = (Feature) element;
                            String name = "";
                            name = ((String) feature.getAttributeValue("ANAME")).trim();
                            if (name != null && !name.equals("null") && !name.equals(""))
                                districts.add(feature);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (MapEditorActivity.SearchCase != 0) {

                    if (districts.size() == 0) {
                        tvDistrict.setError("No District In this Area", ContextCompat.getDrawable(context, android.R.drawable.screen_background_light_transparent));
                        tvDistrict.setAdapter(null);
                        tvDistrict.setText("");
                        tvDistrict.requestFocus();
                        tvDistrict.performClick();
                    } else {
                        tvDistrict.setError(null);
                        districtsAdapter = new DistrictsAdapter(context, R.layout.item_district, districts);
                        tvDistrict.setAdapter(districtsAdapter);
                        tvDistrict.setText("");
                        tvDistrict.requestFocus();
                        tvDistrict.performClick();
                    }
                }
            }

        }
    }

    private class AsyncQueryTaskForCities extends AsyncTask<String, Void, FeatureResult> {

        @Override
        protected void onPreExecute() {
            Utilities.showLoadingDialog(context);
        }

        @Override
        protected FeatureResult doInBackground(String... queryArray) {

            String url = context.getResources().getString(R.string.BaseMap) + DataCollectionApplication.MainCitiesLayer;
            final QueryParameters qParameters = new QueryParameters();
            qParameters.setOutFields(new String[]{"CITY_ID", "CITY_NAME_AR", "CITY_NAME_EN"});
            qParameters.setReturnGeometry(true);
            qParameters.setWhere("1=1");
            qParameters.setOutSpatialReference(spatialReference);

            QueryTask qTask = null;
            try {
                qTask = new QueryTask(url, MapEditorActivity.mapServiceToken);
            } catch (EsriSecurityException e) {
                e.printStackTrace();
            }

            FeatureResult results = null;
            FeatureResult results2 = null;

            try {
                results = qTask.execute(qParameters);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String url2 = context.getResources().getString(R.string.BaseMap) + DataCollectionApplication.CitiesLayer;
            final QueryParameters qParameters2 = new QueryParameters();
            qParameters2.setOutFields(new String[]{"CITY_ID", "ANAME", "ENAME"});
            qParameters2.setReturnGeometry(true);
            qParameters2.setWhere("1=1");
            qParameters2.setOutSpatialReference(spatialReference);

            QueryTask qTask2 = null;
            try {
                qTask2 = new QueryTask(url2, MapEditorActivity.mapServiceToken);
            } catch (EsriSecurityException e) {
                e.printStackTrace();
            }


            try {
                results2 = qTask2.execute(qParameters2);
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (results2 == null && results != null)
                return results;
            else if (results == null && results2 != null)
                return results2;
            else if (results != null && results2 != null) {
                cities = new ArrayList<City>();
                for (Object element : results) {
                    if (element instanceof Feature) {
                        final Feature feature = (Feature) element;
                        cities.add(convertFeatureToCity(feature, true));
                    }
                }
                for (Object element : results2) {
                    if (element instanceof Feature) {
                        final Feature feature = (Feature) element;
                        cities.add(convertFeatureToCity(feature, false));
                    }
                }
                return results;
            }

            return null;

        }

        @Override
        protected void onPostExecute(FeatureResult results) {

            Utilities.dismissLoadingDialog();
            if (results != null) {
                if (MapEditorActivity.SearchCase != 0) {
                    searchAdapterCity = new CitiesAdapter(context, R.layout.item_district, new ArrayList<>(cities));
                    tvCity.setAdapter(searchAdapterCity);
                    searchByDistrict();
                }
            }
        }

    }
}
