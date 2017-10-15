package com.android.sunshine.webservices;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;

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

/**
 * Created by Mohit Goel on 09-10-2017.
 */

public class FetchWeatherAsyncTask extends AsyncTask<String, Void, String[]>
{

    private static  final String LOG_TAG=FetchWeatherAsyncTask.class.getSimpleName();

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

    private String[] getWeatherDataFromJson(String forecastJsonStr,int numOfDays) throws JSONException {
        final String LIST="list";
        final String WEATHER="weather";
        final String TEMPERATURE="temp";
        final String MAX="max";
        final String MIN="min";
        final String DESCRIPTION="main";


            JSONObject weatherObject=new JSONObject(forecastJsonStr);
            JSONArray weatherArray=weatherObject.getJSONArray(LIST);

            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            dayTime = new Time();


            String[] resultStrs = new String[numOfDays];
            for(int i=0;i<numOfDays;i++)
            {
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast=weatherArray.getJSONObject(i);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateFormat(dateTime);

                JSONArray currentWeather=dayForecast.getJSONArray(WEATHER);
               JSONObject currentCondition=currentWeather.getJSONObject(0);
                description=currentCondition.getString(DESCRIPTION);

                JSONObject currentTemp=dayForecast.getJSONObject(TEMPERATURE);
                double max=currentTemp.getDouble(MAX);
                double min=currentTemp.getDouble(MIN);

                highAndLow=formatHighAndLow(max,min);

                resultStrs[i]=day+"-"+ description + "-" +  highAndLow;

            }
            return resultStrs;

    }

    @Override
    protected String[] doInBackground(String... param) {

        if(param.length==0)
        {
                return null;
        }
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

        try
        {
            return getWeatherDataFromJson(forecastJsonStr, Integer.parseInt(numOfDays));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
       return null;
    }

    @Override
    protected void onPostExecute(String[] strings) {
        super.onPostExecute(strings);
    }
}
