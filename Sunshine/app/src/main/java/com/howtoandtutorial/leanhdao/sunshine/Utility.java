package com.howtoandtutorial.leanhdao.sunshine;

/*
 * Utility:
 * - Gồm các hàm tiện ích
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {
    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_locationVN_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }

    static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    // Format date để lưu vào database.
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static String getFriendlyDayString(Context context, long dateInMillis) {
        // Hôm nay: "Hôm nay, Th11 28"
        // Ngày mai:  "Ngày mai"
        // 5 ngày tiếp theo: "Thứ tư" (Tên các ngày trong tuần)
        // Cho tất cả các ngày sau đó: "Thứ 5 Th6 24"

        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int julianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        // Nếu hôm nay thì format string: Hôm nay: "Hôm nay, Th11 28"
        if (julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateInMillis)));
        } else if ( julianDay < currentJulianDay + 7 ) {
            // 5 ngày tiếp theo: "Thứ tư" (Tên các ngày trong tuần)
            return getDayName(context, dateInMillis);
        } else {
            // Cho tất cả các ngày sau đó: "Thứ 5 Th6 24"
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE dd MMM");
            return shortenedDateFormat.format(dateInMillis);
        }

    }

    public static String getDayName(Context context, long dateInMillis) {

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    // Định dạng lại ngày từ database: "Thứ 2", e.g "Th5 24".
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }

    //Định dạng lại thông tin của gió
    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        //Hướng gió
        String direction = "Chưa xác định";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "Bắc";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "Đông Bắc";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "Đông";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "Đông Nam";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "Nam";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "Tây Nam";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "Tây";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "Tây Bắc";
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    // Xác định tình trạng thời tiết trong ngày và set icon tương ứng: như Rain, Clear.
    // Dành cho ví trí thứ 2 trở đi trong list view
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    // Xác định tình trạng thời tiết trong ngày và set art tương ứng: như Rain, Clear.
    // Dành cho ví trí thứ 1 trong list view: đó là Hôm nay
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }
}