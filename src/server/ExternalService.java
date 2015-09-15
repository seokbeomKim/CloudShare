package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import debug.Debug;
import disk.DiskInfo;
import message.Message;
import message.Message.BROADCAST_TYPE;
import message.Message.MESSAGE_TARGET;
import message.Message.MESSAGE_TYPE;
import message.Message.REQUEST_TYPE;
import message.MessageHandler;
import message.MessageReceiver;
import message.MessageSender;
import util.CSService;
import util.IpChecker;

/*
 * 전체적인 시스템 구성
 * Operator와 ExternalService의 Sender가 하나의 쓰레드로,
 * ExternalService와 Receiver가 하나의 쓰레드로 동작한다.
 */
public class ExternalService extends CSService {
	private final String TAG = "ServerService";
	private static ExternalService instance;
	public static ExternalService getInstance() {
		if (instance == null) {
			instance = new ExternalService();
		}
		return instance;
	}
	
	// 클라이언트 리스트 및 소켓통신 관련 

	// 소켓 통신 관련
	private ServerSocket listener;
	private final int portnum_es = 7799;	
	// DiskInfo로부터 초기화한다.
	private List<String> clientList;
	private LinkedList<Socket> clientSocket;
	private SocketListener sock_listener;
	
	private ExternalService() {
		msg_handler = new MessageHandler();
		msg_receiver = new MessageReceiver();
		msg_sender = new MessageSender();
		setClientSocket(new LinkedList<>());
		
		setClientList(DiskInfo.getInstance().getClients());

		// 처음 실행 시, 디스크파일의 정보를 통해 ExternalService에 클라이언트들에 대한 소켓을
		// 초기화한다. **연결 구성**
		// 먼저 소켓서버 리스너를 실행한다.
		sock_listener = new SocketListener();
		sock_listener.start();
		// 이 후, 디스크 파일 정보를 통해 각 클라이언트들에게 클라이언트로써 접속한다.
		createSocketFromClientList();
		
		/* 
		 * ExternalService는 util/DiskOpener에서 디스크 파일을 열었을 때 생성되어 실행된다. 
		 * 생성자에서는 디스크파일에 있는 호스트 및 클라이언트 목록의 IP들에게 노드 attach를 위한 
		 * 요청을 "순차적으로" 보내고 새로운 노드의 발견을 위해 전체 클라이언트들에게 새로운 노드의
		 * 생성을 알린다.
		 */
		
		// 클라이언트 리스트를 통해 Attach 요청
		requestAttachMe();
		
		// 요청이 완료되면 새로운 노드에 대한 정보를 Broadcasting한다.
		// Broadcasting을 받은 클라이언트들은 자신의 파일 정보와 클라이언트 정보를 해당 클라이ᅟ언트에게 
		// 전송해준다.
		String[] v = new String[2];
		v[0] = DiskInfo.getInstance().getDiskip();	// 누구에게
		v[1] = IpChecker.getPublicIP().toString(); // 무엇을 
		Message broadcast_new_node = new Message(
				MESSAGE_TYPE.BROADCAST,						// Broadcast 타입의 
				BROADCAST_TYPE.BROADCAST_NEW_NODE_APPEARED, 	// 새로운 노드 출현 이벤트를
				MESSAGE_TARGET.TARGET_ANYONE,					// 누구에게나 전달 
				v												// disk ip와 현재 클라이언트의 ip와 함께
				);
		send(broadcast_new_node);
	}

	/*
	 * 디스크 파일의 정보로부터 각 노드들로부터 소켓을 요청하여 리스트에 추가한다. 
	 */
	private void createSocketFromClientList() {
		for (int i = 0; i < clientList.size(); i++ ) {
			try {
				Socket s = new Socket(clientList.get(i), getPortnum_es());
				clientSocket.add(s);
			} 
			catch (Exception e) {
				// 소켓 연결에 실패한 경우
				Debug.print(TAG, "createSocketFromClientList", "Failed to add socket : " + clientList.get(i));
			}
		}
	}

	/*
	 * requestAttachMe
	 * 클라이언트 리스트 (ip, clients in DiskInfo)를 통해서 적절한 네트워크 모델을 형성하도록 
	 * (loop가 발생하지 않는) 클라이언트에게 요청한다.
	 */
	private void requestAttachMe() {
		String[] v = new String[2];
		
		for (int i = 0; i < getClientList().size(); i++) {
			v[0] = getClientList().get(i);
			v[1] = IpChecker.getPublicIP();
			
			Message request_attach_me = new Message(
					MESSAGE_TYPE.REQUEST,
					REQUEST_TYPE.REQUEST_ATTACH_NEW_NODE,
					MESSAGE_TARGET.TARGET_ANYONE,
					v
					);
			send(request_attach_me);
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				/*
				 * IMPORTANT
				 * External Service에서 Operator로 명령을 주어 디버그 하는 부분
				 */
				Thread.sleep(1000);
				if (msg_receiver.hasMessage()) {
					msg_handler.handle(msg_receiver.poll());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void receive(Message msg) {
		Debug.print(TAG, "receive", "Server service received message.");
		msg_receiver.receive(msg);
	}

	// TODO send부분 구현 (ExternalService끼리 통신)
	@Override
	public void send(Message msg) {
		Debug.print(TAG, "receive", "Server service sends message.");
		msg_sender.send(msg);
	}

	@Override
	public void handle(Message msg) {
		Debug.print(TAG, "receive", "Server service handles message.");
		msg_handler.handle(msg);
	}

	public static void startService() {
		getInstance().start();
	}

	public List<String> getClientList() {
		return clientList;
	}

	public void setClientList(List<String> clientList) {
		this.clientList = clientList;
	}

	public ServerSocket getListener() {
		return listener;
	}

	public void setListener(ServerSocket listener) {
		this.listener = listener;
	}

	public int getPortnum_es() {
		return portnum_es;
	}

	public LinkedList<Socket> getClientSocket() {
		return clientSocket;
	}

	public void setClientSocket(LinkedList<Socket> clientSocket) {
		this.clientSocket = clientSocket;
	}
}
