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
import android.widget.Toast;

public class MainActivity extends Activity {

	private final static String TAG1 = "MAINACTIVITY";
	private final static String TAG2 = "SHELL";
	private final static String vrDir = "VRCam";

	private Context context;
	private FfmpegManager fm;

	private String leftFilePath;
	private String rightFilePath;	
	private String resultFilePath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();

		fm = new FfmpegManager();
		fm.installBinaryFile(context);

		File directory = new File(Environment.getExternalStorageDirectory()+File.separator+vrDir);
		directory.mkdirs();

		leftFilePath = null;
		rightFilePath = null;
		resultFilePath = null;

		getVideoFromGallery();
	}

	public void getVideoFromGallery() {
		Toast.makeText(context, "Select video file", Toast.LENGTH_LONG).show();
		Intent intent = new Intent(Intent.ACTION_PICK, 
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("video/*");
//		intent.setType("image/*");
		startActivityForResult(intent, 1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);
		if(intent == null) {
			getVideoFromGallery();
		}
		else {
			switch(requestCode){
			case 1:
				if(resultCode == RESULT_OK){
					Uri uri = intent.getData();

					if (uri.getScheme().toString().compareTo("content")==0)	{      
						Cursor cursor =getContentResolver().query(uri, null, null, null, null);
						if (cursor.moveToFirst()) {
							int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
							Uri filePathUri = Uri.parse(cursor.getString(column_index));

							long curTime = System.currentTimeMillis(); 

							if(leftFilePath == null){
								leftFilePath = filePathUri.getPath();
								getVideoFromGallery();
							}
							else { 
								rightFilePath = filePathUri.getPath();	
								resultFilePath = Environment.getExternalStorageDirectory() 
										+File.separator + vrDir + File.separator + curTime + ".mp4";
//								resultFilePath = Environment.getExternalStorageDirectory() 
//										+File.separator + vrDir + File.separator + curTime + ".jpg";


								Toast.makeText(context, "Left : " + leftFilePath + 
										"\nRight : " + rightFilePath, Toast.LENGTH_LONG).show();
								Log.d(TAG1, leftFilePath);
								Log.d(TAG1, rightFilePath);
								Log.d(TAG1, resultFilePath);

								joinVideo(leftFilePath, rightFilePath, resultFilePath);

							}
						}
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
		
		//ffmpeg -i a.jpg -i b.jpg -filter_complex scale=120:-1, tile=2x1 output.jpg
		
//		cmd = fm.getBinaryPath()
//				+ " -i "
//				+ leftFilePath
//				+ " -i "
//				+ rightFilePath
//				+ " -filter_complex "
//				+ "[0]scale=1920:1080[sc0];[1]scale=1920:1080[sc1];[sc0]pad=iw*2:ih[pad];[pad][sc1]overlay=w "
//				+ resultFilePath; 


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
					if(exitValue == 0) {
						Log.d(TAG2, "Completion");
						updateGallery();
//						Toast.makeText(context, "Completion", Toast.LENGTH_LONG).show();
					}
					else {
						Log.d(TAG2, "Fail");
					}
				}

			});
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void updateGallery() {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

		File f = new File(resultFilePath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}
}
