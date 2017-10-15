package com.android.sunshine.callbacks;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mohit Goel on 15-10-2017.
 */

public interface Updatable
{
    void onWeatherUpdate(List<String> weather);
}
