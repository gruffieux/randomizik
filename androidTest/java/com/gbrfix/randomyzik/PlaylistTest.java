package com.gbrfix.randomyzik;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteCursor;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by gab on 01.10.2017.
 */

@RunWith(AndroidJUnit4.class)
public class PlaylistTest {
    final static int TEST_PLAY_ALL_TRACKS = 1;
    final static int TEST_PLAY_ALL_ALBUMS = 2;
    final static int TEST_PLAY_LAST_TRACK = 3;
    final static int TEST_PLAY_ENDED_LIST = 4;

    int currentTest, trackCount, trackTotal;
    AudioService audioService = null;

    private ServiceConnection audioConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final AudioService.AudioBinder audioBinder = (AudioService.AudioBinder)iBinder;
            audioService = audioBinder.getService();
            audioService.setMediaSignalListener(new MediaSignal() {
                @Override
                public void onTrackRead(boolean last) {
                    trackCount++;
                    if (last) {
                        switch (currentTest) {
                            case TEST_PLAY_ALL_TRACKS:
                            case TEST_PLAY_ALL_ALBUMS:
                            case TEST_PLAY_LAST_TRACK:
                            case TEST_PLAY_ENDED_LIST:
                                assertEquals(trackTotal, trackCount);
                                assertEquals(100, trackCount/trackTotal*100);
                                assertFalse(audioService.playerIsActive());
                                break;
                        }
                        currentTest = 0;
                    }
                }

                @Override
                public void onTrackResume(boolean allowed, boolean changeFocus) {
                }

                @Override
                public void onTrackSelect(int id, int duration, int total, int totalRead) {
                    switch (currentTest) {
                        case TEST_PLAY_LAST_TRACK:
                            assertEquals(total, totalRead);
                            break;
                    }
                }

                @Override
                public void onTrackProgress(int position) {
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
            switch (currentTest) {
                case TEST_PLAY_ALL_TRACKS:
                case TEST_PLAY_ALL_ALBUMS:
                case TEST_PLAY_LAST_TRACK:
                case TEST_PLAY_ENDED_LIST:
                    try {
                        audioService.setTest(true);
                        audioService.resume(true);
                    }
                    catch (PlayEndException e) {
                        assertFalse(audioService.playerIsActive());
                        currentTest = 0;
                    }
                    catch (Exception e) {
                        assertTrue(false);
                        currentTest = 0;
                    }
                    break;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            audioService.setBound(false);
        }
    };

    public PlaylistTest() {
        currentTest = trackCount = trackTotal = 0;
        DAOBase.NAME = "playlist-test.db";
    }

    @Test
    public void playAllTracks() {
        currentTest = TEST_PLAY_ALL_TRACKS;

        Context c = InstrumentationRegistry.getTargetContext();

        MediaDAO dao = new MediaDAO(c);
        dao.open();
        dao.updateFlagAll("unread");
        SQLiteCursor cursor = dao.getUnread();
        trackTotal = cursor.getCount();
        dao.close();

        Intent intent = new Intent(c, AudioService.class);
        c.bindService(intent, audioConnection, Context.BIND_AUTO_CREATE);

        while (currentTest != 0) {
        }

        Log.v("PlayAllTracks", "end");
    }

    @Test
    public void playAllAlbums() {
        currentTest = TEST_PLAY_ALL_ALBUMS;

        Context c = InstrumentationRegistry.getTargetContext();

        MediaDAO dao = new MediaDAO(c);
        dao.open();
        dao.updateFlagAll("unread");
        SQLiteCursor cursor = dao.getUnread();
        trackTotal = cursor.getCount();
        dao.close();

        Intent intent = new Intent(c, AudioService.class);
        c.bindService(intent, audioConnection, Context.BIND_AUTO_CREATE);

        while (currentTest != 0) {
        }

        Log.v("playAllAlbums", "end");
    }

    @Test
    public void playLastTrack() throws Exception {
        currentTest = TEST_PLAY_LAST_TRACK;

        Context c = InstrumentationRegistry.getTargetContext();

        MediaDAO dao = new MediaDAO(c);
        dao.open();
        dao.updateFlagAll("read");
        SQLiteCursor cursor = dao.getAll();
        Random random = new Random();
        int total = cursor.getCount();

        if (total > 1) {
            int pos = random.nextInt(total);
            cursor.moveToPosition(pos);
        }
        else if (total > 0) {
            cursor.moveToFirst();
        }
        else {
            throw new Exception("List is empty");
        }

        dao.updateFlag(cursor.getInt(0), "unread");
        trackTotal = 1;
        dao.close();

        Intent intent = new Intent(c, AudioService.class);
        c.bindService(intent, audioConnection, Context.BIND_AUTO_CREATE);

        while (currentTest != 0) {
        }

        Log.v("playLastTrack", "end");
    }

    @Test
    public void playEndedList() throws Exception {
        currentTest = TEST_PLAY_ENDED_LIST;

        Context c = InstrumentationRegistry.getTargetContext();

        MediaDAO dao = new MediaDAO(c);
        dao.open();
        dao.updateFlagAll("read");
        trackTotal = 0;
        dao.close();

        Intent intent = new Intent(c, AudioService.class);
        c.bindService(intent, audioConnection, Context.BIND_AUTO_CREATE);

        while (currentTest != 0) {
        }

        Log.v("playEndedList", "end");
    }
}
