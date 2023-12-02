// WeatherImpl.java
package example.grpcclient;

import io.grpc.stub.StreamObserver;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import service.*;

import java.io.IOException;

class WeatherImpl extends WeatherGrpc.WeatherImplBase {
    private static final String API_KEY = "58132ffec36a0183e43d28a34a758fb8"; // API key I got from OpenWeatherMap

    @Override
    public void getCurrentWeather(WeatherRequest req, StreamObserver<WeatherResponse> responseObserver) {
        String location = req.getLocation();
        String url = "http://api.openweathermap.org/data/2.5/weather?q=" + location + "&appid=" + API_KEY + "&units=imperial";

        try {
            JSONObject json = makeRequest(url);
            JSONObject main = json.getJSONObject("main");
            WeatherResponse response = WeatherResponse.newBuilder()
                    .setDescription(json.getJSONArray("weather").getJSONObject(0).getString("description"))
                    .setTemperature((float) main.getDouble("temp"))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getWeatherForecast(WeatherRequest req, StreamObserver<WeatherForecastResponse> responseObserver) {
        String location = req.getLocation();
        String url = "http://api.openweathermap.org/data/2.5/forecast?q=" + location + "&appid=" + API_KEY + "&units=imperial";

        try {
            JSONObject json = makeRequest(url);
            JSONArray list = json.getJSONArray("list");
            WeatherForecastResponse.Builder response = WeatherForecastResponse.newBuilder();

            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                JSONObject main = item.getJSONObject("main");
                WeatherResponse weather = WeatherResponse.newBuilder()
                        .setDescription(item.getJSONArray("weather").getJSONObject(0).getString("description"))
                        .setTemperature((float) main.getDouble("temp"))
                        .build();
                response.addForecast(weather);
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private JSONObject makeRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder responseContent = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            responseContent.append(line);
        }
        reader.close();
        conn.disconnect();

        return new JSONObject(responseContent.toString());
    }
}