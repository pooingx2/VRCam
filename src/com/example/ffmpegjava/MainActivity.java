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
	private FfmpegManager ffmpegMgr;

	private String leftFilePath;
	private String rightFilePath;	
	private String resultFilePath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = getApplicationContext();

		ffmpegMgr = new FfmpegManager();
		// resource의 raw에 들어있는 binaryfile을 internal storage에 copy
		ffmpegMgr.installBinaryFile(context);

		// result 파일을 저장할 디렉토리 설정
		File directory = new File(Environment.getExternalStorageDirectory()+File.separator+vrDir);
		directory.mkdirs();

		leftFilePath = null;
		rightFilePath = null;
		resultFilePath = null;

		getVideoFromGallery();
	}

	// Gallery에서 비디오 파일을 가져옴
	public void getVideoFromGallery() {
		Toast.makeText(context, "Select video file", Toast.LENGTH_LONG).show();
		Intent intent = new Intent(Intent.ACTION_PICK, 
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("video/*");
		startActivityForResult(intent, 1);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);
		// 파일을 선택되지 않은 경우
		if(intent == null) {
			getVideoFromGallery();
		}
		// 파일을 선택한 경우
		else {
			switch(requestCode){
			case 1:
				if(resultCode == RESULT_OK){
					Uri uri = intent.getData();

					if (uri.getScheme().toString().compareTo("content")==0)	{      
						Cursor cursor =getContentResolver().query(uri, null, null, null, null);
						if (cursor.moveToFirst()) {
							int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
							Uri filePathUri = Uri.parse(cursor.getString(column_index));

							// 결과 파일을 저장하기 위한 시간을 가져옴
							long curTime = System.currentTimeMillis(); 

							// 처음 선택한 파일이 왼쪽파일
							if(leftFilePath == null){
								// 선택한 비디오 파일의 URI로 파일의 경로를 가져옴
								leftFilePath = filePathUri.getPath();
								
								// 왼쪽파일을 선택했으면 오른쪽 파일을 선택
								getVideoFromGallery();
							}
							// 두번째로 선택한 파일이 오른쪽 파일
							else {
								// 선택한 비디오 파일의 URI로 파일의 경로를 가져옴
								rightFilePath = filePathUri.getPath();
								// 결과파일의 저장경로를 설정 (외부저장소 경로/VRCam/시간.mp4
								resultFilePath = Environment.getExternalStorageDirectory() 
										+File.separator + vrDir + File.separator + curTime + ".mp4";

								Toast.makeText(context, "Left : " + leftFilePath + 
										"\nRight : " + rightFilePath, Toast.LENGTH_LONG).show();
								Log.d(TAG1, leftFilePath);
								Log.d(TAG1, rightFilePath);
								Log.d(TAG1, resultFilePath);

								// 2개 영상을 합치고 결과를 resultFilePath에 저장
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
		// 실행할 ffmpeg 명령어
		cmd = ffmpegMgr.getBinaryPath()
				+ " -i "
				+ leftFilePath
				+ " -i "
				+ rightFilePath
				+ " -filter_complex "
				+ "[0:v]pad=iw*2:ih[int];[int][1:v]overlay=W/2:0[vid] "
				+ "-map [vid] -map 0:a -c:v libx264 -crf 23 -preset ultrafast -c:a copy "
				+ resultFilePath; 


		try {
			// 명령어를 수행 ShellCallback은 명령어 수행시 shell의 callback 메시지를 받기 위함
			ffmpegMgr.execProcess(cmd, new ShellCallback() {
				TextView textView1 = (TextView)findViewById(R.id.textView1);
				TextView textView2 = (TextView)findViewById(R.id.textView2);
				ProgressManager progressMgr = new ProgressManager();
				String str;
				
				@Override
				public void shellOut(String shellLine) {
					str = shellLine;
					
					//Duration: 00:00:05.65, start: 0.000000, bitrate: 1401 kb/s
					//00:00:05.65 정보를 얻어 정수형태로 시간 계산 (영상의 총 길이)
					if(str.contains("Duration")) {
						Log.d("PROGRESS", str);
						progressMgr.setDurationFromShell(str);
					}
					//frame= 12 fps=0.0 q=24.0 size= 90kB time=00:00:00.50 bitrate=1467.7kbits/s
					//00:00:00.50 정보를 얻어 정수형태로 시간 계산 (현재까지 진행된 결과 영상 길이)
					if(str.contains("frame") && str.contains("time")) {
						Log.d("PROGRESS", str);
						progressMgr.setTimeFromShell(str);
					}
					
					new Thread(new Runnable() {
						@Override
						public void run(){
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									textView1.setText("Shell > " + str + "\n");
									textView2.setText("Progress : " + progressMgr.getProgress() + "%\n");
								}
							});
						}
					}).start();
					
					Log.d(TAG2, shellLine);
				}

				// 결과 영상 완료시 Callback 메소드
				@Override
				public void processComplete(int exitValue) {
					if(exitValue == 0) {
						Log.d(TAG2, "Completion");
						new Thread(new Runnable() {
							@Override
							public void run(){
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(context, "Completion", Toast.LENGTH_LONG).show();
										progressMgr.setComplete();
										textView2.setText("Progress : " + progressMgr.getProgress() + "%\n");
									}
								});
							}
						}).start();
						updateGallery();
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
