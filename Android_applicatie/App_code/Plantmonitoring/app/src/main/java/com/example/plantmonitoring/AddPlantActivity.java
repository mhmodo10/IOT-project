package com.example.plantmonitoring;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.IOException;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddPlantActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final int PICK_IMAGE_CODE = 1001;
    public String prevStarted = "prevStarted";
    private ImageButton backBtn,addPicBtn;
    private TextView plantName;
    private EditText plantNameInput;
    private TextView frequency;
    private RadioGroup frequencyOptions;
    private RadioButton everyDay,everyXDays,everyWeekDay;
    private SeekBar daysAmountBar;
    private TextView monday,tuesday,wednesday,thursday,friday,saturday,sunday;
    private boolean firstChoiceClicked,secondChoiceClicked,thirdChoiceClicked = false;
    private ArrayList<String> chosenDays = new ArrayList<>();
    private int chosenAmountDays = 1;
    private int chosenFrequency = 0;
    private Uri chosenPicUri;
    private String takenPicturePath;
    private Button addPlantBtn;
    private boolean plantExist = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_plant_activity);
        initializeVars();
        chooseDaysAmount();
        chooseWeekDay();
        getDataFromDatabase();
        addPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPicture();
            }
        });

        addPlantBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SavePlant();
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivityIntent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(mainActivityIntent);
                finish();
            }
        });
    }

    //gets the chosen image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_CODE && resultCode == RESULT_OK) {
            assert data != null;
            chosenPicUri = data.getData();
            addPicBtn.setImageURI(chosenPicUri);
        }
    }

    //checks if storage permissions are given and calls ChoosePic()
    private void addPicture(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ){
                String[] permissionrequest = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissionrequest, PERMISSION_REQUEST_CODE);
                if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    ChoosePic();
            }
            else{
                ChoosePic();
            }
        }
        else{
            ChoosePic();
        }
    }

    //checks if all fields are filled in, saves the data and instantiates the PeriodicWorkRequest to make notifications
    private void SavePlant(){
        String plantName = plantNameInput.getText().toString();
        if(plantName.equals("")){
            Toast.makeText(getApplicationContext(),"Enter plant name",Toast.LENGTH_SHORT).show();
        }
        else if(chosenPicUri == null){
            Toast.makeText(getApplicationContext(),"Take a picture of the plant",Toast.LENGTH_SHORT).show();
        }
        else if(chosenFrequency == 0){
            Toast.makeText(getApplicationContext(),"choose a frequency",Toast.LENGTH_SHORT).show();
        }
        else {
            if(chosenFrequency == 2 && chosenAmountDays == 0){
                Toast.makeText(getApplicationContext(),"Choose a higher number of days",Toast.LENGTH_SHORT).show();
            }
            else if(chosenFrequency == 3 && chosenDays.size() == 0){
                Toast.makeText(getApplicationContext(),"Choose at least one day of the week",Toast.LENGTH_SHORT).show();
            }
            else{
                DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                String chosenWeekDays = TextUtils.join(",",chosenDays);
                if(chosenFrequency == 3){
                    if(db.addPlant(plantName,chosenFrequency,chosenAmountDays,chosenWeekDays,chosenPicUri.toString())) {
                        Toast.makeText(getApplicationContext(),"Plant added successfully",Toast.LENGTH_SHORT).show();
                        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                        PeriodicWorkRequest pWorkRequest = new PeriodicWorkRequest.Builder(NotifWorker.class,1, TimeUnit.DAYS)
                                .setInitialDelay(12,TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .build();
                        Intent openMain = new Intent(getApplicationContext(),MainActivity.class);
                        startActivity(openMain);
                        finish();

                    }
                    else{
                        Toast.makeText(getApplicationContext(),"couldnt add plant",Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    if(db.addPlant(plantName,chosenFrequency,chosenAmountDays,"",chosenPicUri.toString())) {
                        Toast.makeText(getApplicationContext(),"Plant added successfully",Toast.LENGTH_SHORT).show();
                        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                        if(chosenFrequency == 1){
                                PeriodicWorkRequest pWorkRequest = new PeriodicWorkRequest.Builder(NotifWorker.class,1, TimeUnit.DAYS)
                                        .setInitialDelay(12,TimeUnit.HOURS)
                                        .setConstraints(constraints)
                                        .build();
                            WorkManager.getInstance(getApplicationContext()).cancelAllWork();
                            WorkManager.getInstance(getApplicationContext()).enqueue(pWorkRequest);
                        }
                        else{
                            PeriodicWorkRequest pWorkRequest = new PeriodicWorkRequest.Builder(NotifWorker.class,chosenAmountDays, TimeUnit.DAYS)
                                    .setInitialDelay(chosenAmountDays,TimeUnit.DAYS)
                                    .setConstraints(constraints)
                                    .build();
                            WorkManager.getInstance(getApplicationContext()).cancelAllWork();
                            WorkManager.getInstance(getApplicationContext()).enqueue(pWorkRequest);
                        }
                        Intent openMain = new Intent(getApplicationContext(),MainActivity.class);
                        startActivity(openMain);
                        finish();
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"couldnt add plant",Toast.LENGTH_SHORT).show();
                    }
                }

            }
        }
    }

    //gets the data from the sqlite database to be able to edit it
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
        }

        chosenPicUri = Uri.parse(imageUriString);
        Log.d("TAG", "getDataFromDatabase: " + String.valueOf(frequencyMode));
        if(!plantNameText.equals("")){
            addPicBtn.setImageURI(Uri.parse(imageUriString));
            plantNameInput.setText(plantNameText);
            plantExist = true;
            switch (frequencyMode) {
                case 1:
                    FirstRadioChosen();
                    break;
                case 2:
                    SecondRadioChosen(chosenAmountOfDays);
                    break;
                case 3:
                    ThirdRadioChosen(chosenWeekDays);
                    break;
            }

        }
    }

    //starts the activity of taking a pic
    private void ChoosePic(){
        Intent chooseImageFromGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        chooseImageFromGallery.setType("image/*");
        startActivityForResult(chooseImageFromGallery,PICK_IMAGE_CODE);

    }

    //handles the seekbar changes
    private void chooseDaysAmount() {
        daysAmountBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                chosenAmountDays = progress;
                everyXDays.setText(String.format("Every %s Days", String.valueOf(progress)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //initializes all variables of the view
    private void initializeVars() {
        backBtn = findViewById(R.id.back_btn);
        addPicBtn = findViewById(R.id.add_plant_pic);
        plantName = findViewById(R.id.plant_name_label);
        plantNameInput = findViewById(R.id.plant_name_input);
        frequency = findViewById(R.id.frequency);
        frequencyOptions = findViewById(R.id.frequency_options);
        everyDay = findViewById(R.id.every_day_radio_button);
        everyXDays = findViewById(R.id.days_amount_radio_button);
        everyWeekDay = findViewById(R.id.weekdays_radio_button);
        daysAmountBar = findViewById(R.id.days_seekbar);
        monday = findViewById(R.id.monday);
        tuesday = findViewById(R.id.tuesday);
        wednesday = findViewById(R.id.wednseday);
        thursday = findViewById(R.id.thursday);
        friday = findViewById(R.id.friday);
        saturday = findViewById(R.id.saturday);
        sunday = findViewById(R.id.sunday);
        daysAmountBar.setVisibility(View.GONE);
        addPlantBtn = findViewById(R.id.submit_plant);
    }

    //gets called when the third radio button gets toggled
    public void thirdChoiceClicked(View view) {
        chosenFrequency = 3;
        daysAmountBar.setVisibility(View.GONE);
        everyXDays.setText("Every _ Days");
        chosenAmountDays = 1;
    }

    //gets called when the second radio button get toggled
    public void secondChoiceClicked(View view) {
        chosenFrequency = 2;
        daysAmountBar.setVisibility(View.VISIBLE);
        monday.setTextColor(getColor(R.color.weekDaysColor));
        tuesday.setTextColor(getColor(R.color.weekDaysColor));
        wednesday.setTextColor(getColor(R.color.weekDaysColor));
        thursday.setTextColor(getColor(R.color.weekDaysColor));
        friday.setTextColor(getColor(R.color.weekDaysColor));
        saturday.setTextColor(getColor(R.color.weekDaysColor));
        sunday.setTextColor(getColor(R.color.weekDaysColor));
        chosenDays.clear();
    }

    //gets called when the first radio button gets called
    public void firstChoiceClicked(View view) {
        chosenFrequency = 1;
        daysAmountBar.setVisibility(View.GONE);
        everyXDays.setText("Every _ Days");
        monday.setTextColor(getColor(R.color.weekDaysColor));
        tuesday.setTextColor(getColor(R.color.weekDaysColor));
        wednesday.setTextColor(getColor(R.color.weekDaysColor));
        thursday.setTextColor(getColor(R.color.weekDaysColor));
        friday.setTextColor(getColor(R.color.weekDaysColor));
        saturday.setTextColor(getColor(R.color.weekDaysColor));
        sunday.setTextColor(getColor(R.color.weekDaysColor));
        chosenDays.clear();
        chosenAmountDays = 1;
    }

    //handles week days selection
    private void chooseWeekDay(){
        monday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenFrequency == 3){
                    if(chosenDays.contains("Monday")){
                        monday.setTextColor(getResources().getColor(R.color.weekDaysColor));
                        chosenDays.remove("Monday");
                    }
                    else{
                        monday.setTextColor(getResources().getColor(R.color.colorAccent));
                        chosenDays.add("Monday");
                    }

                }
            }
        });
        tuesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chosenFrequency == 3) {
                    if (chosenDays.contains("Tuesday")) {
                        tuesday.setTextColor(getColor(R.color.weekDaysColor));
                        chosenDays.remove("Tuesday");
                    } else {
                        tuesday.setTextColor(getColor(R.color.colorAccent));
                        chosenDays.add("Tuesday");
                    }
                }
            }
        });
        wednesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenFrequency == 3) {
                    if (chosenDays.contains("Wednesday")) {
                        wednesday.setTextColor(getColor(R.color.weekDaysColor));
                        chosenDays.remove("Wednesday");
                    } else {
                        wednesday.setTextColor(getColor(R.color.colorAccent));
                        chosenDays.add("Wednesday");
                    }
                }
            }
        });
        thursday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenFrequency == 3) {
                    if (chosenDays.contains("Thursday")) {
                        thursday.setTextColor(getColor(R.color.weekDaysColor));
                        chosenDays.remove("Thursday");
                    } else {
                        thursday.setTextColor(getColor(R.color.colorAccent));
                        chosenDays.add("Thursday");
                    }
                }
            }
        });
        friday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenFrequency == 3) {
                    if (chosenDays.contains("Friday")) {
                        friday.setTextColor(getColor(R.color.weekDaysColor));
                        chosenDays.remove("Friday");
                    } else {
                        friday.setTextColor(getColor(R.color.colorAccent));
                        chosenDays.add("Friday");
                    }
                }
            }
        });
        saturday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenFrequency == 3) {
                    if (chosenDays.contains("Saturday")) {
                        saturday.setTextColor(getColor(R.color.weekDaysColor));
                        chosenDays.remove("Saturday");
                    } else {
                        saturday.setTextColor(getColor(R.color.colorAccent));
                        chosenDays.add("Saturday");
                    }
                }
            }
        });
        sunday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenFrequency == 3) {
                    if (chosenDays.contains("Sunday")) {
                        sunday.setTextColor(getColor(R.color.weekDaysColor));
                        chosenDays.remove("Sunday");
                    } else {
                        sunday.setTextColor(getColor(R.color.colorAccent));
                        chosenDays.add("Sunday");
                    }
                }
            }
        });
    }

    //used to toggle first radio button when in edit mode
    private void FirstRadioChosen(){
        everyDay.toggle();
        chosenFrequency = 1;
        daysAmountBar.setVisibility(View.GONE);
        everyXDays.setText("Every _ Days");
        monday.setTextColor(getColor(R.color.weekDaysColor));
        tuesday.setTextColor(getColor(R.color.weekDaysColor));
        wednesday.setTextColor(getColor(R.color.weekDaysColor));
        thursday.setTextColor(getColor(R.color.weekDaysColor));
        friday.setTextColor(getColor(R.color.weekDaysColor));
        saturday.setTextColor(getColor(R.color.weekDaysColor));
        sunday.setTextColor(getColor(R.color.weekDaysColor));
        chosenDays.clear();
        chosenAmountDays = 1;
    }

    //used to toggle second radio button when in edit mode
    private void SecondRadioChosen(int amountDays){
        everyXDays.toggle();
        everyXDays.setText(String.format(Locale.US,"Every %d Days", amountDays));
        chosenFrequency = 2;
        daysAmountBar.setVisibility(View.VISIBLE);
        daysAmountBar.setProgress(amountDays);
        monday.setTextColor(getColor(R.color.weekDaysColor));
        tuesday.setTextColor(getColor(R.color.weekDaysColor));
        wednesday.setTextColor(getColor(R.color.weekDaysColor));
        thursday.setTextColor(getColor(R.color.weekDaysColor));
        friday.setTextColor(getColor(R.color.weekDaysColor));
        saturday.setTextColor(getColor(R.color.weekDaysColor));
        sunday.setTextColor(getColor(R.color.weekDaysColor));
        chosenDays.clear();
    }

    //used to toggle third radio button when in edit mode
    private void ThirdRadioChosen(String weekDays){
        everyWeekDay.toggle();
        chosenFrequency = 3;
        daysAmountBar.setVisibility(View.GONE);
        everyXDays.setText("Every _ Days");
        chosenAmountDays = 1;
        if(weekDays.contains("Monday")){
            monday.setTextColor(getColor(R.color.colorAccent));
        }
        if(weekDays.contains("Tuesday")){
            tuesday.setTextColor(getColor(R.color.colorAccent));
        }
        if(weekDays.contains("Wednesday")){
            wednesday.setTextColor(getColor(R.color.colorAccent));
        }
        if(weekDays.contains("Thursday")){
            thursday.setTextColor(getColor(R.color.colorAccent));
        }
        if(weekDays.contains("Friday")){
            friday.setTextColor(getColor(R.color.colorAccent));
        }
        if(weekDays.contains("Saturday")){
            saturday.setTextColor(getColor(R.color.colorAccent));
        }
        if(weekDays.contains("Sunday")){
            sunday.setTextColor(getColor(R.color.colorAccent));
        }
    }
}
