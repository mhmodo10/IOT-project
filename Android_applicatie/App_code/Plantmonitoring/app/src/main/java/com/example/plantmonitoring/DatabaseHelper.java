package com.example.plantmonitoring;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.os.DropBoxManager;
import android.os.strictmode.SqliteObjectLeakedViolation;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private final static String PLANT_TABLE = "plant";
    private final static String PLANT_NAME = "name";
    private final static String PLANT_CHECK_MODE = "check_mode";
    private final static String AMOUNT_DAYS = "amount_days";
    private final static String CHOSEN_DAYS = "chosen_days";
    private final static String PLANT_IMAGE_URI = "image_uri";

    public DatabaseHelper(@Nullable Context context) {
        super(context, PLANT_TABLE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + PLANT_TABLE
                +  "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " + PLANT_NAME + " TEXT,"
                + PLANT_CHECK_MODE + " INTEGER,"
                + AMOUNT_DAYS + " INTEGER,"
                + CHOSEN_DAYS + " TEXT,"
                + PLANT_IMAGE_URI + " TEXT)";
        db.execSQL(createTableQuery);

    }
    public boolean addPlant(String plantName, int checkMode, int amountDays, String chosenDays, String imageUri){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(PLANT_NAME,plantName);
        contentValues.put(PLANT_CHECK_MODE,checkMode);
        contentValues.put(AMOUNT_DAYS,amountDays);
        contentValues.put(CHOSEN_DAYS,chosenDays);
        contentValues.put(PLANT_IMAGE_URI, imageUri);
        long data = db.insert(PLANT_TABLE,null,contentValues);
        if (data == -1) {
            return false;
        } else {
            return true;
        }
    }

    public boolean editPlant(String plantName, int checkMode, int amountDays, String chosenDays, String imageUri){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(PLANT_NAME,plantName);
        contentValues.put(PLANT_CHECK_MODE,checkMode);
        contentValues.put(AMOUNT_DAYS,amountDays);
        contentValues.put(CHOSEN_DAYS,chosenDays);
        contentValues.put(PLANT_IMAGE_URI,imageUri);

        long data = db.update(PLANT_TABLE,contentValues," WHERE ID = 1",null);
        if(data == -1){
            return false;
        }
        else {
            return true;
        }

    }

    public Cursor getPlant(){

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.query(PLANT_TABLE,new String[]{PLANT_NAME,PLANT_CHECK_MODE,AMOUNT_DAYS,CHOSEN_DAYS,PLANT_IMAGE_URI},null,null,null,null,"ID DESC","1");
        return data;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
