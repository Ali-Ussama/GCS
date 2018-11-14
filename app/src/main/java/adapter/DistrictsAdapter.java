package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.esri.core.map.Feature;

import java.util.ArrayList;

import com.gcs.riyadh.R;

public class DistrictsAdapter extends ArrayAdapter<Feature> {

    public int CITY_ID = -1;
    Context context;
    ArrayList<Feature> districts;
    ArrayList<Feature> filterDisList;
    LayoutInflater inflater;
    private Filter filter;


    public DistrictsAdapter(Context context, int resource, ArrayList<Feature> objects) {
        super(context, resource, objects);
        this.context = context;
        inflater = LayoutInflater.from(this.context);
        this.districts = objects;
        if (objects != null) {
            this.filterDisList = new ArrayList<>(objects);
        }
    }

    @Override
    public int getCount() {
        if (districts == null)
            return 0;

        return districts.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Feature getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_district, parent, false);
        }

        TextView venueName = (TextView) convertView.findViewById(R.id.districtName);
        venueName.setText((String) districts.get(position).getAttributeValue("ANAME"));

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new VenueFilter(this);
        }
        return filter;
    }


    private class VenueFilter extends Filter {

        ArrayAdapter<Feature> searchAdapter;

        VenueFilter(ArrayAdapter<Feature> searchAdapter) {
            this.searchAdapter = searchAdapter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Feature> list = new ArrayList<Feature>(filterDisList);
            FilterResults result = new FilterResults();

            // if no constraint is given, return the whole list
            if (constraint == null || constraint.length() == 0) {
                result.values = list;
                result.count = list.size();
            } else {
                String substr = constraint.toString();
                // iterate over the list of venues and find if the venue matches the constraint. if it does, add to the result list
                final ArrayList<Feature> retList = new ArrayList<Feature>();
                for (Feature venue : list) {
                    String districtName = "";

                    districtName = (String) venue.getAttributeValue("ANAME");

                    if (districtName.contains(constraint)) {
                        retList.add(venue);
                    }
                }
                result.values = retList;
                result.count = retList.size();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // we clear the adapter and then pupulate it with the new results
            searchAdapter.clear();
            if (results.count > 0) {
                for (Feature o : (ArrayList<Feature>) results.values) {
                    searchAdapter.add(o);
                }
            }
            searchAdapter.notifyDataSetChanged();
        }

    }
}
