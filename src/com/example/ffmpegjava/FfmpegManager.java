package com.example.ffmpegjava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FfmpegManager {


	private final static String TAG = "FFMPEG";
	private String ffmpegBin;

	public FfmpegManager() {

	}

	public File getAlbumStorageDir(String albumName) {
		File file = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), albumName);
		if (!file.mkdirs()) {
			Log.e(TAG, "Directory not created");
		}
		return file;
	}

	public void installBinaryFile(Context context) {
		ffmpegBin = installBinary(context, R.raw.ffmpeg, "ffmpeg");
	}

	public String getBinaryPath () {
		return ffmpegBin;
	}

	public String installBinary(Context ctx, int resId, String filename) {
		
		Log.d(TAG,"Install Binalry file");
		
		try {
			File f = new File(ctx.getDir("bin", 0), filename);
			if (f.exists()) {
				f.delete();
			}
			copyRawFile(ctx, resId, f, "0755");
			return f.getCanonicalPath();
		} catch (Exception e) {
			Log.e(TAG, "Install Binary failed: " + e.getLocalizedMessage());
			return null;
		}
	}

	public void copyRawFile(Context ctx, int resid, File file, String mode) throws IOException, InterruptedException {
		
		Log.d(TAG,"Copy Binalry file");
		
		final String abspath = file.getAbsolutePath();
		final FileOutputStream out = new FileOutputStream(file);
		final InputStream is = ctx.getResources().openRawResource(resid);
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		Runtime.getRuntime().exec("chmod " + mode + " " + abspath).waitFor();
	}

	public int execProcess(String cmd, ShellCallback sc) throws IOException, InterruptedException {		

		int exitVal = 0;
		
		cmd = String.format(Locale.US, "%s", cmd);
		sc.shellOut(cmd);

		try{
			Process p = Runtime.getRuntime().exec(cmd);

			StreamThread error = new StreamThread( p.getErrorStream(), "ERROR", sc);
			StreamThread output = new StreamThread(p.getInputStream(), "OUTPUT", sc);

			error.start();
			output.start();

			//exitVal = p.waitFor();
			//sc.processComplete(exitVal);

		}catch(Exception e){
			System.out.println(e);
		}

		return exitVal;

	}


	private class StreamThread extends Thread {
		InputStream is;
		String type;
		ShellCallback sc;

		StreamThread(InputStream is, String type, ShellCallback sc) {
			this.is = is;
			this.type = type;
			this.sc = sc;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null)
					if (sc != null)
						sc.shellOut(line);

			} catch (IOException ioe){
				ioe.printStackTrace();
			}
		}
	}
}
