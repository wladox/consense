package com.postnikoff.consense.network;

import android.app.ListActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.postnikoff.consense.R;
import com.postnikoff.consense.model.User;
import com.postnikoff.consense.model.UserFeature;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        String userList = getIntent().getStringExtra("userlist");
        List<User> users = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            JSONArray array = new JSONArray(userList);
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                User user = new User();
                user.setUserId(jsonObject.getInt("userId"));
                user.setUsername(jsonObject.getString("username"));
                user.setEmail(jsonObject.getString("email"));
                try {
                    user.setBirthday(sdf.parse(jsonObject.getString("birthday")));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                users.add(user);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayAdapter<User> userArrayAdapter = new ArrayAdapter<>(this, R.layout.user_list_item,
                R.id.user_item_textview, users);

        setListAdapter(userArrayAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String selectedItem = (String) getListView().getItemAtPosition(position);

        Toast.makeText(UserListActivity.this, "you clicked " + selectedItem + " with id: "
                + id, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
