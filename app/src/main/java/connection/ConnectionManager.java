package connection;

import java.util.concurrent.TimeUnit;

import data.Surveyor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by eslamelhoseiny on 11/1/17.
 */

public final class ConnectionManager {
    private static final ConnectionManager ourInstance = new ConnectionManager();
    private static final String BASE_URL = "http://54.187.60.111/CollectorMobileWebService/";
    private CollectorAPI api;
    public static ConnectionManager getInstance() {
        return ourInstance;
    }

    private ConnectionManager() {


        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        api = retrofit.create(CollectorAPI.class);
    }

    public Call<Surveyor> login(String userName, String password, String deviceId) {
        return api.login(userName, password, deviceId);
    }
}
