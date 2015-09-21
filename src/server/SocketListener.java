package server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import debug.Debug;

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
		Debug.print(TAG, "run", "Listening socket connection request...");
		if (s == null) {
			try {
				Debug.print(TAG, "run", "Try to make new ServerSocket instance..");
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
		
		try {
			while (true) {
				try {
					Socket socket = s.accept();
					Debug.print(TAG, "run", "Succeed to get new socket instance");
					addClientSocket(socket);
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
	 * addClientSocket: 새로운 소켓을 clientsocket리스트에 ExternalService을 통해 추가한다.
	 */
	private void addClientSocket(Socket s) {
		ExternalService.getInstance().addClientSocket(s);
	}
}
