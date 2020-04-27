package com.example.plantmonitoring;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public String prevStarted = "prevStarted";
    private ImageButton closeBtn,editBtn;
    private TextView plantName,lastWateredDate,frequencyType;
    private ImageView plantImage;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initianlize();
        getDataFromDatabase();
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startAddPlantActivity = new Intent(getApplicationContext(),AddPlantActivity.class);
                startActivity(startAddPlantActivity);
                finish();
            }
        });
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initianlize() {
        closeBtn = findViewById(R.id.close_btn);
        editBtn = findViewById(R.id.edit_plant);
        plantName = findViewById(R.id.plant_name);
        frequencyType = findViewById(R.id.frequency_type);
        plantImage = findViewById(R.id.plant_image);
    }
    private void getDataFromDatabase(){
        DatabaseHelper db = new DatabaseHelper(getApplicationContext());
        Cursor data = db.getPlant();
        String plantNameText = "";
        String imageUriString = "";
        int frequencyMode = 0;
        int chosenAmountOfDays = 0;
        String chosenWeekDays = "";
        while (data.moveToNext()){
            plantNameText = data.getString(0);
            imageUriString = data.getString(4);
            frequencyMode = data.getInt(1);
            chosenAmountOfDays = data.getInt(2);
            chosenWeekDays = data.getString(3);
            chosenWeekDays = chosenWeekDays.replace(",",", ");
        }

        Uri imageUri = Uri.parse(imageUriString);
        Log.d("TAG", "getDataFromDatabase: " + imageUriString);
        if(!imageUriString.equals("") && !plantNameText.equals("")){
            plantImage.setImageURI(imageUri);
            plantName.setText(plantNameText);
            if(frequencyMode == 1){
                frequencyType.setText("Every day");
            }
            else if(frequencyMode == 2){
                frequencyType.setText(String.format(Locale.US,"Every %d Days",chosenAmountOfDays));
            }
            else if(frequencyMode == 3){
                Log.d("TAG", "getDataFromDatabase: " + chosenWeekDays);
                frequencyType.setText("Each " + chosenWeekDays);
            }

        }
        else{
            Intent openActivity = new Intent(this,AddPlantActivity.class);
            startActivity(openActivity);
            finish();
        }


        Log.d("TAG", "getDataFromDatabase: " + imageUri.toString());
    }
}
