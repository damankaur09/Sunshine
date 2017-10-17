package com.android.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sunshine.callbacks.Updatable;
import com.android.sunshine.webservices.FetchWeatherAsyncTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements Updatable {

    ArrayAdapter<String> mForecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView=inflater.inflate(R.layout.fragment_main,container,false);
        String [] fakeData=
                {
                        "Today - Sunny-88/63",
                        "Tommorow - Foggy-70/46",
                        "Weds - Cloudy-72/63",
                        "Thurs - Rainy-64/51",
                        "Fri - Foggy-70/46",
                        "Sat - Sunny-76/68"
                };

        ArrayList<String> weekForecast=new ArrayList<>(Arrays.asList(fakeData));

        mForecastAdapter=new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,R.id.list_item_forecast_textview,weekForecast);

        ListView listView=(ListView)rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CharSequence tempString =((TextView)view).getText();
                Intent intent=new Intent(getActivity(),DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT,tempString);
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment,menu);
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.action_refresh:
               updateWeather();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    private void updateWeather()
    {
        FetchWeatherAsyncTask weatherAsyncTask=new FetchWeatherAsyncTask(getActivity());
        weatherAsyncTask.updatable=this;

        SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location=sharedPref.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        weatherAsyncTask.execute(location);
    }

    @Override
    public void onWeatherUpdate(List<String> weather) {
        mForecastAdapter.clear();

         mForecastAdapter.addAll(weather);
    }
}
