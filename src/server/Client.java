package server;

import java.net.Socket;

/*
 * Client 클래스
 * 
 * Client의 IP주소와 소켓을 담아두는 클래스이다.
 */
public class Client {
	private String ipAddr;
	private Socket socket;
	
	// 네트워크 구성 요소
	Client parent;
	Client lClient;
	Client rClient;
	
	public Client()
	{
		ipAddr = null;
		socket = null;
		
		parent	= null;
		lClient = null;
		rClient	= null;
	}
	
	public Client(String ipAddr) {
		this.ipAddr = ipAddr;
		socket = null;
		
		parent	= null;
		lClient = null;
		rClient	= null;
	}
	
	public Client(String ipAddr, Socket socket) {
		this.ipAddr = ipAddr;
		this.socket = socket;
		parent	= null;
		lClient = null;
		rClient	= null;
	}
	
	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
