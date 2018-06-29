package com.gbrfix.randomyzik;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by gab on 16.07.2017.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String MEDIAS_TABLE_CREATE = "CREATE TABLE `medias` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "`path` TEXT, `flag` TEXT, " +
            "`track_nb` TEXT, `title` TEXT, `album` TEXT, `artist` TEXT, " +
            "`media_id` INTEGER);";
    public static final String MEDIA_TABLE_DROP = "DROP TABLE `medias`;";

    private Context context;

    public DatabaseHandler(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);

        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(MEDIAS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(MEDIA_TABLE_DROP);
            db.execSQL(MEDIAS_TABLE_CREATE);
        }
        if (oldVersion == 2 && newVersion >= 3) {
            try {
                db.execSQL("ALTER TABLE `medias` ADD `track_nb` TEXT;");
            }
            catch (SQLiteException e) {
                Log.v("SQLiteException", e.getMessage());
            }
        }
        if (oldVersion <= 3 && newVersion >= 4) {
            try {
                db.execSQL("ALTER TABLE `medias` ADD `title` TEXT;");
                db.execSQL("ALTER TABLE `medias` ADD `album` TEXT;");
                db.execSQL("ALTER TABLE `medias` ADD `artist` TEXT;");

                // On corrige tous les meta tags
                fixMediaTags(db, true, true, true, true);
            }
            catch (Exception e) {
                Log.v("Exception", e.getMessage());
            }
        }
        if (oldVersion <= 4 && newVersion >= 5) {
            try {
                // On corrige les titres
                fixMediaTags(db, false, true, false, false);
            }
            catch (Exception e) {
                Log.v("Exception", e.getMessage());
            }
        }
        if (oldVersion <= 5 && newVersion >= 6) {
            try {
                // On corrige les artistes
                fixMediaTags(db, false, false, false, true);
            }
            catch (Exception e) {
                Log.v("Exception", e.getMessage());
            }
        }
        if (oldVersion <= 6 && newVersion >= 7) {
            try {
                db.execSQL("ALTER TABLE `medias` ADD `media_id` INTEGER;");
                fixMediaTags(db, true, true, true, true);
            }
            catch (Exception e) {
                Log.v("Exception", e.getMessage());
            }
        }
    }

    private void fixMediaTags(SQLiteDatabase db, boolean trackNb, boolean title, boolean album, boolean artist) {
        Cursor cursor = context.getContentResolver().query(DbService.MEDIA_URI, new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
            }, null, null, null);

        while (cursor.moveToNext()) {
            int mediaId = cursor.getInt(0);
            String path = cursor.getString(1);
            ContentValues values = new ContentValues();
            if (trackNb) {
                values.put("track_nb", cursor.getString(2));
            }
            if (title) {
                values.put("title", cursor.getString(3));
            }
            if (album) {
                values.put("album", cursor.getString(4));
            }
            if (artist) {
                values.put("artist", cursor.getString(5));
            }
            if (DAOBase.VERSION > 7) {
                db.update("medias", values, "`media_id`=?", new String[] {String.valueOf(mediaId)});
            } else {
                values.put("media_id", mediaId);
                db.update("medias", values, "`path`=?", new String[]{path});
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int newVersion, int oldVersion) {
    }
}
