package server;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import debug.Debug;
import debug.MyConstants;
import disk.DiskInfo;
import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import message.MessageHandler;
import message.MessageReceiver;
import message.MessageSender;
import util.IpChecker;

/*
 * 전체적인 시스템 구성
 * Operator와 ExternalService의 Sender가 하나의 쓰레드로, 
 * ExternalService와 Receiver가 하나의 쓰레드로 동작한다.
 * 자세한 구성은 src/README 참조할 것 
 */
public class ExternalService {
	private final String TAG = "ExternalService";
	private static ExternalService instance;
	public static ExternalService getInstance() {
		if (instance == null) {
			Debug.print("ExternalService", "getInstance", "Create new instance..");
			instance = new ExternalService();
		}
		return instance;
	}
	
	// Sender와 Receiver를 위한 메세지 큐
	public Queue<Message> mSendQueue;
	public Queue<Message> mRecvQueue;
	// 메세지큐 동기화를 위한 Mutex
	private Mutex sendQMutex;	
	private Mutex recvQMutex;	
	
	// External Service threads
	// 1. Socket Listener: 외부 클라이언트와의 통신을 위한 소켓 등록 및 관리 
	private final int portnum_es = 7799;	
	private List<String> clientList;	// 클라이언트 IP 주소 리스트 
	private LinkedList<Socket> clientSocket;	// 클라이언트 소켓 리스트 
	private Mutex mutexClSockets;
	private SocketListener sock_listener;		// Socket listener thread
	// 2. Message Receiver
	MessageReceiver msg_receiver;
	// 3. Message Sender
	MessageSender msg_sender;
	// 4. Message Handler
	MessageHandler msg_handler;
	
	// 네트워크 상에서의 부모, 자식 클라이언트 링크
	private Socket pClient, lClient, rClient;
	
	private ExternalService() {
		/* 초기화 부분 */
		// 메세지 큐 초기화
		mSendQueue = new LinkedList<>();
		mRecvQueue = new LinkedList<>();
		sendQMutex = new Mutex();
		recvQMutex = new Mutex();		
		// 클라이언트 IP주소 리스트, 소켓 리스트 초기
		setClientSocket(new LinkedList<>());
		setMutexClSockets(new Mutex());
		setClientList(DiskInfo.getInstance().getClients());
		// 클라이언트 링크 null로 초기화
		pClient = null;
		lClient = null;
		rClient = null;
		
		/* 실행 부분 */
		// 1. Socket Listener
		if (sock_listener == null) {
			sock_listener = new SocketListener();
			sock_listener.start();
		}
		
		// 디스크의 생성자를 확인하여 자기 자신인지 아닌지 확인한다.
		// 디스크 생성자가 아닌 경우에는 해당 클라이언트와의 통신을 위한 소켓을 생성하여 리스트에 추가한다.
		// 중요한 것은 디스크 생성자라고 특별해지는 것이 없다. 다만 클라이언트 리스트에 디스크 생성자가 추가되느냐 아니냐가 달라질 뿐이다.
		try {
			if (DiskInfo.getInstance().getDiskip().compareTo(IpChecker.getPublicIP()) != 0) {
				// 디스크 생성자가 아닌 경우 클라이언트 리스트에 추가
				Debug.print(TAG, "ExternalService", "You are not disk creator.");
				addDiskAddrToList();
			}
			else {
				Debug.print(TAG, "ExternalService", "You are disk creater.");
			}
		} catch (NullPointerException e) {
			Debug.print(TAG, "ExternalService", "Please check your IP configuration.. Maybe you need to run virtualbox.");
			e.printStackTrace();
			System.exit(MyConstants.NEED_TO_RUN_VIRTUALBOX);
		}
		
		// 디스크 파일 정보를 통해 각 클라이언트들에게 클라이언트로써 접속한다.
		createSocketFromClientList();
	}
	
	/*
	 * ExternalService가 생성될 때 쓰레드를 동시에 생성하면 쓰레드문제가 발생할 수 있기 때문에
	 * 쓰레드 실행을 인스턴스가 충분히 생성된 뒤로 미룬다.
	 */
	public void postInitialize() {
		// 메세지 handler, receiver, sender 초기화
		msg_handler		= MessageHandler.getInstance();
		msg_receiver	= MessageReceiver.getInstance();
		msg_sender		= MessageSender.getInstance();
		// 2.3.4. 메세지 handler, sender, receiver 실행
		msg_sender.start();
		msg_receiver.start();
		msg_handler.start();
		
		// ExternalService는 util/DiskOpener에서 디스크 파일을 열었을 때 생성되어 실행된다. 
		// 생성자에서는 디스크파일에 있는 호스트 및 클라이언트 목록의 IP들에게 노드 attach를 위한 
		// 요청을 "순차적으로" 보내고 새로운 노드의 발견을 위해 전체 클라이언트들에게 새로운 노드의
		// 생성을 알린다.		
		// 클라이언트 리스트를 통해 Attach 요청 (메세지 송신)
		requestAttachMe();
	}

	private void addDiskAddrToList() {
		try {
			clientList.add(DiskInfo.getInstance().getDiskip());
		} catch (Exception e) {
			// 리스트 추가 실패
			Debug.print(TAG, "addDiskAddrToList", "Failed to add disk ip address to list: " +  
						DiskInfo.getInstance().getDiskip());
		}
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
	 * 
	 * 클라이언트 리스트 (ip, clients in DiskInfo)를 통해서 적절한 네트워크 모델을 형성하도록 
	 * (loop가 발생하지 않는) 클라이언트에게 요청한다.
	 * 어떠한 다른 요청보다도 "우선적으로" 네트워크에 노드로써 구성이 완료되어야 한다.
	 */
	private void requestAttachMe() {
		Debug.print(TAG, "requestAttachMe", "Try to send message... ");
		Debug.print(TAG, "requestAttachMe", "You have " + getClientList().size() + " clients.");
		
		for (int i = 0; i < getClientList().size(); i++) {
			Message request_attach_me = new Message(
					MESSAGE_TYPE.REQUEST,
					MESSAGE_DETAIL.REQUEST_ATTACH_NEW_NODE,
					IpChecker.getPublicIP(),
					getClientList().get(i),
					IpChecker.getPublicIP()
					); 
			Debug.print(TAG, "requestAttachMe", "target = " + getClientList().get(i));
			send(request_attach_me);
		}
	}
	
	/*
	 * getClientSocketWithIpAddr
	 * 클라이언트 리스트로부터 IP주소가 일치하는 것을 찾아 해당 소켓을 리턴해준다.
	 * 실패할 경우에는 null 리턴
	 */
	public static Socket getClientSocketWithIpAddr(String ipaddr) {
		for (int i = 0; i < getInstance().clientSocket.size(); i++) {
			if (getInstance().clientSocket.get(i).getInetAddress().
					getHostAddress().
					compareTo(ipaddr) == 0) {
				return getInstance().clientSocket.get(i);
			}
		}
		return null;
	}

	/*
	 * send
	 * Add message to mSendQueue
	 */
	public void send(Message msg) {
		Debug.print(TAG, "receive", "Server service sends message.");
		mSendQueue.add(msg);
	}

	public void handle(Message msg) {
		Debug.print(TAG, "receive", "Server service handles message.");
		msg_handler.handle(msg);
	}

	public List<String> getClientList() {
		return clientList;
	}

	public void setClientList(List<String> list) {
		this.clientList = list;
	}
	
	// 클라이언트 리스트 관련 메서드
	public void addNewClientWithIPAddr(String ip) {
		clientList.add(ip);
	}
	
	public void removeClientWithIPAddr(String ip) {
		for (int i = 0; i < clientList.size(); i++) {
			if (clientList.get(i).compareTo(ip) == 0) {
				clientList.remove(i);
			}
		}
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

	/*
	 * addClientSocket
	 * 클라이언트의 새로운 소켓을 리스트에 추가한다.
	 * 이 때 해당 소켓이 중복되는지 아닌지 확인하여 중복되면 기존에 등록되어 있던 소켓을 삭제하고
	 * 새로이 등록을 한다.
	 */
	public void addClientSocket(Socket s) {
		// 중복 확인
		for (int i = 0; i < this.clientSocket.size(); i++) {
			if (this.clientSocket.get(i).getInetAddress().getHostAddress().compareTo(
					s.getInetAddress().getHostAddress()) == 0) {
				Debug.print(TAG, "addClientSocket", "Compare ip address between " + clientSocket.get(i) + " and " + 
					s.getInetAddress().getHostAddress());
				// 중복되는 것이 있다면 삭제
				Debug.print(TAG, "addClientSocket", "Duplicated client sent connection request..."
						+ "remove the socket from the list");
				this.clientSocket.remove(i);
				break;
			}
		}
		
		// 소켓 등록
		this.clientSocket.add(s);
	}

	public static void startService() {
		// 단순히 ExternalService 인스턴스가 제대로 초기화 되어있는지 확인하고 
		// 나머지 쓰레드를 확인한다.
		if (!checkInstance()) {
			System.err.println("External Service instance does not exist yet. Do initialize again..");
			ExternalService.getInstance().postInitialize();
		}
	}
	
	private static boolean checkInstance() {
		return instance == null ? false : true;
	}

	public static Mutex sendMutex() {
		return getInstance().getSendQMutex();
	}
	
	public Mutex getSendQMutex() {
		return sendQMutex;
	}

	public void setSendQMutex(Mutex sendQMutex) {
		this.sendQMutex = sendQMutex;
	}

	public static Mutex recvMutex() {
		return getInstance().getRecvQMutex();
	}
	public Mutex getRecvQMutex() {
		return recvQMutex;
	}

	public void setRecvQMutex(Mutex recvQMutex) {
		this.recvQMutex = recvQMutex;
	}
	
	public static Queue<Message> getRecvQueue() {
		return getInstance().mRecvQueue;
	}
	
	public static Queue<Message> getSendQueue() {
		return getInstance().mSendQueue;
	}

	public Mutex getMutexClSockets() {
		return mutexClSockets;
	}

	public void setMutexClSockets(Mutex mutexClSockets) {
		this.mutexClSockets = mutexClSockets;
	}

	/*
	 * 현재 클라이언트 네트워크 노드 연결에서 attach가능한 자리가 있는지 확인한다.
	 */
	public static boolean nodeAttachAble() {
		boolean r = false;
		if (getInstance().pClient == null || 
				getInstance().lClient == null || 
				getInstance().rClient == null) {
			r = true;
		}
		return r;
	}

	public Socket getrClient() {
		return rClient;
	}

	public void setrClient(Socket rClient) {
		this.rClient = rClient;
	}

	public Socket getlClient() {
		return lClient;
	}

	public void setlClient(Socket lClient) {
		this.lClient = lClient;
	}

	public Socket getpClient() {
		return pClient;
	}

	public void setpClient(Socket pClient) {
		this.pClient = pClient;
	}

	/*
	 * attach 시에 현재 다른 곳에 똑같은 노드가 연결되는지도 확인해야한다.
	 */
	public static void attachNode(Socket clientSocketWithIpAddr) {
		// 먼저 중복연결을 확인한다.
		if (getInstance().pClient.getInetAddress().getHostAddress().compareTo(
				clientSocketWithIpAddr.getInetAddress().getHostAddress()) == 0 ||
				getInstance().lClient.getInetAddress().getHostAddress().compareTo(
				clientSocketWithIpAddr.getInetAddress().getHostAddress()) == 0 ||
				getInstance().rClient.getInetAddress().getHostAddress().compareTo(
						clientSocketWithIpAddr.getInetAddress().getHostAddress()) == 0) {
			return;
		}
		
		if (getInstance().pClient == null) {
			getInstance().pClient = clientSocketWithIpAddr;
		}
		else if (getInstance().lClient == null) {
			getInstance().lClient = clientSocketWithIpAddr;
		}
		else if (getInstance().rClient == null) {
			getInstance().rClient = clientSocketWithIpAddr;
		}
		else {
			System.err.println("Failed to attach node. There is no attachable place for new client.");
		}
	}

	/*
	 * 네트워크 상에서 직접적으로 연결되어 있는 클라이언트 노드들에게 메세지를 보낸다.
	 * (parent, child * 2)
	 */
	public static void sendMessageToFamily(Message msg) {
		// TODO sendMessageToFamily
		
	}

	/*
	 * 네트워크 상에서 직접적으로 연결되어 있는 자식 노드 레벨의 클라이언트에게 메세지 전달
	 */
	public static void sendMessageToChildren(Message msg) {
		if (getInstance().lClient != null) {
			msg.setTo(getInstance().lClient.getInetAddress().getHostAddress());
			getInstance().send(msg);
		}
		else if (getInstance().rClient != null) {
			msg.setTo(getInstance().rClient.getInetAddress().getHostAddress());
			getInstance().send(msg);
		}
		else {
			System.err.println("There is nothing to transfer message.");
		}
	}
}
