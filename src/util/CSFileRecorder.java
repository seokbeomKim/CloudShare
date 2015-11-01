package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import debug.Debug;
import disk.CloudShareInfo;

/*
 * cs파일 생성 및 관리하는 클래스이다.
 */
public class CSFileRecorder {
	private final String TAG = "CSFileRecorder";
	private static CSFileRecorder instance = null;
	public static CSFileRecorder getInstance() {
		if (instance == null) {
			instance = new CSFileRecorder();
		}
		return instance;
	}
		
	public static void createFile(String name, int num) {
		try {
			getInstance()._createFile(name, num);
		} catch (IOException e) {
			Debug.error("CSFileRecorder", "createFile", "Failed to create meta file ("+name+")");
			e.printStackTrace();
		}
	}

	/*
	 * _createFile
	 * 메타 파일을 생성한다. 위치는 .CloudShare
	 * @param fname: 메타파일 이름 
	 */
	private void _createFile(String fname, int part_num) throws IOException {		
		String metaPath = CloudShareInfo.getInstance().getCacheDirectory() + fname + ".cs";
		Debug.print(TAG, "createFile", "Create meta file (" + fname + ") at " + metaPath);

		File metaFile = new File(metaPath);
		if (metaFile.exists()) {
			// 만약 존재하면 지우고 다시 생성
			metaFile.delete();
		}
		// 메타 파일 생성
		metaFile.createNewFile();
	}

	public static void addLink(String fname, String slink, int i) {
		getInstance()._addLink(fname, slink, i);
	}

	/*
	 * _addLink
	 * 공유 링크를 메타파일에 추가한다.
	 * @param fname: 메타파일 이름
	 * @param slink: 공유링크 주소
	 * @param i: 부분 번호 (1부터 시작)
	 */
	private void _addLink(String fname, String slink, int i) {
		String metaPath = CloudShareInfo.getInstance().getCacheDirectory() + fname + ".cs";
		
		File metaFile = new File(metaPath);
		File tempFile = new File(metaPath + ".tmp");	// temp파일 
		// 메타파일이 있는지 확인한다.
		if (!metaFile.exists()) {
			// 없는 경우에는 에러 발생
			Debug.error(TAG, "_addLink", "There is no meta file ("+metaFile.getAbsolutePath() + ")");
			return;
		}
		if (slink == null) {
			// 추가하고자 하는 공유링크가 null값인 경우 에러
			Debug.error(TAG, "_addLink", "Link is broken. Failed to add new link");
			return;
		}
		Debug.print(TAG, "_addLink", "meta file = " + fname + ", new link = " + slink + " at " + i);
		// 정상적으로 값이 들어온 경우에는 추가한다.
		// 부분 파일명 (예. TESTFILE.1)
		String fp_name = fname + "." + i;
		BufferedReader br;
		PrintWriter writer;
		
		// 현재 metaFile에 해당 링크가 있는지 검사하고 있으면 없앤다. 
		try {
			tempFile.createNewFile();

			FileReader fr = new FileReader(metaFile);
			br = new BufferedReader(fr);
			writer = new PrintWriter(tempFile);
			String s;
			while ( (s = br.readLine()) != null ) {
				// 사용되는 구분자는 : 
				String[] v = s.split(": ");
				if (v[0].compareTo(fp_name) == 0) {
					// 같은 경우에는 해당 라인을 제외한다.
				}
				else {
					writer.println(s);
				}
			}
			
			// 해당 파일을 거르고 난 뒤에 새롭게 링크를 추가한다.
			String d = fp_name + ": " + slink;
			System.out.println("d = "+d);
			writer.println(d);
			writer.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		// temp파일을 완성하였으면 이를 원본으로 대체한다.
		tempFile.renameTo(metaFile);
	}

	/*
	 * 부분파일 이름으로부터 메타파일의 이름을 알아낸다.
	 */
	public static String getMetaFileNameFromPartFileName(String pf_name) {
		return pf_name.substring(0, pf_name.length() - 2);
	}

	public static boolean checkCompletedMetaFile(String metaFile) {
		return getInstance()._checkCompleteMetaFile(metaFile);
	}

	private boolean _checkCompleteMetaFile(String metaFileName) {
		// 가장 밑의 줄의 내용이 분할 파일의 마지막을 나타내는 링크이므로 이를 이용하여
		// 링크를 완전하게 갖고 있는지 확인한다.
		String mfPath = CloudShareInfo.getInstance().getCacheDirectory() + metaFileName;
		Debug.print(TAG, "_checkCompleteMetaFile", "meta file path is " + mfPath);
		File mf = new File(mfPath);
		try {
			BufferedReader r = new BufferedReader(new FileReader(mf));
			String s;
			LinkedList<String> values = new LinkedList<String>();
			
			while ( (s = r.readLine()) != null ) {
				values.add(s);
			}
			
			// 다 넣고 난 뒤에 검사
			String temp = values.getFirst();
			Debug.error(TAG, "_checkCompleteMetaFile", "last line is " + temp);
			String[] v = temp.split(": ");
			String n = v[0].trim().toString();
			int max = Integer.parseInt(n.substring(n.length() - 1));
			boolean[] checkList = new boolean[max + 1];
			
			for (int i = 0; i < values.size(); i++) {
				// 루프 돌면서 검사
				temp = values.get(i);
				v = temp.split(": ");
				n = v[0].trim().toString();
				int idx = Integer.parseInt(n.substring(n.length() - 1));
				checkList[idx] = true;
			}
			
			for (int i = 0; i < checkList.length; i++) {
				if (checkList[i] == false) {
					r.close();
					return false;
				}
			}
			
			r.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return false;
	}
}
