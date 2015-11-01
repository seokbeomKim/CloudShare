package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import debug.Debug;
import disk.DiskInfo;

/*
 * SocketListener
 * 
 * 외부 클라이언트와의 소켓 통신 연결을 위한 쓰레드 관리 클래스
 * 클라이언트간 최초의 소켓 연결을 위해 존재한다.
 */
public class SocketListener extends Thread {
	private final String TAG = "SocketListener";
	private final int portnum_es = 7799;	
	private ServerSocket s;
	
	@Override
	public void run() {
		// 서버소켓을 초기화 부분
		if (s == null) {
			try {
				s = new ServerSocket(portnum_es);
			} catch (IOException e) {
				System.err.println("Failed to allocate ServerSocket.");
				e.getStackTrace();
			}
		}
		else {
			if (s.isClosed()) {
				System.out.println("The server socket is closed now.");
			}
		}
		
		// ==============================================================
		// 소켓 연결 Listening 
		// 새로운 클라이언트가 접속을 하면 클라이언트 리스트에 추가한다.
		// ==============================================================

		try {
			while (true) {
				try {
					addClientSocket(s.accept());
					Debug.print(TAG, "run", "Allocate new socket instance for new client.");
				} catch (Exception e) {
					e.getStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); 
			Debug.print(TAG, "run", "Failed to get socket instance - External Service.");
		}
	}
	
	/*
	 * addClientSocket: 
	 * 1. 새로운 소켓을 clientsocket리스트에 ExternalService을 통해 추가한다.
	 * 2. 소켓의 IP 주소를 clientList에 추가한다.
	 */
	private void addClientSocket(Socket s) {
		try {
			ExternalService.addClient(
					new Client(s.getInetAddress().getHostAddress(), s));
			// 클라이언트 추가 후에 디스크정보 저장
			DiskInfo.getInstance().save();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
