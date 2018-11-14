package connection;

import data.Surveyor;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by eslamelhoseiny on 11/1/17.
 */

interface CollectorAPI {

    @FormUrlEncoded
    @POST("CollectorSer.asmx/GetByPassAndName")
    Call<Surveyor> login(@Field("Name") String userName, @Field("Pass") String password, @Field("DeviceId") String deviceId);

}
