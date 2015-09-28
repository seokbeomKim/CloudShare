package server;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
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
import message.Message.WHAT;
import message.MessageHandler;
import message.MessageReceiver;
import message.MessageSender;
import message.handler.BrcstAnsHandler;
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
	private List<Client> clientList;	// 클라이언트 IP 주소 리스트
	private Mutex mutexClientList;
	private SocketListener sock_listener;		// Socket listener thread
	// 2. Message Receiver
	MessageReceiver msg_receiver;
	// 3. Message Sender
	MessageSender msg_sender;
	// 4. Message Handler
	MessageHandler msg_handler;
	
	// 브로드캐스팅 ANSWER 용 큐
	HashMap<String, Queue<Message>> brcstAnswerQueue; 
	public HashMap<String, Method> answerMethods;
	
	// 나의 가족들
	Client parent = null;
	Client lChild = null;
	Client rChild = null;
	
	private ExternalService() {
		/* 초기화 부분 */
		// 메세지 큐 초기화
		mSendQueue = new LinkedList<>();
		mRecvQueue = new LinkedList<>();
		sendQMutex = new Mutex();
		recvQMutex = new Mutex();
		
		brcstAnswerQueue = new HashMap<String, Queue<Message>>();
		answerMethods = new HashMap<String, Method>();
		
		// 클라이언트 IP주소 리스트, 소켓 리스트 초기화
		setMutexClientList(new Mutex());
		clientList = new LinkedList<Client>();
		for (int i = 0; i < DiskInfo.getInstance().getClients().size(); i++) {
			clientList.add(new Client(DiskInfo.getInstance().getClients().get(i)));
		}
			
		/* 실행 부분 */
		// 1. Socket Listener
		if (sock_listener == null) {
			sock_listener = new SocketListener();
			sock_listener.start();
		}
		
		// 디스크의 생성자를 확인하여 자기 자신인지 아닌지 확인한다.
		// 디스크 생성자가 아닌 경우에는 해당 클라이언트와의 통신을 위한 소켓을 생성하여 리스트에 추가한다.
		// 중요한 것은 디스크 생성자라고 특별해지는 것이 없다. 다만 클라이언트 리스트에 디스크 생성자가 추가되느냐 아니냐가 달라질 뿐이다.
		LinkedList<String> tryIpList = new LinkedList<String>();
		
		try {
			if (DiskInfo.getInstance().getDiskip().compareTo(IpChecker.getPublicIP()) != 0) {
				// 디스크 생성자가 아닌 경우 클라이언트 리스트에 추가
				Debug.print(TAG, "ExternalService", "You are not disk creator.");
				tryIpList.add(DiskInfo.getInstance().getDiskip());
			}
			else {
				Debug.print(TAG, "ExternalService", "You are disk creater.");
			}
		} catch (NullPointerException e) {
			Debug.print(TAG, "ExternalService", "Please check your IP configuration.. Maybe you need to run virtualbox.");
			e.printStackTrace();
			System.exit(MyConstants.NEED_TO_RUN_VIRTUALBOX);
		}
		
		// 디스크 파일 정보를 통해서 "하나의 연결가능한 노드"에 네트워크 연결 요청을 한다.
		for (int i = 0; i < tryIpList.size(); i++ ) {
			try {
				Socket s = new Socket(tryIpList.get(i), getPortnum_es());
				_addClient(new Client(
						tryIpList.get(i), s));
				
				// 성공한 경우는 끝내고 다음 메세지 요청을 수행한다.
				break;
			} 
			catch (Exception e) {
				// 소켓 연결에 실패한 경우
				Debug.error(TAG, "createSocketFromClientList", "Failed to add socket : " + clientList.get(i).getIpAddr());
				e.printStackTrace();
			}
		}
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
		
		// ANSWER 핸들러 등록
		try {
			answerMethods.put(MESSAGE_DETAIL.ANSWER_ATTACH_NEW_NODE, ExternalService.class.getMethod("handler_AttachNode"));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		// ExternalService는 util/DiskOpener에서 디스크 파일을 열었을 때 생성되어 실행된다. 
		// 생성자에서는 디스크파일에 있는 호스트 및 클라이언트 목록의 IP들에게 노드 attach를 위한 
		// 요청을 "순차적으로" 보내고 새로운 노드의 발견을 위해 전체 클라이언트들에게 새로운 노드의
		// 생성을 알린다.		
		// 최초, 클라이언트 리스트를 위해 리스트 요청을 한 뒤, 수신된 클라이언트 리스트를 통해 
		// 새로운 접속 사실을 알린다.
		
		// 클라이언트 리스트를 요청한다. 클라이언트 리스트는 "ONLY" 오직 디스크 파일을 위해 존재하는 
		// 데이터이다. 
		requestClientList();
		
		// 해당 클라이언트에게 노드 구성을 요청한다.
		requestAttachMe();
	}

	/*
	 * requestClientList
	 * 
	 * 현재 접속된 클라이언트들에게 새로운 클라이언트 리스트 요청 메세지를 보낸다.
	 * 이 때, ClientList를 받는 이유는 디스크파일을 위한 클라이언트 리스트를 갱신하기 위함이다.
	 * [중요] 절대로 네트워크 연결을 위한 것이 아니다.
	 */
	private void requestClientList() {
		Debug.print(TAG, "requestFileList", "You have " + getClientList().size() + " client(s).");
		for (int i = 0; i < getClientList().size(); i++) {
			Message request_file_list = new Message (
					MESSAGE_TYPE.REQUEST,
					MESSAGE_DETAIL.REQUEST_CLIENT_LIST,
					IpChecker.getPublicIP(),
					getClientList().get(i).getIpAddr(),
					null
					);
			Debug.print(TAG, "requestClientList", "target = " + getClientList().get(i).getIpAddr());
			_send(request_file_list);
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
		Debug.print(TAG, "requestAttachMe", "You have " + getClientList().size() + " client(s).");
		
		/*
		 * 브로드캐스팅 메세지를 보냄과 동시에 ANSWER 메세지를 받기 위해
		 * 새로운 큐를 할당받고 BroadcastAnswer Handler를 통해 일정시
		 * 간 동안 메세지를 받은 후 처리한다. 
		 */
		allocateBrcstAnswersQueue(MESSAGE_DETAIL.BROADCAST_ATTACH_NEW_NODE);
		
		for (int i = 0; i < getClientList().size(); i++) {
			Message request_attach_me = new Message(
					MESSAGE_TYPE.BROADCAST,
					MESSAGE_DETAIL.BROADCAST_ATTACH_NEW_NODE,
					IpChecker.getPublicIP(),
					getClientList().get(i).getIpAddr(),
					IpChecker.getPublicIP()
					);
			Debug.print(TAG, "requestAttachMe", "target = " + getClientList().get(i).getIpAddr());
			_send(request_attach_me);
		}
	}

	/*
	 * receiveBrcstAnswers
	 * 브로드캐스팅 메세지를 보낼 때 실행되는 메서드이다. 
	 * 브로드캐스팅 후에 여러 클라이언트에서 메세지가 올 수 있기 때문에 정해진 시간 동안
	 * 메세지를 수집한 후에 처리하는 방식으로 처리한다.
	 * 이 메서드는 새로운 큐를 만들고 해당 큐를 일정시간 후에 처리할 수 있도록 쓰레드를 생
	 * 성하는 역할을 한다.
	 */
	private void allocateBrcstAnswersQueue(String broadcast_type) {
		Queue<Message> q = new LinkedList<Message>();
		brcstAnswerQueue.put(broadcast_type, q);
		
		BrcstAnsHandler bAnsHandler = new BrcstAnsHandler(q, broadcast_type);
		bAnsHandler.start();
	}

	/*
	 * getClientSocketWithIpAddr
	 * 클라이언트 리스트로부터 IP주소가 일치하는 것을 찾아 해당 소켓을 리턴해준다.
	 * 실패할 경우에는 null 리턴
	 */
	public static Socket getClientSocketWithIpAddr(String ipaddr) {
		for (int i = 0; i < getInstance().clientList.size(); i++) {
			if (getInstance().clientList.get(i).getIpAddr().compareTo(ipaddr) == 0) {
				return getInstance().clientList.get(i).getSocket();
			}
		}
		return null;
	}
	
	public static Client getClientWithIpAddr(String ipaddr) {
		for (int i = 0; i < getInstance().clientList.size(); i++) {
			if (getInstance().clientList.get(i).getIpAddr().compareTo(ipaddr) == 0) {
				return getInstance().clientList.get(i);
			}
		}
		return null;
	}

	/*
	 * send
	 * Add message to mSendQueue
	 */
	public void _send(Message msg) {
		Debug.print(TAG, "send", "External service sends message.");
		try {
			sendQMutex.acquire();
			mSendQueue.add(msg);
			sendQMutex.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	public static void send(Message msg) {
		getInstance()._send(msg);
	}

	public void handle(Message msg) {
		Debug.print(TAG, "receive", "Server service handles message.");
		msg_handler.handle(msg);
	}

	public static List<Client> getClientList() {
		return getInstance()._getClientList();
	}
	public List<Client> _getClientList() {
		return clientList;
	}

	public void setClientList(List<Client> list) {
		this.clientList = list;
	}
	
	// 클라이언트 리스트 관련 메서드
	public void addNewClientWithIPAddr(String ip) {
		clientList.add(new Client(ip));
	}
	
	public void removeClientWithIPAddr(String ip) {
		for (int i = 0; i < clientList.size(); i++) {
			if (clientList.get(i).getIpAddr().compareTo(ip) == 0) {
				clientList.remove(i);
			}
		}
	}

	public int getPortnum_es() {
		return portnum_es;
	}

	/*
	 * addClientSocket
	 * 클라이언트의 새로운 소켓을 리스트에 추가한다.
	 * 이 때 해당 소켓이 중복되는지 아닌지 확인하여 중복되면 기존에 등록되어 있던 소켓을 삭제하고
	 * 새로이 등록을 한다.
	 */
	public void addClientSocket(Socket s) {
		// 중복 확인
		for (int i = 0; i < this.clientList.size(); i++) {
			if (this.clientList.get(i).getIpAddr().compareTo(
					s.getInetAddress().getHostAddress()) == 0) {
				clientList.get(i).setSocket(s);
				break;
			}
		}
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

	public Mutex getMutexClientList() {
		return mutexClientList;
	}

	public void setMutexClientList(Mutex mutexClSockets) {
		this.mutexClientList = mutexClSockets;
	}

	// 메세지 전달 메서드
	// 전달 시 from 값이 바뀐다.
	public static void sendMessageToFamily(Message msg) {
		msg.setFrom(IpChecker.getPublicIP());
		for (int i = 0; i < getClientList().size(); i++) {
			msg.setTo(getClientList().get(i).getIpAddr());
			send(msg);
		}
	}
	public static void sendMessageToParent(Message msg) {
		msg.setFrom(IpChecker.getPublicIP());
		msg.setTo(getInstance().parent.getIpAddr());
		send(msg);
	}	
	public static void sendMessageToChildren(Message msg) {
		msg.setFrom(IpChecker.getPublicIP());
		msg.setTo(getInstance().lChild.getIpAddr());
		send(msg);
		msg.setTo(getInstance().rChild.getIpAddr());
		send(msg);
	}	

	public static void addClient(Client client) {
		getInstance()._addClient(client);
	}
	/*
	 * _addClient
	 * 클라이언트 리스트에 클라이언트를 추가한다.
	 */
	public void _addClient(Client client) {
		// 클라이언트 추가 시에 중복되는 것이 없는지(ip주소) 검사하고, 
		// 중복되는 것이 있다면 해당 인덱스 제거 후 다시 추가한다.
		for (int i = 0; i < clientList.size(); i++) {
			if (clientList.get(i).getIpAddr().compareTo(client.getIpAddr()) == 0) {
				clientList.remove(i);
				break;
			}
		}
		clientList.add(client);
		
		// 마지막으로, MessageReceiver에서 사용되는 스트림을 갱신한다.
		if (msg_receiver != null) {
			// 초기화 시에는 receiver가 초기화되어 있지 않을 수도 있다.
			msg_receiver.needToBeRefresh = true;
		}
	}

	/*
	 * checkFamilyAvailable
	 * 
	 * 현재 parent, children으로써 새로운 노드를 가질 수 있는지 확인한다.
	 * @return true는 가능, false는 불가능(자리없음)
	 */
	public boolean checkFamilyAvailable() {
		if (parent != null && lChild != null && rChild != null) {
			return false;
		}
		return true;
	}

	/*
	 * disposeNewNode
	 * 
	 * 새로운 노드로써 parent, children으로 배치한다.
	 */
	public void disposeNewNode(String ipAddr) {
		String pos = null;
		// 현재 이미 연결되어 있는 노드일 수도, 아닐수도 있기 때문에 먼저 확인한다.
		Client c = getClientWithIpAddr(ipAddr);
		if (c == null) {
			c = connectTo(ipAddr);
		}
		
		if (this.lChild == null) {
			this.lChild = c;
			pos = Message.LEFT_CHILD;
		}
		else if (this.rChild == null) {
			this.rChild = c;
			pos = Message.RIGHT_CHILD;
		}
		else {
			Debug.error(TAG, "disposeNewNode", "There's nothing to do for new node.");
		}
		// 자리 할당 후 해당 노드에게 위치를 새로운 네트워크 관계를 알려준다.
		Message answer = new Message(
				MESSAGE_TYPE.ANSWER,
				MESSAGE_DETAIL.ANSWER_MAKE_PAIR,
				IpChecker.getPublicIP(),
				ipAddr,
				pos
				);
		_send(answer);
		
		Debug.print(TAG, "disposeNewNode", "New client["+ipAddr+"] will be " + pos);
	}
	
	public Client connectTo(String ipAddr) {
		Socket s;
		try {
			s = new Socket(ipAddr, getPortnum_es());
			Client c = new Client(ipAddr, s);
			_addClient(c);
			return c;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * 임시 소켓 할당
	 */
	public Socket allocateSocketTemporarily(String ipAddr) {
		try {
			Socket s = new Socket(ipAddr, getPortnum_es());
			return s;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * 메세지의 ANSWER 타입에 따라 해당 큐에 메세지를 추가한다.
	 */
	public void receiveBroadcastAnswer(Message msg) {
		Debug.print(TAG, "receiveBroadcastAnswer", "Add message to brcstAnswerQueue: " + msg.getDetail() + " from " + msg.getFrom());
		Queue<Message> q = brcstAnswerQueue.get(msg.getDetail());
		q.add(msg);
	}
	
	/*
	 * broadcast 메세지에 대한 핸들러들
	 */
	public static void handler_AttachNode() {
		getInstance()._handler_AttachNode();
	}
	
	public void _handler_AttachNode() {
		Debug.print(TAG, "handler_AttachNode", "Attach Node finalize..");
		// 받은 메세지들 중 가장 빠른 클라이언트를 찾아 attach node request를 한다.
		// 처리 후에는 answer message queue에서 삭제
		Queue<Message> msgQ = brcstAnswerQueue.get(MESSAGE_DETAIL.BROADCAST_ATTACH_NEW_NODE);
		
		long best = -1;
		Message bestMatch = null;
		if (msgQ.size() == 0) {
			Debug.error(TAG, "_handler_AttachNode", "There is no answer from broadcast message: attach new node");
			return;
		}
		
		for (int i = 0; i < msgQ.size(); i++) {
			Message msg = msgQ.poll();
			long start = System.currentTimeMillis();
			try {
				boolean status = InetAddress.getByName(msg.getFrom()).isReachable(30);

				if (!status) {
					continue;
				}
			} catch (IOException e) {
				e.printStackTrace();
				Debug.error(TAG, "_handler_AttachNode", "Failed to ping to host = " + msg.getFrom());
			}
			
			long ellapsed = System.currentTimeMillis() - start;
			if (best < 0) {
				best = ellapsed;
				bestMatch = msg;
			}
			else if (ellapsed < best) {
				best = ellapsed;
				bestMatch = msg;
			}
		}
		
		// REQUEST 메세지 전달
		Message req = new Message(
				MESSAGE_TYPE.REQUEST,
				MESSAGE_DETAIL.REQUEST_MAKE_PAIR,
				IpChecker.getPublicIP(),
				bestMatch.getFrom(),
				null
				);
		_send(req);
		
		// answer 큐 정리
		brcstAnswerQueue.remove(MESSAGE_DETAIL.BROADCAST_ATTACH_NEW_NODE);
	}

	/*
	 * makeClientToFamily
	 * 메세지로부터 값을 읽어 해당 클라이언트의 자리를 결정한다.
	 */
	public void makeClientToFamily(Message msg) {
		Debug.print(TAG, "makeClientToFamily", "Message: This client will be " + msg.getValue() + " of " + msg.getFrom());
		if (Message.is(WHAT.VALUE, msg.getValue(), Message.LEFT_CHILD) || 
				Message.is(WHAT.VALUE, msg.getValue(), Message.RIGHT_CHILD)) {
			parent = getClientWithIpAddr(msg.getFrom());
		}
		else {
			Debug.error(TAG, "makeClientToFamily", "Need to implement more..about extra case");
		}
	}
}
