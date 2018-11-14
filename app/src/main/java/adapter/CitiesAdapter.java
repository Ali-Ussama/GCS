package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

import data.City;
import com.gcs.riyadh.R;

public class CitiesAdapter extends ArrayAdapter<City> {

    Context context;
    ArrayList<City> cities;
    ArrayList<City> filterCity;
    LayoutInflater inflater;
    private Filter filter;


    public CitiesAdapter(Context context, int resource, ArrayList<City> objects) {
        super(context, resource, objects);
        this.context = context;
        inflater = LayoutInflater.from(this.context);
        this.cities = objects;
        if (objects != null)
            this.filterCity = new ArrayList<>(objects);
    }

    @Override
    public int getCount() {
        if (cities == null)
            return 0;

        return cities.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public City getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_district, parent, false);
        }

        TextView venueName = (TextView) convertView.findViewById(R.id.districtName);
        venueName.setText(cities.get(position).getAname());
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

        ArrayAdapter<City> searchAdapter;

        VenueFilter(ArrayAdapter<City> searchAdapter) {
            this.searchAdapter = searchAdapter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<City> list = new ArrayList<City>(filterCity);
            FilterResults result = new FilterResults();

            // if no constraint is given, return the whole list
            if (constraint == null || constraint.length() == 0) {
                result.values = list;
                result.count = list.size();
            } else {
                String substr = constraint.toString();
                // iterate over the list of venues and find if the venue matches the constraint. if it does, add to the result list
                final ArrayList<City> retList = new ArrayList<City>();
                for (City venue : list) {
                    String cityName = "";

                    cityName = venue.getAname();

                    if (cityName.contains(constraint)) {
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
                for (City o : (ArrayList<City>) results.values) {
                    searchAdapter.add(o);
                }
            }
            searchAdapter.notifyDataSetChanged();
        }

    }
}
