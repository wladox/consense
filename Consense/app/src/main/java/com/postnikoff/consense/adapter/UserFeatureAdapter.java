package com.postnikoff.consense.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.postnikoff.consense.R;
import com.postnikoff.consense.model.UserFeature;

import java.util.ArrayList;

/**
 * Created by CodeX on 17.08.2015.
 */
public class UserFeatureAdapter extends ArrayAdapter<UserFeature> {

    private final Context context;
    private final ArrayList<UserFeature> userFeatureList;

    public UserFeatureAdapter(Context context, ArrayList<UserFeature> userFeatureList) {
        super(context, R.layout.user_feature_list_item, userFeatureList);

        this.context = context;
        this.userFeatureList = userFeatureList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.user_feature_list_item, parent, false);

        TextView categoryIdView = (TextView) rowView.findViewById(R.id.category_view);
        TextView featureNameView = (TextView) rowView.findViewById(R.id.feature_name_view);

        categoryIdView.setText(userFeatureList.get(position).getCategoryName());
        featureNameView.setText(userFeatureList.get(position).getFeatureName());

        return rowView;

    }
}
