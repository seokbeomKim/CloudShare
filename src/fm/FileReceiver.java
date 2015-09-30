package fm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.jndi.toolkit.url.Uri;
import com.sun.org.apache.xerces.internal.util.URI;

import debug.Debug;
import disk.CloudShareInfo;

/*
 * 파일 송수신에 쓰이는 클래스
 */
public class FileReceiver extends Thread {
	private final String TAG = "FileReceiver";
	private final String TOKEN = "::__::";
	private final int BUFFER_SIZE = 8192;
	private final int META_SIZE	= 512;

	Socket s;
	public FileReceiver(Socket ns) {
		s = ns;
	}
	
    BufferedInputStream		bis = null;
    BufferedOutputStream	bos = null;

	@Override
	public void run() {
		int bytesRead;
	    int current = 0;
	    
	    try {			
			bis = new BufferedInputStream(s.getInputStream());
			
			/*
			 * 실질적인 전송 부분 
			 */
			// 메타 데이터(파일이름, 파일크기)부터 받는다.
			byte[] metadata = new byte[META_SIZE];
			bis.read(metadata, 0, META_SIZE);
			String metaStr =  new String(metadata, "UTF-8");
			Debug.print(TAG, "run", "Received meta data: " + metaStr);
			
			String[] v = metaStr.split("\'");
			int fsize = Integer.parseInt(v[0]);
			String fname = v[1];	
			current = 0;

			// 파일 생성 및 파일 출력 위한 스트림 준비
			// JAVA의 파일 이름관련 보안문제로 확장자 (.*)를 사용 불가 - NUL character
			String fp = CloudShareInfo.getInstance().getMntPoint() + File.separator + fname;
			fp = fp.replace("\0", "");

			Path path = Paths.get(fp);


			File file = new File(path.toUri());

			Debug.print(TAG, "run", "new file will be " + fp);
			Debug.print(TAG, "run", file.getAbsolutePath());
			
			FileOutputStream fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			
		    byte[] buffer = new byte[fsize];
			buffer  = new byte [fsize];
			
			Debug.print(TAG, "run", "file name = " + fname + ", fsize = " + fsize + ", fp = " + fp + 
					", buffer size = " + buffer.length);
			
			Debug.print(TAG, "run", "Start to receive bytes of file");
			do {
		    	bytesRead = bis.read(buffer, current, fsize - current);
		    	System.out.println("bytesRead : " + bytesRead);
		    	bos.write(buffer, current, bytesRead);
		    	if(bytesRead >= 0) current += bytesRead;
		    	System.out.println("*****"); 
		    } while(bytesRead > 0);

			bos.flush();
			bos.close();
			fos.close();
			bis.close();
			
			// 완료 후 이름 바꾼다.
//			Process p = Runtime.getRuntime().exec("mv " + fp + " " + dest_fp);
//		    p.waitFor();
	    }
	    catch (Exception e) {
	    	Debug.error(TAG, "run", "Failed to receive file");
	    	e.printStackTrace();
	    }
	}
}
