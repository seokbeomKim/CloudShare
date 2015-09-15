package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import debug.Debug;

/*
 * SocketListener
 * 
 * 외부 클라이언트와의 소켓 통신 연결을 위한 쓰레드 관리 클래스
 * 최초의 소켓 연결을 위해 존재한다.
 */
public class SocketListener extends Thread {
	private final String TAG = "SocketListener";
	private final int portnum_es = 7799;	
	ServerSocket s;

	@Override
	public void run() {
		try {
			if (s == null || s.isClosed()) {
				s = new ServerSocket(portnum_es);
			}
			Socket socket = s.accept();
			Debug.print(TAG, "run", "Succeed to get new socket instance");
			ExternalService.getInstance().getClientSocket().add(socket);
		} catch (IOException e) {
			e.printStackTrace(); 
			Debug.print(TAG, "run", "Failed to get socket instance - External Service.");
		}
		
	}
}
