package com.example.weather_rss;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ArrayList<Integer> indexes;
    TextView textView;
    ListView weatherList;
    Spinner spinner;
    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        indexes = new ArrayList<>(
                Arrays.asList(629634, 625144, 524901, 5368361, 498817));

        textView = (TextView) findViewById(R.id.text);
        weatherList = findViewById(R.id.weatherList);

        spinner = (Spinner) findViewById(R.id.cities_spinner);
        ArrayAdapter<CharSequence> adapt = ArrayAdapter.createFromResource(this,
                R.array.cities_array, android.R.layout.simple_spinner_item);
        adapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapt);

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "weatherDB").allowMainThreadQueries().build();
    }

    public void onButtonClicked(View view) {
        RequestQueue queue = Volley.newRequestQueue(this);
        int city = indexes.get(spinner.getSelectedItemPosition());
        String url ="https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/" + city;

        weatherList.setAdapter(null);
        textView.setText("");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    WeatherXmlParser parser = new WeatherXmlParser();
                    if(parser.parse(response))
                    {
                        ArrayList<WeatherReply> rep = parser.getWeathers();
                        ArrayList<Weather> dat = new ArrayList<>();
                        for (int i = 0; i < rep.size(); i++) {
                            try {
                                WeatherReply pr = rep.get(i);
                                pr.setId(i + 1);
                                Weather w = new WeatherData(pr).getWeather();
                                w.city = city;
                                dat.add(w);
                            }catch(Exception ignored){}
                        }
                        CustomAdapter adapter = new CustomAdapter(dat, getApplicationContext());
                        weatherList.setAdapter(adapter);
                        List<Weather> rov = db.weatherDao().findByCity(city);
                        for (Weather w : rov)
                            db.weatherDao().delete(w);
                        for (Weather w : dat)
                            db.weatherDao().insert(w);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("Проблемы с интернетом");
                List<Weather> lst = db.weatherDao().findByCity(city);
                if(lst.size() == 0){
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Город не найден", Toast.LENGTH_SHORT);
                    toast.show();

                }else{
                    CustomAdapter adapter = new CustomAdapter((ArrayList<Weather>) lst, getApplicationContext());
                    weatherList.setAdapter(adapter);
                }
            }
        });
        queue.add(stringRequest);
    }
}