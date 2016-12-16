package com.howtoandtutorial.leanhdao.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.content.Context.MODE_PRIVATE;

/*
 * ForecastFragment:
 * - Tạo fragment trong MainActivity.
 * - Cập nhật giao diện: Địa điểm - Last update, list thời tiết, action floating button Refresh, SwipeRefresh method.
 * - Kiểm tra kết nối mạng từ đó load thông tin từ API hoặc từ SQLite
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    private static final int FORECAST_LOADER = 0;
    public static final String LAST_UPDATE_PREFERENCES = "LocationAndLastUpdate";
    private TextView locationAndLastUpdate;
    private ImageButton imgbtnRefresh;
    private ForecastAdapter mForecastAdapter;

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }
    SwipeRefreshLayout swipeRefreshLayout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Liên kết và đặt Listener cho action floating button Refresh
        imgbtnRefresh = (ImageButton) getActivity().findViewById(R.id.button_refresh);
        imgbtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int status = NetworkUtil.getConnectivityStatusString(getContext());
                if(status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
                    Toast.makeText(getContext(), "Vui lòng kết nối mạng", Toast.LENGTH_LONG).show();
                }
                else{
                    updateWeather();
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override //tạo giao diện
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        int status = NetworkUtil.getConnectivityStatusString(getContext());
        if(status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
            Toast.makeText(getContext(), "Vui lòng kết nối mạng để cập nhật thông tin thời tiết mới nhất", Toast.LENGTH_LONG).show();
            //Địa điểm và thời gian câp nhật cuối cùng
            locationAndLastUpdate = (TextView)rootView.findViewById(R.id.location_and_last_update);
            locationAndLastUpdate.setText(restoringPreferences());
        }
        else{
            locationAndLastUpdate = (TextView)rootView.findViewById(R.id.location_and_last_update);
            locationAndLastUpdate.setText(showLocationAndLastUpdate());
        }

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        //Liên kết SwipeRefresh Layout
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        // Lắng nghe khi người dùng click vào list view
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                    startActivity(intent);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // Load lại thông tin khi thay đổi ví trí trong Settings
    void onLocationChanged( ) {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    //Hàm load thông tin
    public void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());

        locationAndLastUpdate = (TextView)getView().findViewById(R.id.location_and_last_update);
        locationAndLastUpdate.setText(showLocationAndLastUpdate());
        savingPreferences();
        weatherTask.execute(location);
    }

    //Lấy vị trí và thời gian trả về chuỗi String
    public String showLocationAndLastUpdate(){
        String location = Utility.getPreferredLocation(getActivity());
        Calendar c = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        String lastUpdate = location + " - Last update: " + timeFormat.format(c.getTime());
        return lastUpdate;
    }

    //Lấy thông tin từ SharedPreferences
    private String restoringPreferences() {
        SharedPreferences pre = getContext().getSharedPreferences(LAST_UPDATE_PREFERENCES, MODE_PRIVATE);
        //lấy giá trị lastUpdate ra, nếu không thấy thì giá trị mặc định là null
        String lastUpdate = pre.getString("lastUpdate", null);
        return lastUpdate;
    }

    //Lưu thông tin vào SharedPreferences
    private void savingPreferences() {
        //tạo đối tượng getSharedPreferences
        SharedPreferences pre = getContext().getSharedPreferences(LAST_UPDATE_PREFERENCES, MODE_PRIVATE);
        //tạo đối tượng Editor để lưu thay đổi
        SharedPreferences.Editor editor = pre.edit();
        String lastUpdate = showLocationAndLastUpdate();
        if(null != lastUpdate)
        {
            //lưu vào editor
            editor.putString("lastUpdate", lastUpdate);
        }
        //chấp nhận lưu xuống file
        editor.commit();
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        // Sắp xếp:  ASC, theo ngày.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());
        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override //Vuốt xuống để Refresh
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(false);
                int status = NetworkUtil.getConnectivityStatusString(getContext());
                if(status == NetworkUtil.NETWORK_STATUS_NOT_CONNECTED){
                    Toast.makeText(getContext(), "Vui lòng kết nối mạng", Toast.LENGTH_LONG).show();
                }
                else{
                    updateWeather();
                }
            }
        }, 1300);// thời gian chờ là 1.3s
    }
}