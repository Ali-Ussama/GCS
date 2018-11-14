package util;

import android.media.MediaPlayer;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by admin on 7/24/2017.
 */

public class CollectorMediaPlayer extends MediaPlayer {
    private String fileName;

    public CollectorMediaPlayer(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        super.setDataSource(fd);
    }

    public String getFileName() {
        return fileName;
    }
}
