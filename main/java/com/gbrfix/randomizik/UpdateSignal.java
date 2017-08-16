package com.gbrfix.randomizik;

/**
 * Created by gab on 09.08.2017.
 */

public interface UpdateSignal {
    void onTrackRead(boolean last);
    void onTrackResume(boolean start);
    void onTrackSelect(int id, int duration);
    void onTrackProgress(int position);
}
