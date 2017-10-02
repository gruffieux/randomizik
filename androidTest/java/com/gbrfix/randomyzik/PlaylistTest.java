package com.gbrfix.randomyzik;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteCursor;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by gab on 01.10.2017.
 */

@RunWith(AndroidJUnit4.class)
public class PlaylistTest {
    final static int TEST_CHECK_SIZE = 1;
    final static int TEST_PLAY_ALL_TRACKS = 2;
    final static int TEST_PLAY_ALL_ALBUMS = 3;

    int currentTest = 0;
    DbService dbService = null;
    AudioService audioService = null;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            DbService.DbBinder binder = (DbService.DbBinder)iBinder;
            dbService = binder.getService();
            dbService.setDbSignalListener(new DbSignal() {
                @Override
                public void onScanCompleted(final boolean update) {
                    switch (currentTest) {
                        case TEST_CHECK_SIZE:
                            MediaDAO dao = new MediaDAO(InstrumentationRegistry.getTargetContext());
                            dao.open();
                            SQLiteCursor cursor = dao.getAll();
                            assertEquals(2231, cursor.getCount());
                            dao.close();
                            break;
                    }
                }

                @Override
                public void onError(String msg) {
                    switch (currentTest) {
                        case TEST_CHECK_SIZE:
                            assertFalse(true);
                            break;
                    }
                }
            });
            dbService.setBound(true);
            if (currentTest > 0) {
                dbService.start();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dbService.setBound(false);
        }
    };

    private ServiceConnection audioConnection = new ServiceConnection() {
        int seek = 0;

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final AudioService.AudioBinder audioBinder = (AudioService.AudioBinder)iBinder;
            audioService = audioBinder.getService();
            audioService.setMediaSignalListener(new MediaSignal() {
                @Override
                public void onTrackRead(final boolean last) {
                }

                @Override
                public void onTrackResume(boolean start) {
                }

                @Override
                public void onTrackSelect(int id, int duration, int total, int totalRead) {
                }

                @Override
                public void onTrackProgress(final int position) {
                }
            });
            audioService.setBound(true);
            switch (currentTest) {
                case TEST_PLAY_ALL_TRACKS:
                    audioService.setMode(AudioService.MODE_TRACK);
                    break;
                case TEST_PLAY_ALL_ALBUMS:
                    audioService.setMode(AudioService.MODE_ALBUM);
                    break;
            }
            if (currentTest == TEST_PLAY_ALL_TRACKS || currentTest == TEST_PLAY_ALL_ALBUMS) {
                try {
                    audioService.resume();
                }
                catch (PlayEndException e) {
                    assertTrue(true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            audioService.setBound(false);
        }
    };

    public PlaylistTest() {
        DAOBase.NAME = "playlist-test.db";

        Context c = InstrumentationRegistry.getTargetContext();
        Intent intent = new Intent(c, DbService.class);
        c.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        Intent intent2 = new Intent(c, AudioService.class);
        c.bindService(intent2, audioConnection, Context.BIND_AUTO_CREATE);
    }

    @Test
    public void dropList() {
        Context c = InstrumentationRegistry.getTargetContext();
        MediaDAO dao = new MediaDAO(c);

        dao.open();
        dao.dropMedias();
        SQLiteCursor cursor = dao.getAll();
        assertEquals(0, cursor.getCount());
        dao.close();
    }

    @Test
    public void checkSize() {
        currentTest = TEST_CHECK_SIZE;
    }

    @Test
    public void playAllTracks() {
        currentTest = TEST_PLAY_ALL_TRACKS;
    }

    @Test
    public void playAllAlbums() {
        currentTest = TEST_PLAY_ALL_ALBUMS;
    }
}