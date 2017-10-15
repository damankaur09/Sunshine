package com.android.sunshine.webservices;

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

public class FetchWeatherAsyncTask extends AsyncTask<Void,Void,Void>
{

    private static  final String LOG_TAG=FetchWeatherAsyncTask.class.getSimpleName();

    @Override
    protected Void doInBackground(Void... voids) {

        HttpURLConnection urlConnection=null;
        BufferedReader reader=null;

        String forecastJsonStr=null;

        String baseURl="http://api.openweathermap.org/data/2.5/forecast/daily?q=560034&units=metric&mode=json&cnt=7";
        String apiKey="";
        try {
            // Create a request to OpenWeatherMap, and open the connection
            URL url=new URL(baseURl.concat(apiKey));
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
