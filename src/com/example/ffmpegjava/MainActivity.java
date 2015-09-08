package com.example.ffmpegjava;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	private final static String TAG1 = "MAINACTIVITY";
	private final static String TAG2 = "SHELL";

	private Context context;
	private FfmpegManager fm;
	private final static String vrDir = "VRCam";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();

		fm = new FfmpegManager();
		fm.installBinaryFile(context);

		File directory = new File(Environment.getExternalStorageDirectory()+File.separator+vrDir);
		directory.mkdirs();

		Intent intent = new Intent(Intent.ACTION_PICK, 
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("video/*");
		startActivityForResult(intent, 1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);
		switch(requestCode){
		case 1:
			if(resultCode == RESULT_OK){
				Uri uri = intent.getData();

				if (uri.getScheme().toString().compareTo("content")==0)	{      
					Cursor cursor =getContentResolver().query(uri, null, null, null, null);
					if (cursor.moveToFirst()) {
						int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
						Uri filePathUri = Uri.parse(cursor.getString(column_index));

						String leftFilePath = filePathUri.getPath();
						String rightFilePath = leftFilePath;	
						long curTime = System.currentTimeMillis(); 
						String resultFilePath = Environment.getExternalStorageDirectory() 
								+File.separator + vrDir + File.separator + curTime + ".mp4";

						Log.d(TAG1, leftFilePath);
						Log.d(TAG1, rightFilePath);
						Log.d(TAG1, resultFilePath);

						joinVideo(leftFilePath, rightFilePath, resultFilePath);

						Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						
						File f = new File(resultFilePath);
						Uri contentUri = Uri.fromFile(f);
						mediaScanIntent.setData(contentUri);
						this.sendBroadcast(mediaScanIntent);
					}
				}
			}
		}
	}

	public void joinVideo(String leftFilePath, String rightFilePath, String resultFilePath) {
		String cmd;
		cmd = fm.getBinaryPath()
				+ " -i "
				+ leftFilePath
				+ " -i "
				+ rightFilePath
				+ " -filter_complex "
				+ "[0:v]pad=iw*2:ih[int];[int][1:v]overlay=W/2:0[vid] "
				+ "-map [vid] -map 0:a -c:v libx264 -crf 23 -preset ultrafast -c:a copy "
				+ resultFilePath; 


		try {
			fm.execProcess(cmd, new ShellCallback() {

				TextView textView = (TextView)findViewById(R.id.textView);
				String str;

				@Override
				public void shellOut(String shellLine) {
					str = shellLine;
					new Thread(new Runnable() {
						@Override
						public void run(){
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									textView.setText("Shell > " + str + "\n");
								}
							});
						}
					}).start();

					Log.d(TAG2, shellLine);
				}

				@Override
				public void processComplete(int exitValue) {
					if(exitValue == 0)
						Log.d(TAG2, "Complation Success");
					else
						Log.d(TAG2, "Complation Fail");
				}

			});
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
