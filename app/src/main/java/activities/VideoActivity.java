package activities;

import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.io.File;

import com.gcs.riyadh.R;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_video_attachment);


        RelativeLayout rlVideoActivity = (RelativeLayout) findViewById(R.id.rlVideoActivity);
        RelativeLayout rlVideo = (RelativeLayout) findViewById(R.id.rlVideo);
        final VideoView videoView = (VideoView) findViewById(R.id.videoView);
        Uri contentUri = FileProvider.getUriForFile(this, getString(R.string.app_package_name),  (File)getIntent().getExtras().getSerializable("file"));
        videoView.setVideoURI(contentUri);
        videoView.setZOrderOnTop(true);;
        videoView.setMediaController(new MediaController(this));
        videoView.start();


        rlVideoActivity.setOnClickListener(this);
        videoView.setOnClickListener(this);
        rlVideo.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.rlVideoActivity){
            finish();
        }
    }
}
