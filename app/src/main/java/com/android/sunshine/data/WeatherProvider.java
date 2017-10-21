package com.android.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by Mohit Goel on 19-10-2017.
 */

public class WeatherProvider extends ContentProvider
{
    //The URI Matcher used by this Content Provider
    private static final UriMatcher sUriMatcher=buildUriMatcher();

    private WeatherDBHelper mOpenHelper;

    static final int WEATHER=100;
    static final int WEATHER_WITH_LOCATION=101;
    static final int WEATHER_WITH_LOCATION_AND_DATE=102;
    static final int LOCATION=103;

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    static
    {
        sWeatherByLocationSettingQueryBuilder=new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id=location._id

        sWeatherByLocationSettingQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }

    //location.location_setting=?
    private static final String sLocationSettingSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    //location.location_setting=? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_CITY_NAME + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    //location.location_setting=? AND date=?
    private static final String sLocationSettingAndDaySelection =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri,String[] projection,String sortOrder)
    {
        String locationSetting=WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate= WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String [] selectionArgs;
        String selection;

        if(startDate==0)
        {
            selection=sLocationSettingSelection;
            selectionArgs=new String[]{locationSetting};
        }
        else
        {
            selectionArgs=new String[]{locationSetting,Long.toString(startDate)};
            selection=sLocationSettingWithStartDateSelection;
        }

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getLocationSettingAndDate(Uri uri,String[] projection,String sortOrder)
    {
        String locationSetting= WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long date= WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[]{locationSetting,Long.toString(date)},
                null,
                null,
                sortOrder);
    }

    static UriMatcher buildUriMatcher()
    {
        final UriMatcher matcher=new UriMatcher(UriMatcher.NO_MATCH);
        final String authority=WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority,WeatherContract.PATH_WEATHER,WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*",WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper=new WeatherDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor setCursor;
        switch (sUriMatcher.match(uri))
        {
            case WEATHER_WITH_LOCATION_AND_DATE:
                setCursor=getLocationSettingAndDate(uri,projection,sortOrder);
                break;
            case WEATHER_WITH_LOCATION:
                setCursor=getWeatherByLocationSetting(uri,projection,sortOrder);
                break;
            case WEATHER:
                setCursor=mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case LOCATION:
                setCursor=mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: "+uri);
        }
        setCursor.setNotificationUri(getContext().getContentResolver(),uri);
        return setCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match=sUriMatcher.match(uri);

        switch (match)
        {
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: "+uri);
        }

    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db=mOpenHelper.getWritableDatabase();
        final int match=sUriMatcher.match(uri);

        Uri returnUri;
        switch (match)
        {
            case WEATHER: {
                normalizeDate(contentValues);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, contentValues);
                if (_id > 0) {
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                } else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri "+uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings)
    {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    private void normalizeDate(ContentValues values)
    {
        if(values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE))
        {
            long dateValue=values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE,WeatherContract.normalizeDate(dateValue));
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db=mOpenHelper.getWritableDatabase();
        final int match=sUriMatcher.match(uri);

        switch (match)
        {
            case WEATHER:
                db.beginTransaction();
                int returnCount=0;
                try
                {
                    for(ContentValues value:values)
                    {
                        normalizeDate(value);
                        long _id=db.insert(WeatherContract.WeatherEntry.TABLE_NAME,null,value);
                        if(_id != 1)
                        {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                }
                finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri,null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }

    }

    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
