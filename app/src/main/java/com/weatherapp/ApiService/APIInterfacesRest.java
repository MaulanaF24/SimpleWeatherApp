package com.weatherapp.ApiService;



import com.weatherapp.WeatherModel.Example;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;



public interface APIInterfacesRest {

    @GET("weather")
    Call<Example> getWeatherData(@Query("lat")double lat,@Query("lon")double longitude);

    @GET("weather")
    Call<Example> getCityData(@Query("q")String city);

}

