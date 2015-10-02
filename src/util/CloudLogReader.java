package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import debug.Debug;

public class CloudLogReader {
	private final String TAG = "CloudLogReader";
	private static CloudLogReader instance = null;
	String logPath;
	
	private static CloudLogReader getInstance() {
		if (instance == null) {
			instance = new CloudLogReader();
		}
		return instance;
	}
	
	private CloudLogReader() {
		// logPath 초기화
		logPath = System.getenv("HOME") + File.separator + ".cslog";
	}
	
	/*
	 * Log파일로부터 fname에 해당하는 공유링크를 읽어 링크를 반환한다.
	 * null일 경우에는 없는 것 
	 * @param fname: 파일 이름(분할된 파일 이름 .x 이어야한다.)
	 */
	public static String getLink(String fname) {
		return getInstance()._getLink(fname);
	}

	private String _getLink(String fname) {
		Debug.print(TAG, "_getLink", "Try to get share link for "+fname);
		Debug.print(TAG, "_getLink", "Log file is placed at " + logPath);
		File logFile = new File(logPath);
		try {
			FileReader fr = new FileReader(logFile);
			BufferedReader br = new BufferedReader(fr);
			String s;
			while ( (s = br.readLine()) != null ) {
				Debug.print(TAG, "_getLink", "Read a line from logfile: " + s);
				String[] v = s.split(": ");
				System.out.println("v[0] = " + v[0] + ", v[1] = " + v[1]);
				File p = new File(v[0]);
				if (p.getName().compareTo(fname) == 0) {
					br.close();
					return v[1];
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return null;
	}

}
