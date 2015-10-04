package fm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import debug.Debug;
import debug.MyConstants;
import disk.CloudShareInfo;
import util.CSFileRecorder;
import util.CloudLogReader;

/*
 * 파일의 일부분만을 쓰기로 저장하는 클래스이다.
 */
public class FilePartSaver extends Thread {
	private final String TAG = "FilePartSaver";
	String file_path;
	int offset;
	int num;
	public FilePartSaver(String path, long offset, int part_num) {
		this.file_path = path;
		this.offset = (int)offset;
		this.num = part_num;
	}

	@Override
	public void run() {
		File f = new File(file_path);
		try {
			FileInputStream 	fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis);
			File fp = new File(CloudShareInfo.getInstance().getDownloadPoint() + f.getName() + "." + num);
			FileOutputStream	fos = new FileOutputStream(fp);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			
			int bytesRead;
			byte[] buffer = new byte[MyConstants.FILE_BUFFER_SIZE];
			int try_size = buffer.length;	// 읽을 바이트 크기
			if (try_size + offset > f.length()) {
				try_size = (int)(f.length() - offset);
			}

			// read위치 변경
			bis.skip(offset);

			while ( (bytesRead = bis.read(buffer, 0, try_size)) > 0 ) {
				bos.write(buffer, 0, bytesRead);
				
				offset += bytesRead;
				if (try_size + offset > f.length()) {
					try_size = (int)(f.length() - offset);
					if (try_size > MyConstants.FILE_BUFFER_SIZE) {
						try_size = MyConstants.FILE_BUFFER_SIZE;
					}
				}
			}
			bos.flush();
			
			// 카피한 후에는 원래 복사했던 완전파일을 삭제한다.
			f.delete();
			bis.close();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// 나머지 부분을 클라우드에 저장하고 나면, 메타파일을 만들어서 저장한다.
        CSFileRecorder.createFile(f.getName(), num);
        String slink = CloudLogReader.getLink(f.getName() + "." + num);
        Debug.print(TAG, "run", "slink is " + slink);
        CSFileRecorder.addLink(f.getName(), slink, num);
	}
}
