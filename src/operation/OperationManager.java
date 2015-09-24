package operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import debug.Debug;
import disk.DiskInfo;
import message.IPCMessage;
import message.Message;
import message.Message.MESSAGE_TYPE;
import server.ExternalService;

/*
 * OperationManager - 메인 쓰레드
 * 
 * FUSE_Program(written in C) <---------> Operation Manager <---------> JAVA External Service
 * 
 * Operation Manager는 두 개의 독립적인 프로세스간의 통신을 담당하며 이를 위해
 * Socket 통신을 기반으로 IPC를 한다.
 * 
 */
public class OperationManager {
	private final String TAG = "OperationManager";
	
	// IPC 통신 관련
	private final int latency = 500;	// 0.5 seconds
	private final int PortNum_out = 7788;
	private final int PortNum_in = 7789;
	public Mutex lock;
	private Socket socket_in;
	private Socket socket_out;
	private ServerSocket listener;
	private ServerSocket listener2;
	
	// 메세지 큐 (External Service로부터 수신된 메세지를 위한 큐)
	private Queue<Message> msgQueue;

	PrintWriter fd_out;
	BufferedReader fd_in;	
	
	// 인스턴스 관련
	private static OperationManager instance;
	public static OperationManager getInstance() {
		if (instance == null) {
			instance = new OperationManager();
		}
		return instance;
	}
	
	// 마운트 포인트
	private String mountpoint;
	
	private OperationManager() {
		setMsgQueue(new LinkedList<Message>());
		setMountpoint(System.getenv("HOME") + File.separator + "CloudShare");
	}
	
	/*
	 * private_opendisk
	 * 
	 * 디스크 파일의 정보를 읽고 DiskInfo 객체를 초기화한다.
	 */
	@SuppressWarnings("null")
	private void private_opendisk(String path) {
		DiskInfo diskInfo = DiskInfo.getInstance();
		
		Debug.print(TAG, "private_opendisk", "path = "+path);
		JSONParser parser = new JSONParser();
		Object obj;
		try { 
			obj = parser.parse(new FileReader(new File(path)));
	        JSONObject json_obj = (JSONObject) obj;
			diskInfo.setDiskid(json_obj.get("diskid").toString());
			diskInfo.setDiskip(json_obj.get("ip").toString());
			diskInfo.setDiskname(json_obj.get("diskname").toString());
			diskInfo.setOwner(json_obj.get("email").toString());
			
			JSONArray arr = (JSONArray)json_obj.get("clients");
			if (arr != null || arr.size() == 0) {
				for( int i = 0; i < arr.size(); i++) {
					diskInfo.getClients().add(arr.get(i).toString());
				}
			}
			else {
				// 클라이언트 리스트가 없다면 
				System.out.println("Disk file has no client list.");
			}
			
		} catch (Exception e) {
			System.err.println("Failed to open disk at " + path);
			System.err.println(e);
		}
	}
	
	/*
	 * private_mount
	 * FUSE를 통해서 mount point에 가상의 파일시스템을 마운트하는 부분이다.
	 * FUSE관련된 부분은 C로 프로그래밍된 프로그램을 동작시킨다. 이 때 IPC를 위해
	 * Socket통신을 사용한다.
	 */
	private void private_mount() {
		// 마운트 프로그램(FUSE) 실행
		// TODO 마운트 프로그램 실행되는 부분을 자바 프로그램이 실행되었을 때 자동으로 하도록 구현해야 함.
		// executeFUSEProgram();
		
		// 소켓 통신 준비
		while (true) {
			doSocketConnection();
		}
	}

	/*
	 * doSocketConnection
	 * 소켓 연결을 만들고 FUSE-mounter와 소켓 통신을 한다.
	 */
	private void doSocketConnection() {
		System.out.println("Waiting for new connection...");
		try {
			listener = new ServerSocket(PortNum_in);
			listener2 = new ServerSocket(PortNum_out);
			socket_in = listener.accept();
			socket_out = listener2.accept();
			
			System.out.println("Succeed to connect FUSE-mounter throughout SOCK");
			fd_out= new PrintWriter(socket_out.getOutputStream(), true);
		    fd_in = new BufferedReader(
		        new InputStreamReader(socket_in.getInputStream()));
		    /*
		     * 프로그램 종료때까지 Mounter 프로그램과 IPC(Socket) 통신한다.
		     * 통신 방식은 IPC message 타입을 먼저 보낸 뒤 FUSE-mounter에게 ACK을 보내면 
		     * FUSE-mounter에서 해당 메세지에 대한 값을 보낸다. 값을 받은 뒤에는 수신완료에 대한
		     * 확인으로 다시 ACK을 보낸다. 
		     */
		    while (true) {
				// FUSE-mounter 와의 통신
				try {
					// FUSE-mounter로부터 메세지 확인 및 처리
					HandleMessageFromFuseMounter();
					// External Service로부터의 메세지 확인 및 처리
					HandleMessageFromExternalService();
				} catch (NullMessageException e1) {
					// 메세지가 null인 경우 client가 종료되었거나 비정상적인 상태를 의미하므로 연결을 중지한다.
					System.err.println("Guess the client has been halted. Close the connection");
					break;
				}
				
				// latency 설정
		    	try {
					Thread.sleep(latency);
				} catch (InterruptedException e) {
					System.err.println("Error when doing sleep(TIME_FOR_RETRY)");
				}
		    }
		    
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Failed to get server socket.");
		} catch (NullPointerException e) {
			System.err.println("Null pointer exception... maybe connection with client might be closed.");
			System.err.println("Restart operator server...");
		}
		
		// 클라이언트와의 접속이 끊긴 경우
		try {		
			// Close socket
			listener.close();
			listener2.close();
		} catch (IOException e) {
			System.err.println("Failed to close socket...");
		}
	}
	
	/*
	 * Handle message from external service
	 * 
	 * External Service의 receiver는 외부클라이언트로부터 메세지를 받아 recvQueue에 메세지를
	 * 저장한다. 이를 External Service의 handler에서 처리하게 되는데 FUSE Mounter와의 통신이
	 * 필요할 경우에 OperationManager의 큐에 등록되게 된다. 해당 메서드에서는 큐를 검사하여 
	 * 메세지 유무를 검사하고 처리하는 역할을 한다.
	 */
	private void HandleMessageFromExternalService() {
		if (!msgQueue.isEmpty()) {
			Debug.print(TAG, "HandleMessageFromExternalService", 
					"Message queue is not empty! Do handle a message from external service");
			Message msg = msgQueue.poll();
			sendMessageToFuseMounter(msg.toIPCMessage());
		}
	}

	/*
	 * HandleMessageFromFuseMounter
	 * 
	 * FUSE-mounter로부터 메세지를 수신하고 처리한다.
	 * FUSE-Mounter로부터 수신되는 메세지는 다음의 특징을 갖는다.
	 * 메세지 타입 -> 메세지 값
	 * 때문에 각각의 경우에 대해서 처리를 해준다.
	 */
	private void HandleMessageFromFuseMounter() throws IOException, NullMessageException {
    	String r = fd_in.readLine(); 
    	if (r == null) {
    		throw new NullMessageException();
    	}
    	// 메세지가 수신되면 메세지를 완성하여 처리한다.
    	Message request = new Message();
    	if (r.compareTo(Message.MESSAGE_TOKEN) == 0) {	// 처음 수신된 메세지는 ::__:: 토큰
    		// 메세지 타입 및 디테일 설정
    		String t = fd_in.readLine();	
    		request.setType(t);
    		request.setDetail(request.getType());
    	}
    	// 메세지 값 설정
    	request.setValue(fd_in.readLine().substring(2));
    	
    	if(fd_in.readLine().compareTo(Message.MESSAGE_TOKEN) == 0) {
    		Debug.print(TAG, "receiveAndHandleMessage", "Complete to establish message for external service.");
    		// ExternalService의 SendQueue에 메세지를 등록한다. 
    		try {
				ExternalService.getInstance().getSendQMutex().acquire();
	    		ExternalService.getSendQueue().add(request);
	    		ExternalService.getInstance().getSendQMutex().release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
	}
	/*
	 * sendACKtoClient(deprecated)
	 * - method for debugging
	 * @param type 메세지 종류 (IPCMessage type)
	 * @param aboutType 메세지 종류에 대한 메세지에 대한 ACK일 경우 true, 값에 대한 ACK일 경우 false
	 * type값에 따라 적절한 ACK을 클라이언트에게 보낸다.
	 */
	@SuppressWarnings("unused")
	private void sendACKtoClient(String type, boolean aboutType) {
		Debug.print(TAG, "sendACKtoClient", "Send ACK to client");
		if (aboutType) {
			Debug.print(TAG, "sendACKtoClient", "Send ACK(" + IPCMessage.findAppropriateACKFromRequest(type) + ") to client");
			fd_out.println(IPCMessage.findAppropriateACKFromRequest(type));
		}
		else {
			// 값 수신에 대한 ACK
			Debug.print(TAG, "sendACKtoClient", "Send ACK(" + IPCMessage.ACK_VALUE + ") to client");
			fd_out.println(IPCMessage.ACK_VALUE);
		}
		fd_out.flush();
	}

	/*
	 * handleMessage
	 * TODO handleMessage 구현 
	 * FUSE-Mounter가 보낸 메세지를 처리하는 부분이다.
	 */
	private void handleMessage(IPCMessage msg) {
		Debug.print(TAG, "handleMessage", "Message : " + msg.getType() + ", " + msg.getValue());
	}
	
	/*
	 * sendMessage
	 * FUSE-Mounter에게 메세지를 보내는 메서드.
	 * 처음 Connection에서는 ACK을 보내면서 확인했지만 로컬에서만 이루어지고 간단하므로 
	 * ACK을 따로 받지 않고 보내기만 한다. 
	 * (ACK을 보내는 것이 아니다)
	 */
	private void sendMessageToFuseMounter(IPCMessage msg) {
		Debug.print(TAG, "sendMessage", "Message : " + msg.getType() + ", " + msg.getValue());
		String value = msg.getType() + "::" + msg.getValue();
		fd_out.println(value);
		fd_out.flush();
	}

	/*
	 * executeFUSEProgram
	 * C로 구현된 프로그램을 실행하는 코드
	 */
	private void executeFUSEProgram() {
		try {
			@SuppressWarnings("unused")
			Process process = Runtime.getRuntime().exec("./CloudShare");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Failed to execute FUSE based program");
		}
	}
	
	/*
	 * OpenDisk
	 * 디스크 파일을 통해서 디스크 정보를 초기화하고 실제로 디스크를 마운트하는 부분
	 */
	public static void OpenDisk(String path) {
		getInstance().private_opendisk(path);	// 디스크 정보 초기화 
	}
	
	/*
	 * mount
	 * 정해진 디렉토리에 마운트를 시도한다.
	 */
	public static void mount() {
		getInstance().private_mount();			// FUSE 마운트
	}
	
	/*
	 * updateDisk
	 * @param filelist 파일리스트
	 * 파라미터로 전해받은 파일 리스트를 통해서 disk를 업데이트한다.
	 */
	public static void updateDisk(List v) {
		getInstance().private_updateDisk(v);
	}

	private void private_updateDisk(List filelist) {
		IPCMessage msg = new IPCMessage(IPCMessage.REQUEST_REFRESH, filelist.toString());
		sendMessageToFuseMounter(msg);
	}

	public String getMountpoint() {
		return mountpoint;
	}

	public void setMountpoint(String mountpoint) {
		this.mountpoint = mountpoint;
	}

	public Queue<Message> getMsgQueue() {
		return msgQueue;
	}

	public void setMsgQueue(Queue<Message> msgQueue) {
		this.msgQueue = msgQueue;
	}
	
}
