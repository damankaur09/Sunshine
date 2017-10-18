package com.android.sunshine.data;

import android.net.wifi.aware.PublishConfig;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by Mohit Goel on 17-10-2017.
 */

public class WeatherContract
{

    public static long normalizeDate(long startDate)
    {
        Time time=new Time();
        time.set(startDate);
        int julianDay=Time.getJulianDay(startDate,time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    public static  final class LocationEntry implements BaseColumns
    {
        public static final String TABLE_NAME="location";
    }

    public static final class WeatherEntry implements BaseColumns
    {
        public static final String TABLE_NAME="weather";

        //column with foreign into location table

        public static final String COLUMN_LOC_KEY="location_id";

        public static final String COLUMN_DATE="date";

        public static final String COLUMN_WEATHER_ID="weather_id";

        public static final String COLUMN_SHORT_DESC="short_desc";

        public static final String COLUMN_MAX_TEMPERATURE="max";

        public static final String COLUMN_MIN_TEMPERATURE="min";

        public static final String COLUMN_HUMIDITY="humidity";

        public static final String COLUMN_PRESSURE="pressure";

        public static final String COLUMN_WIND_SPEED="wind";

        public static final String COLUMN_DEGREES="degrees";
    }
}
