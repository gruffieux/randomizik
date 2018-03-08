package com.gbrfix.randomyzik;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.widget.ListView;

/**
 * Created by gab on 14.10.2017.
 */

public class SingleTrackDialogFragment extends AppCompatDialogFragment {
    private int id;
    private MediaDAO dao;

    public void setId(int id) {
        this.id = id;
    }

    private void resetFlag(final Activity activity) {
        dao.open();
        dao.updateFlag(id, "unread");
        dao.close();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    dao.open();
                    SQLiteCursor cursor = dao.getAllOrdered();
                    ListView listView = activity.findViewById(R.id.playlist);
                    TrackCursorAdapter adapter = (TrackCursorAdapter) listView.getAdapter();
                    adapter.changeCursor(cursor);
                    dao.close();
                } catch (SQLException e) {
                    Log.v("SQLException", e.getMessage());
                }
            }
        });
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity)getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        dao = new MediaDAO(getContext());

        dao.open();
        SQLiteCursor cursor = dao.getFromId(id);
        cursor.moveToFirst();
        String title = cursor.getString(4);
        dao.close();

        builder.setMessage(getText(R.string.edit_single_track_msg))
            .setTitle(title)
            .setPositiveButton(getText(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetFlag(activity);
                }
            })
            .setNeutralButton(R.string.dialog_yes_play, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetFlag(activity);
                    Bundle args = new Bundle();
                    args.putInt("id", id);
                    activity.mediaBrowser.sendCustomAction("selectTrack", args, null);
                    MediaControllerCompat.getMediaController(activity).getTransportControls().play();
                }
            })
            .setNegativeButton(getText(R.string.dialog_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

        return builder.create();
    }
}
