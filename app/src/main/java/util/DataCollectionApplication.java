package util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import data.BookMark;


public class DataCollectionApplication extends MultiDexApplication {

    static final String BOOKMARKS = "bookmarks";
    static final String SENT_TOKEN_TO_SERVER = "fcm_token";
    static final String BOOKMARKS_COUNT = "bookmarksCount";
    static final String SharedPreferenceName = "DataCollection";
    static final String SurveyorName = "SurveyorName";
    static final String SurveyorId = "SurveyorId";
    static final String ShourtCut = "ShourtCut";
    private static Locale currentLocale;

    //ids for the layers
    public static String MainCitiesLayer = "/17";
    public static String CitiesLayer = "/19";
    public static String DistrictsLayer = "/60";

    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;

    static final String GeoSharedPreferenceName = "FileNum";
    static SharedPreferences GeoDatabaseFilesSharedPreference;
    static SharedPreferences.Editor GeoDatabaseFilesEditor;


    public static int getDatabaseNumber() {
        return GeoDatabaseFilesSharedPreference.getInt("number", 1);
    }

    public static void resetDatabaseNumber() {
        GeoDatabaseFilesEditor.putInt("number", 1);
        GeoDatabaseFilesEditor.commit();
    }

    public static void incrementDatabaseNumber() {
        GeoDatabaseFilesEditor.putInt("number", getDatabaseNumber() + 1);
        GeoDatabaseFilesEditor.commit();
    }
    public static void setLocalDatabaseTitle(String title){
        GeoDatabaseFilesEditor.putString(String.valueOf(getDatabaseNumber()), title );
        GeoDatabaseFilesEditor.commit();
    }
    public static void setLocalDatabaseTitle(String title, int number){
        GeoDatabaseFilesEditor.putString(String.valueOf(number), title );
        GeoDatabaseFilesEditor.commit();
    }

    public static String getDatabases(int key){
        return GeoDatabaseFilesSharedPreference.getString(String.valueOf(key), null);
    }


    public static ArrayList<String> getOfflineDatabasesTitle() {
        ArrayList<String> titles = new ArrayList<String>();
        for (int i = 1; i < getDatabaseNumber(); i++) {
            titles.add(getDatabases(i));
        }
        return titles;
    }


    private static void initPreference(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(SharedPreferenceName, 0);
            editor = sharedPreferences.edit();
        }
        if (GeoDatabaseFilesSharedPreference == null) {
            GeoDatabaseFilesSharedPreference = context.getSharedPreferences(GeoSharedPreferenceName, 0);
            GeoDatabaseFilesEditor = GeoDatabaseFilesSharedPreference.edit();

        }

    }

    public static boolean isShortCutCreated() {
        int x = getInteger(ShourtCut);
        if (x != 0) {
            return true;
        } else {
            setInteger(ShourtCut, 1);
            return false;
        }
    }

    public static String getSurveyorName() {
        return sharedPreferences.getString(SurveyorName, null);
    }

    public static void setSurveyorName(String name) {
        editor.putString(SurveyorName, name);
        editor.commit();
    }

    public static int getSurveyorId() {
        return sharedPreferences.getInt(SurveyorId, -1);
    }

    public static void setSurveyorId(int id) {
        editor.putInt(SurveyorId, id);
        editor.commit();
    }

    public static void addBookMark(String json, String title) {
        try {
            int index = getInteger(BOOKMARKS_COUNT);
            BookMark bookMark = new BookMark(title, json, index);
            String bookmarkJson = Utilities.convertObjectToJsonString(bookMark);
            setString(BOOKMARKS + index, bookmarkJson);
            setInteger(BOOKMARKS_COUNT, ++index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeBookMark(int index) {

        try {
            setString(BOOKMARKS + index, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static ArrayList<BookMark> getAllBookMarks() {

        ArrayList<BookMark> bookMarks = new ArrayList<>();

        int count = getInteger(BOOKMARKS_COUNT);

        for (int i = 0; i < count; i++) {
            String str = getString(BOOKMARKS + i);
            if (str != null)
                bookMarks.add(Utilities.convertStringToObject(str, BookMark.class));
        }

        return bookMarks;
    }

    public static void logout() {
        setSurveyorName(null);
        setSurveyorId(-1);
        setIsSentToServer(false);
    }

    public static boolean isSentToServer() {
        return sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false);
    }

    public static void setIsSentToServer(boolean isSentToServer) {
        editor.putBoolean(SENT_TOKEN_TO_SERVER, isSentToServer);
        editor.commit();
    }

    public static String getString(String name) {

        return sharedPreferences.getString(name, null);
    }

    public static void setString(String name, String value) {

        editor.putString(name, value);
        editor.commit();
    }

    public static int getInteger(String name) {

        return sharedPreferences.getInt(name, 0);
    }

    public static void setInteger(String name, int value) {

        editor.putInt(name, value);
        editor.commit();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        initPreference(getApplicationContext());
    }

    public static void changeLocal(Context context){


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            currentLocale = context.getResources().getConfiguration().getLocales().get(0);
        }else{
            currentLocale = context.getResources().getConfiguration().locale;
        }

        Log.d("Language","Current Language is " + currentLocale.getLanguage());
        Log.d("Language","Change Language to " + "EN");


        String languageToLoad  = "en"; // your language
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        }else{
            config.locale = locale;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.createConfigurationContext(config);
        }else {
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }

    public static void resetLanguage(Context context) {
         if(currentLocale != null){
            Log.d("Current Locale" , currentLocale.getLanguage());

            String languageToLoad  = currentLocale.getLanguage(); // your language
            Log.d("Language","Set Language to " + languageToLoad);
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale);
            }else{
                config.locale = locale;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                context.createConfigurationContext(config);
            }else {
                context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            }
        }else{
             Log.d("Current Locale is" , currentLocale+"");

         }

    }

}
