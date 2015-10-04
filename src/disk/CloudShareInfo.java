package disk;
/*
 * CloudShareInfo
 * 
 * CloudShare 정보 관리 클래스
 * 	: 마운트 위치와 해당 디렉토리의 파일 리스트 등의 정보를 다룬다.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import debug.Debug;
import debug.MyConstants;

public class CloudShareInfo {
	private final String TAG = "CloudShareInfo";
	private static CloudShareInfo instance = null;
	private String cache_dir;		// ᅟcache directory
	private String cloud_mntPoint;	// mount directory
	private LinkedList<String> fileList;
	// 클라우드 상에서 사용할 공간
	// 
	private String downloadPoint;
		
	// 실제 cs 메타데이터 파일이 저장되는 곳
	private String directory_name = ".CloudShare" + File.separator;
	
	public static CloudShareInfo getInstance() {
		if (instance == null) {
			instance = new CloudShareInfo();
		}
		return instance;
	}
	
	private CloudShareInfo() {
		this.cache_dir = System.getenv("HOME") + File.separator + directory_name;
		this.fileList = new LinkedList<>();
		this.refreshFileList();
		
		// NdriveFUSE가 마운트된 곳을 저장한다.
		// 만약 마운트된 곳이 없다면, 프로그램을 종료한다.
		try {
			getCloudMountPoint();
			this.setDownloadPoint(cloud_mntPoint + File.separator + "CloudShare" + File.separator);
			// 만약 해당 포인트가 없다면 디렉토리를 새로 만든다.
			File dp = new File(getDownloadPoint());
			dp.mkdirs();
		} catch (Exception e) {
			Debug.error(TAG, "CloudShareInfo", "Failed to get Ndrive mount point.");
			System.exit(MyConstants.NO_NDRIVE_MNTPOINT);
		}
		
		Debug.print(TAG, "CloudShareInfo", "NDrive is mounted at " + getCloud_mntPoint());
	}

	private void getCloudMountPoint() throws IOException {
		Runtime rt = Runtime.getRuntime();
		String[] cmd = {
				"/bin/bash",
				"-c",
				"mount -l | grep NDriveFUSE | awk '{print $3}'"
				};

		Process proc = rt.exec(cmd);

		BufferedReader stdInput = new BufferedReader(new 
		     InputStreamReader(proc.getInputStream()));

		// read the output from the command
		String s = null;
		while ((s = stdInput.readLine()) != null) {
		    this.setCloud_mntPoint(s);
		}
		// 마지막으로, 받은 값 검사
		if (this.cloud_mntPoint == null) {
			Debug.error(TAG, "getCloudMountPoint", "Failed to get Ndrive mount point.");
			System.exit(MyConstants.NO_NDRIVE_MNTPOINT);
		}
	}

	/*
	 * refreshFileList
	 * 파일 리스트 갱신
	 */
	private void refreshFileList() {
//		Debug.print(TAG, "refreshFileList", "mount point is " + this.mntPoint);
		fileList.clear();
		File folder = new File(this.cache_dir);
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            // Skip
	        } else {
//	        	Debug.print(TAG, "refreshFileList", "add file name " + fileEntry.getName());
	            fileList.add(fileEntry.getName().replaceAll("\\s",""));
	        }
	    }
	}

	public LinkedList<String> getFileListLst() {
		refreshFileList();
		return fileList;
	}
	
	public String getFileListStr() {
		refreshFileList();
		Debug.print(TAG, "getFileListStr", "CloudShare file list = " + fileList.toString());
		return fileList.toString();
	}

	public void setFileList(LinkedList<String> fileList) {
		this.fileList = fileList;
	}

	public String getCacheDirectory() {
		return cache_dir;
	}

	public void setMntPoint(String mntPoint) {
		this.cache_dir = mntPoint;
	}

	/*
	 * checkFileExist
	 * 현재 해당 파일을 갖고 있는지 확인한다.
	 * true: Exist, false: non-exist
	 */
	public boolean checkFileExist(String filename) {
		refreshFileList();
		boolean r = false;
		for (int i = 0; i < fileList.size(); i++) {
			if (fileList.get(i).compareTo(filename) == 0) {
				r = true;
				break;
			}
		}
		return r;
	}

	public String getCloud_mntPoint() {
		return cloud_mntPoint;
	}

	public void setCloud_mntPoint(String cloud_mntPoint) {
		this.cloud_mntPoint = cloud_mntPoint;
	}

	public String getDownloadPoint() {
		return downloadPoint;
	}

	public void setDownloadPoint(String downloadPoint) {
		this.downloadPoint = downloadPoint;
	}
	
	
}
