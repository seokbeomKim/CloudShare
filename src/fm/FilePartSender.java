package fm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import debug.Debug;

/*
 * 메세지의 타겟으로 파일을 보낸다.
 * 송신자는 클라이언트, 수신자는 msg.getFrom()
 * 파일 리스트는 msg.getValue()
 */
public class FilePartSender extends Thread {
	private final String TAG = "FileSender";
	private final int port_num = 7798;
	
	private long offset;
	private long len;
	private int num;	// 파일 분할 번호 
	
	private final int BUFFER_SIZE = 8192;
	private final int META_SIZE	= 512;

	String target;
	String filename;

	public FilePartSender(String to, String filename, long offset, long len, int num) {
		this.target = to;
		this.filename = filename;
		this.offset = offset;
		this.len = len;
		this.num = num;
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
			
			String fp = filename;	// file path
			String fnameWithNum = fp + "." + num;
			File tFile = new File (fnameWithNum);
			File myFile = new File (fp);
				
			byte [] mybytearray  = new byte [BUFFER_SIZE];
				
			fis = new FileInputStream(myFile);
	        bis = new BufferedInputStream(fis);
	        System.out.println("Sending " + fp + "(" + len + " bytes)");
	        System.out.println("File info: name = " + myFile.getName() + ", size = " + len);

	        // 처음 파일 이름과 파일 크기를 알려준다.
	        String metaStr = (int)len + "\'" + tFile.getName() + "\'" + "PartDw";
	        System.out.println("metadata = " + metaStr);
	        byte[] metaData = new byte[META_SIZE];
	        metaData = Arrays.copyOf(metaStr.getBytes(Charset.forName("UTF-8")), META_SIZE);
	        
	        bos.write(metaData, 0, META_SIZE);
	        bos.flush();

	        // 그 후에 파일 전송
	        int total_read = 0;
	        int try_size = BUFFER_SIZE;
	        
	        if (BUFFER_SIZE > len) {
	        	try_size = (int)(len);
	        }

	        bis.skip(offset);
	        
	        offset = 0;
	        Debug.print(TAG, "run", "offset = " + offset + ", len = " + len + ", try_size = " + try_size);
	        while ((bytesRead = bis.read(mybytearray, 0, try_size)) > 0) {
                bos.write(mybytearray, 0, bytesRead);
                total_read += bytesRead;
                offset += bytesRead;
                if (total_read == (int)len) {
                	// 목표치만큼 읽었으면 끝낸다
                	break;
                }
                if (offset + BUFFER_SIZE > len) {
    	        	try_size = (int)(len - offset);
    		        Debug.print(TAG, "run", "offset = " + offset + ", len = " + len + ", try_size = " + try_size);
    	        }
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
