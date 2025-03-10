package com.hexa.downloader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hexa.downloader.config.Config;


public class DefaultDownloadHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME_DOWNLOAD_INFO = "download_info";
    public static final String TABLE_NAME_DOWNLOAD_THREAD_INFO = "download_thread_info";
    private static final String SQL_CREATE_DOWNLOAD_TABLE = String.format(
            "CREATE TABLE %s (_id varchar(255) PRIMARY KEY NOT NULL,supportRanges integer NOT NULL,createAt long NOT NULL,uri varchar(255) NOT NULL,path varchar(255) NOT NULL,size long NOT NULL, progress long NOT NULL,status integer NOT NULL);",
            TABLE_NAME_DOWNLOAD_INFO);
    private static final String SQL_CREATE_DOWNLOAD_THREAD_TABLE = String.format(
            "CREATE TABLE %s (_id integer PRIMARY KEY NOT NULL,threadId integer NOT NULL,downloadInfoId varchar(255) NOT NULL,uri varchar(255) NOT NULL,start long NOT NULL,end long NOT NULL,progress long NOT NULL);",
            TABLE_NAME_DOWNLOAD_THREAD_INFO);
    private static final int DB_VERSION = 1;

    public DefaultDownloadHelper(Context context, Config config) {
        super(context, config.getDatabaseName(), null, config.getDatabaseVersion());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    private void createTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DOWNLOAD_TABLE);
        db.execSQL(SQL_CREATE_DOWNLOAD_THREAD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO upgrade database

    }
}




