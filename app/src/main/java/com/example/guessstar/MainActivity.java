package com.example.guessstar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.autofill.FieldClassification;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private Button button0;
    private Button button1;
    private Button button2;
    private Button button3;
    private ImageView imageViewFighter;

    private String url = "https://vmma.ru/reyting-boycov-mma/";

    private ArrayList<String> urls;
    private ArrayList<String> names;
    private ArrayList<Button> buttons;
    private int numberOfQuestion;
    private int numberOfRightAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        button0 = findViewById(R.id.button0);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        buttons = new ArrayList<>();
        buttons.add(button0);
        buttons.add(button1);
        buttons.add(button2);
        buttons.add(button3);
        imageViewFighter = findViewById(R.id.imageViewFighter);
        urls = new ArrayList<>();
        names = new ArrayList<>();
        getContent();
        playGame();
    }

    // когда классы готовы, создадим метод, который будет получать контент

    private void getContent(){
        DownloadContentTask task = new DownloadContentTask();
        // и получаем из него контент
        try {
            String content = task.execute(url).get();
            String start = "<ul class=\"\"><li class='fighter-item-big Ultimate Fighting Championship'>";
            String finish = "<h2 class=\"screen-reader-text\">Навигация по записям</h2>";
            Pattern pattern = Pattern.compile(start + "(.*?)" + finish);
            Matcher matcher = pattern.matcher(content);
            String splitContent = "";
            while (matcher.find()){
                splitContent = matcher.group(1);
            }
            // теперь из новой строки нам нужно получить два массива, в одном мы будем хранить все имена в другом все картинки, для этого нам нужно создать два Pattern
            Pattern patternImg = Pattern.compile("<img src='(.*?)'");
            Pattern patternName = Pattern.compile("alt='(.*?)' />");
            Matcher matcherImg = patternImg.matcher(splitContent);
            Matcher matcherName = patternName.matcher(splitContent);
            while (matcherImg.find()){
                urls.add(matcherImg.group(1));
            }
            while (matcherName.find()){
                names.add(matcherName .group(1));
            }
            for(String s : urls){
                Log.i("MyResult", s);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void playGame(){
        generateQuestion();
        DownloadImageTask task = new DownloadImageTask();
        try {
            Bitmap bitmap = task.execute(urls.get(numberOfQuestion)).get();
            if( bitmap != null){
                imageViewFighter.setImageBitmap(bitmap);
                for (int i = 0; i < buttons.size(); i++){
                    if (i == numberOfRightAnswer){
                        buttons.get(i).setText(names.get(numberOfQuestion));
                    } else {
                        int wrongAnswer = generateWrongAnswer();
                        buttons.get(i).setText(names.get(wrongAnswer));
                    }
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private void generateQuestion(){
        numberOfQuestion = (int) (Math.random() * names.size());
        numberOfRightAnswer = (int) (Math.random() * buttons.size());

    }

    private int generateWrongAnswer(){
        return (int) (Math.random() * names.size());
    }

    public void Answer(View view) {
        Button button = (Button) view;
        String tag = button.getTag().toString();
        if (Integer.parseInt(tag) == numberOfRightAnswer){
            Toast.makeText(this, "Верно! Красавчик!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Неверно! На самом деле: " + names.get(numberOfQuestion), Toast.LENGTH_SHORT).show();
        }
        playGame();
    }

    // здесь нам понадобятся два класса, рассширяющие AsyncTask, один для загрузки контента, второй для загрузки изображения
    private static class DownloadContentTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            // создаём наши эллементы
            URL url = null;
            HttpURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();
            // присваеваем значение нашему URL и получаем его из наших параметров, оборачиваем всё это в try catch
            try {
                url = new URL(strings[0]);
                // открываем соединение, сделаем приведение типов к HTTPurlconnection
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                    // теперь необходимо получить наш InputStream (поток ввода), мы получим его из нашего urlConnection
                    InputStream inputStream = urlConnection.getInputStream();
                    // теперь необходимо создать reader, создаём его из InputStream
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    // и для того чтобы читать данные по строчно создаём BufferedReader
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    // будем читать по строчно
                    String line = reader.readLine();
                    // и будем читать до тех пор, пока наша строка не будет равна null
                    while (line != null) {
                        // после того как мы прочитали строку мы добавляем её в наш result
                        result.append(line);
                        // и строчке присваиваем значение
                        line = reader.readLine();
                    }
                    // когда чтение закончено возвращаем StringBuilder приведённый к строке
                    return result.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            URL url = null;
            HttpURLConnection urlConnection = null;
            StringBuilder result = new StringBuilder();

            try {
                url = new URL(strings[0]);

                try {
                    urlConnection = (HttpURLConnection) url.openConnection();

                    InputStream inputStream = urlConnection.getInputStream();
                    // создаём Bitmap из нашего InputStream
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;


                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
