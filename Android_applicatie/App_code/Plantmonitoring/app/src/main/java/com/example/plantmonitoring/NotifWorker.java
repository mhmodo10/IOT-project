package com.example.plantmonitoring;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static androidx.core.content.ContextCompat.getSystemService;

public class NotifWorker extends Worker {
    private static final String CHANNEL_ID = "waterNotif";
    private String SensorOutput = "";
    private int checkMode = 0;
    private int amountDays = 0;
    private String chosenDays = "";

    public NotifWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    //runs whenever this class is called
    @NonNull
    @Override
    public Result doWork() {
        DatabaseHelper db = new DatabaseHelper(getApplicationContext());
        Cursor data = db.getPlant();

        //gets data from database
        while(data.moveToNext()){
            checkMode = data.getInt(1);
            amountDays = data.getInt(2);
            chosenDays = data.getString(3);
        }
        String thingSpeakUrl = "https://api.thingspeak.com/channels/1029560/feeds.json?api_key=9ZGRTHTBGCKI6EFV&results=2";
        OkHttpClient client = new OkHttpClient();

        //sends request to read data from thingspeak channel
        Request request = new Request.Builder()
                .url(thingSpeakUrl)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String thingSpeakResponse = response.body().string();
                    try {
                        //handles the response from thingspeak
                        JSONObject responseJson = new JSONObject(thingSpeakResponse);
                        JSONArray feedArray = responseJson.getJSONArray("feeds");
                        JSONObject channelArray = responseJson.getJSONObject("channel");
                        int lastEntryId = channelArray.getInt("last_entry_id");
                        int lastSensorUpdateIndex = lastEntryId - 1;
                        JSONObject SensorOutputArray = feedArray.getJSONObject(lastSensorUpdateIndex);
                        SensorOutput = SensorOutputArray.getString("field1");

                        //checks if the plant needs water
                        if(SensorOutput.equals("1")){
                            Log.d("TAG", "in if statement: ");
                            if(checkMode == 3){
                                LocalDate date = LocalDate.now();
                                DayOfWeek dayOfWeek = date.getDayOfWeek();
                                String weekDayString = dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH);
                                if(chosenDays.contains(weekDayString)){
                                    MakeNotification();
                                }

                            }
                            else{
                                MakeNotification();
                            }
                        }
                        Log.d("RESPONSE", "onResponse: " + SensorOutput);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

        return Result.success();
    }

    //notifies the user that the plant needs water
    private void MakeNotification(){
        Log.d("Notification", "the worker is working");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NotifChannel";
            String description = "Notifications for water";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.water_plant_notif)
                .setContentTitle("Water the plant")
                .setContentText("Your plant needs water")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        int notificationId = 1;
        notificationManager.notify(notificationId, builder.build());
    }
}
