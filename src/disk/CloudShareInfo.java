package disk;
/*
 * CloudShareInfo
 * 
 * CloudShare 정보 관리 클래스
 * 	: 마운트 위치와 해당 디렉토리의 파일 리스트 등의 정보를 다룬다.
 */

import java.io.File;
import java.util.LinkedList;

import debug.Debug;
import operation.OperationManager;

public class CloudShareInfo {
	@SuppressWarnings("unused")
	private final String TAG = "CloudShareInfo";
	private static CloudShareInfo instance = null;
	private String mntPoint;
	private LinkedList<String> fileList;
	
	@SuppressWarnings("unused")
	
	// 실제 cs 메타데이터 파일이 저장되는 곳
	private String directory_name = ".CloudShare";
	
	public static CloudShareInfo getInstance() {
		if (instance == null) {
			instance = new CloudShareInfo();
		}
		return instance;
	}
	
	private CloudShareInfo() {
		this.mntPoint = System.getenv("HOME") + File.separator + directory_name;
		this.fileList = new LinkedList<>();
		this.refreshFileList();
	}

	/*
	 * refreshFileList
	 * 파일 리스트 갱신
	 */
	private void refreshFileList() {
//		Debug.print(TAG, "refreshFileList", "mount point is " + this.mntPoint);
		fileList.clear();
		File folder = new File(this.mntPoint);
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            // Skip
	        } else {
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

	public String getMntPoint() {
		return mntPoint;
	}

	public void setMntPoint(String mntPoint) {
		this.mntPoint = mntPoint;
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
	
	
}
