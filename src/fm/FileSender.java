package fm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;

import debug.Debug;
import disk.CloudShareInfo;
import message.Message;
import util.MyConverter;

/*
 * 메세지의 타겟으로 파일을 보낸다.
 * 송신자는 클라이언트, 수신자는 msg.getFrom()
 * 파일 리스트는 msg.getValue()
 */
public class FileSender extends Thread {
	private final String TAG = "FileSender";
	private final int port_num = 7798;
	private final String TOKEN = "::__::";
	
	private final int connect_try = 10;	// 최대 연결 시도 횟수 
	private final long retry_time = 1000;	// 재시도 하는데 걸리는 시간
	
	private final int BUFFER_SIZE = 8192;
	private final int META_SIZE	= 512;

	String target;
	String filename;

	public FileSender(String to, String filename) {
		this.target = to;
		this.filename = filename;
	}

	// 파일 전송
	public void run() {
		// 소ᅟ켓 및 파일 핸들링을 위한 스트림 
		FileInputStream			fis = null;
	    BufferedInputStream 	bis = null;
	    BufferedOutputStream	bos = null;
	    
		// 소켓 할당 및 스트림 초기화
		Socket s = null;
		int bytesRead;
		
		try {
			s = new Socket(target, port_num);
			// 소켓 관련 스트림 초기화
			bos = new BufferedOutputStream(s.getOutputStream());
			
			String fp = CloudShareInfo.getInstance().getMntPoint() + File.separator + filename;	// file path
			File myFile = new File (fp);
				
			byte [] mybytearray  = new byte [BUFFER_SIZE];
				
			fis = new FileInputStream(myFile);
	        bis = new BufferedInputStream(fis);
	        System.out.println("Sending " + fp + "(" + myFile.length() + " bytes)");
	        System.out.println("File info: name = " + myFile.getName() + ", size = " + myFile.length());

	        // 처음 파일 이름과 파일 크기를 알려준다.
	        String metaStr = (int)myFile.length() + "\'" + filename;
	        System.out.println("metadata = " + metaStr);
	        byte[] metaData = new byte[META_SIZE];
	        metaData = Arrays.copyOf(metaStr.getBytes(Charset.forName("UTF-8")), META_SIZE);
	        
	        bos.write(metaData, 0, META_SIZE);
	        bos.flush();

	        // 그 후에 파일 전송
	        while ((bytesRead = bis.read(mybytearray)) > 0) {
                bos.write(mybytearray, 0, bytesRead);
            }
            bos.flush();

	        // 마지막으로 파일 전송 완료를 알린다.
	        System.out.println("Done.");
	        
	        fis.close();
	        bis.close();
	        bos.close();
			s.close();	
		} catch (Exception e) {
			Debug.error(TAG, "run", "Failed to get socket for file transfer.");
			e.printStackTrace();
			return;
		}
	}	
}
