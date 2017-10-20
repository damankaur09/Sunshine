package com.android.sunshine.webservices;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.android.sunshine.R;
import com.android.sunshine.callbacks.Updatable;
import com.android.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by Mohit Goel on 09-10-2017.
 */

public class FetchWeatherAsyncTask extends AsyncTask<String, Void, String[]>
{

    private static  final String LOG_TAG=FetchWeatherAsyncTask.class.getSimpleName();

    public Updatable updatable;

    private Context mContext;

    public FetchWeatherAsyncTask(Context mContext) {
        super();
        this.mContext = mContext;
    }

    /* The date/time conversion code is going to be moved  outside the asynctask later,
         * so for  convenience I'm breaking it out into its own method now.
        */

    private String getReadableDateFormat(long time)
    {
        SimpleDateFormat dateFormat=new SimpleDateFormat("EEE MMM dd");
        return dateFormat.format(time);
    }

    /**
     * Prepare the weather high/low for  presentation.
     */

    private String formatHighAndLow(double high,double low)
    {
        long roundedHigh=Math.round(high);
        long roundedLow=Math.round(low);

        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitTypeRequired=sharedPreferences.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_default)
        );
        if(unitTypeRequired.equals(mContext.getString(R.string.pref_units_metric)))
        {
            high=(high * 1.8) + 32;
            low=(low * 1.8) + 32;
        }
        else if(!unitTypeRequired.equals(mContext.getString(R.string.pref_units_metric)))
        {
            Log.d(LOG_TAG, "Unit type not found: " + unitTypeRequired);
        }

        String highLowStr=roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Take the string representing the complete forecast in JSON format and
     * pull out the data we need to construct the strings needed for the wireframes.
     *
     * Fortunately parsing is easy: constructor takes the JSON string and convert it
     * into an object hierarchy for us.
     */

    private String[] getWeatherDataFromJson(String forecastJsonStr,String locationSetting)  {
        //location information
        final String CITY="city";
        final String CITY_NAME="name";
        final String COORD="coord";

        //location coordinate
        final String LATITUDE="lat";
        final String LONGITUDE="lon";

        //Weather information, Each day's forecast info is an element  of the  list array
        final String LIST="list";
        final String PRESSURE="pressure";
        final String HUMIDITY="humidity";
        final String WIND_SPEED="speed";
        final String WIND_DIRECTION="deg";

        // All temperatures are children of the "temp" object.
        final String TEMPERATURE="temp";
        final String MAX="max";
        final String MIN="min";

        final String WEATHER="weather";
        final String DESCRIPTION="main";
        final String WEATHER_ID="id";

        try {
            JSONObject forecastJson=  new JSONObject(forecastJsonStr);
            JSONArray weatherArray=forecastJson.getJSONArray(LIST);

            JSONObject cityJson=forecastJson.getJSONObject(CITY);
            String cityName=cityJson.getString(CITY_NAME);

            JSONObject cityCoord=cityJson.getJSONObject(COORD);
            double cityLatitude=cityCoord.getDouble(LATITUDE);
            double cityLongitude=cityCoord.getDouble(LONGITUDE);

            long locationId=addLocation(locationSetting,cityName,cityLatitude,cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector=new Vector<>(weatherArray.length());


            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();

            for(int i=0;i<weatherArray.length();i++)
            {
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                JSONObject dayForecast=weatherArray.getJSONObject(i);

                dateTime = dayTime.setJulianDay(julianStartDay+i);

                pressure=dayForecast.getDouble(PRESSURE);
                humidity=dayForecast.getInt(HUMIDITY);
                windSpeed=dayForecast.getDouble(WIND_SPEED);
                windDirection=dayForecast.getDouble(WIND_DIRECTION);

                JSONObject currentCondition = dayForecast.getJSONArray(WEATHER).getJSONObject(0);
                description=currentCondition.getString(DESCRIPTION);
                weatherId=currentCondition.getInt(WEATHER_ID);

                JSONObject currentTemp=dayForecast.getJSONObject(TEMPERATURE);
                high=currentTemp.getDouble(MAX);
                low=currentTemp.getDouble(MIN);

                ContentValues weatherValues=new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY,locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE,dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY,humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE,pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES,windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMPERATURE,high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMPERATURE,low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,weatherId);

                cVVector.add(weatherValues);

            }

            if(cVVector.size()>0)
            {
                // Student: call bulkInsert to add the weatherEntries to the database here
            }

            String sortOrder= WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
            Uri weatherLocationUri= WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting,System.currentTimeMillis());

            String [] resultStrs=convertContentValuesToUXFormat(cVVector);
            return resultStrs;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */

    long addLocation(String locationSetting,String cityName,double lat,double lon)
    {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        return -1;
    }

    /*
        Students: This code will allow the FetchWeatherTask to continue to return the strings that
        the UX expects so that we can continue to test the application even once we begin using
        the database.
     */
    String [] convertContentValuesToUXFormat(Vector<ContentValues> cvv)
    {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for ( int i = 0; i < cvv.size(); i++ ) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighAndLow(
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMPERATURE),
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMPERATURE));
            resultStrs[i] = getReadableDateFormat(
                    weatherValues.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;
        }
        return resultStrs;
    }

    @Override
    protected String[] doInBackground(String... param) {

        if(param.length==0)
        {
                return null;
        }

        String locationQuery=param[0];
        HttpURLConnection urlConnection=null;
        BufferedReader reader=null;

        String forecastJsonStr=null;
        String format="json";
        String units="metric";
        String numOfDays="7";
        String appID="273d09eec63fef96db00f20143533b4d";

        final String FORECAST_BASE_URL="http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String QUERY_PARAM="q";
        final String FORMAT_PARAM="mode";
        final String UNITS_PARAM="units";
        final String DAYS_PARAM="cnt";
        final String APP_ID="appid";
        try {
            // Create a request to OpenWeatherMap, and open the connection

            Uri uri=Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM,param[0])
                    .appendQueryParameter(UNITS_PARAM,units)
                    .appendQueryParameter(FORMAT_PARAM,format)
                    .appendQueryParameter(DAYS_PARAM,numOfDays)
                    .appendQueryParameter(APP_ID,appID)
                    .build();

            URL url=new URL(uri.toString());
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into String
            InputStream inputStream=urlConnection.getInputStream();
            reader=new BufferedReader(new InputStreamReader(inputStream));

            StringBuffer buffer=new StringBuffer();

            if(inputStream==null)
            {
                return null;
            }

            String line;

            while((line=reader.readLine())!=null)
            {
                buffer.append(line+"\n");
            }

            if(buffer.length()==0)
            {
                return null;
            }

            forecastJsonStr=buffer.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(urlConnection!=null)
            {
                urlConnection.disconnect();
            }
            if(reader!=null)
            {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

            return getWeatherDataFromJson(forecastJsonStr, locationQuery);

    }

    @Override
    protected void onPostExecute(String[] result) {
        if(result !=null)
        {
            updatable.onWeatherUpdate(Arrays.asList(result));
        }

    }
}
