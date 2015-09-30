package fm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * FileSender로부터의 파일 전송 요청 처리 
 */
public class FileListener extends Thread {
	private final int port_num = 7798;
	ServerSocket s;
	
	@Override
	public void run() {
		try {
			s = new ServerSocket(port_num);
			
			while (true) {
				// 새로운 파일전송 요청이 들어온 경우
				Socket ns = s.accept();
				
				// 파일 전송 수신자를 생성하여 처리 
				FileReceiver receiver = new FileReceiver(ns);
				receiver.start();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
