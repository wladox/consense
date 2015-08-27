package com.postnikoff.consense.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.postnikoff.consense.R;
import com.postnikoff.consense.geo.MyGeofence;

import java.util.List;

/**
 * Created by CodeX on 27.08.2015.
 */
public class GeofenceAdapter extends ArrayAdapter<MyGeofence> {

    private final Context context;
    private final List<MyGeofence> geofenceList;

    public GeofenceAdapter(Context context, int resource, List<MyGeofence> objects) {
        super(context, resource, objects);
        this.context = context;
        this.geofenceList = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.geofence_list_item, parent, false);
        rowView.setId(geofenceList.get(position).getGeofenceId());
        TextView geofenceName = (TextView) rowView.findViewById(R.id.geofence_name_view);
        geofenceName.setText(geofenceList.get(position).getName());

        return rowView;

    }
}
