package com.android.sunshine.webservices;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by Mohit Goel on 09-10-2017.
 */

public class FetchWeatherAsyncTask extends AsyncTask<String,Void,Void>
{

    private static  final String LOG_TAG=FetchWeatherAsyncTask.class.getSimpleName();

    @Override
    protected Void doInBackground(String... param) {

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
        String appID="";

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

        return null;
    }


}
