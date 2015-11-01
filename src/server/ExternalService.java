package server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import debug.Debug;
import debug.MyConstants;
import disk.CloudShareInfo;
import disk.DiskInfo;
import fm.FileListener;
import fm.FilePartSaver;
import fm.FilePartSender;
import fm.FileSender;
import message.IPCMessage;
import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import message.MessageHandler;
import message.MessageReceiver;
import message.MessageSender;
import message.handler.BrcstAnsHandler;
import operation.OperationManager;
import util.IpChecker;
import util.MyConverter;

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
	private List<Client> clientList;
	private Mutex mutexClientList;
	private SocketListener sock_listener;		// Socket listener thread
	private FileListener file_listener;			// Socket listener for file transfer 
	// 2. Message Receiver
	MessageReceiver msg_receiver;
	// 3. Message Sender
	MessageSender msg_sender;
	// 4. Message Handler
	MessageHandler msg_handler;
	
	// 브로드캐스팅 ANSWER 용 큐	 
	HashMap<String, Queue<Message>> brcstAnswerQueue;
	// 파일 관련 브로드캐스팅 시에 transfer할 경로를 담아두는 곳
	public HashMap<String, String> brcstFilePath;	
	public HashMap<String, Method> answerMethods;
	
	private ExternalService() {
		// 메세지 큐 초기화
		mSendQueue = new LinkedList<>();
		mRecvQueue = new LinkedList<>();
		sendQMutex = new Mutex();
		recvQMutex = new Mutex();
		
		brcstAnswerQueue = new HashMap<String, Queue<Message>>();
		brcstFilePath	 = new HashMap<String, String>();
		answerMethods 	 = new HashMap<String, Method>();
		
		clientList = new LinkedList<Client>();
		mutexClientList = new Mutex();
			
		if (sock_listener == null) {
			// ExternalService간 통신 포트 7799
			sock_listener = new SocketListener();	
			sock_listener.start();
		}
		
		if (file_listener == null) {
			// File 전송 위한 포트 7798
			file_listener = new FileListener();
			file_listener.start();
		}
		
		/*
		 * 디스크파일은 프로그램이 실행되어 현재 네트워크에 접속되어 있는 노드로부터 클라이언트 정보를 받아와야한다.
		 * 먼저, tryIpList에 디스크파일로부터 읽어들인 클라이언트들의 리스트를 담고, 이 중에서 연결가능한 노드를 
		 * clientList에 담는다. 
		 * 이 후에, clientList에 저장된 "연결가능한 노드"에게 클라이언트 리스트를 요청하여 clientList의 정보를
		 * 업데이트한다.
		 */
		LinkedList<String> tryIpList = new LinkedList<String>();
		boolean isCreator = false;
		
		try {
			// 디스크 생성자 추가 
			if (DiskInfo.getInstance().getDiskip().compareTo(IpChecker.getPublicIP()) != 0) {
				// 디스크 생성자가 아닌 경우 클라이언트 리스트에 추가
				Debug.print(TAG, "ExternalService", "You are not disk creator.");
				tryIpList.add(DiskInfo.getInstance().getDiskip());
			}
			else {
				Debug.print(TAG, "ExternalService", "You are disk creater.");
				isCreator = true;
			}
			
			// 클라이언트 리스트로부터 추가 
			for (int i = 0; i < DiskInfo.getInstance().getClients().size(); i++) {
				tryIpList.add(DiskInfo.getInstance().getClients().get(i));
			}
		}
		catch (NullPointerException e) {
			Debug.print(TAG, "ExternalService", 
					"Please check your IP configuration.. Maybe you need to run virtualbox.");
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
				if (clientList.size() != 0) {
					Debug.error(TAG, "createSocketFromClientList", 
							"Failed to add socket : " + clientList.get(i).getIpAddr());
				}
				else {
					// 이 경우에는 디스크 파일에 충분한 클라이언트 리스트가 없거나 현재 연결가능한 클라이언트가 없는 상태
					
					// 디스크 파일 생성자인 경우에는 실행이 가능하다.
					if (isCreator)
					{
						break;
					}
					
					Debug.error(TAG, "ExternalService", 
							"No client is available to make connection. Program halted");
					System.exit(MyConstants.NO_CLIENT_AVAILABLE); 
				}
				
				e.printStackTrace();
			}
		}
	}
	
	/**
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
		
		// ANSWER 핸들러 등록 (브로드캐스트에 대한 클라이언트의 응답 처리를 위한 메서드)
		try {
			answerMethods.put(MESSAGE_DETAIL.ANSWER_ATTACH_NEW_NODE, ExternalService.class.getMethod("handler_AttachNode"));
			answerMethods.put(MESSAGE_DETAIL.ANSWER_FILE_LIST, ExternalService.class.getMethod("handler_FileList"));
			answerMethods.put(MESSAGE_DETAIL.ANSWER_FILE_UPLOAD, ExternalService.class.getMethod("handler_Upload"));
			answerMethods.put(MESSAGE_DETAIL.ANSWER_NEW_METAFILE, ExternalService.class.getMethod("handler_NewMetaFile", 
					String.class));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		// 현재 연결되어 있는 클라이언트 리스트 중 하나에게 자신이 갖고 있는 클라이언트 리스트를
		// 요청한다. 이 때 요청받은 클라이언트 리스트를 가지고 clientList를 갱신하며 각각의 
		// 클라이언트에 대한 소켓또한 이 때 초기화된다.
		requestClientList();
	}

	/**
	 * 현재 가지고 있는 클라이언트 리스트 정보를 통해 클라이언트들에게
	 * 리스트 정보를 요청한다. 이 요청으로 전달받는 리스트로 초기화되며
	 * 기존의 리스트 정보는 삭제된다.
	 */
	private void requestClientList() {
		if (clientList.size() != 0) {
			_send(new Message(
					MESSAGE_TYPE.REQUEST,
					MESSAGE_DETAIL.REQUEST_CLIENT_LIST,
					IpChecker.getPublicIP(),
					clientList.get(0).getIpAddr(),
					null
					));
		}
	}

	/**
	 * receiveBrcstAnswers
	 * 브로드캐스팅 메세지를 보낼 때 실행되는 메서드이다. 
	 * 브로드캐스팅 후에 여러 클라이언트에서 메세지가 올 수 있기 때문에 정해진 시간 동안
	 * 메세지를 수집한 후에 처리하는 방식으로 처리한다.
	 * 이 메서드는 새로운 큐를 만들고 해당 큐를 일정시간 후에 처리할 수 있도록 쓰레드를 생
	 * 성하는 역할을 한다.
	 * @param broadcast_type 브로드캐스트 메세지 타입
	 */
	public void allocateBrcstAnswersQueue(String broadcast_type) {
		Queue<Message> q = new LinkedList<Message>();
		brcstAnswerQueue.put(broadcast_type, q);
		
		BrcstAnsHandler bAnsHandler = new BrcstAnsHandler(q, broadcast_type);
		bAnsHandler.start();
	}
	
	/**
	 * 모든 클라이언트들에게 메세지를 보낸다.
	 * @param msg 보낼 메세지
	 */
	public void broadcastMessage(Message msg) {
		for (int i = 0; i < getClientList().size(); i++) {
			Message t = new Message(
					msg.getType(),
					msg.getDetail(),
					IpChecker.getPublicIP(),
					getClientList().get(i).getIpAddr(),
					msg.getValue(),
					msg.getHide()
					);
			_send(t);
		}
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
	
	/**
	 * ip를 통해 새로운 클라이언트를 추가한다. 이 때, 소켓또한 새로 할당받는다.
	 * @param ip 클라이언트 아이피 주소
	 * @throws IOException IO Exception
	 * @throws UnknownHostException 웹 서버의 DNS 설정이 되어있지 않을 때 발생 
	 */
	public void addNewClientWithIPAddr(String ip) 
			throws UnknownHostException, IOException {
		Socket s = new Socket(ip, getPortnum_es());
		clientList.add(new Client(ip, s));
	}
	
	public void removeClientWithIPAddr(String ip) {
		for (int i = 0; i < clientList.size(); i++) {
			Client c = clientList.get(i);
			if (c.getIpAddr().compareTo(ip) == 0) {
				clientList.remove(i);
			}
		}
	}
	
	public void removeClientWithIndex(int i) {
		clientList.remove(i);
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

	public static void addClient(Client client) throws InterruptedException {
		getInstance()._addClient(client);
	}
	/*
	 * _addClient
	 * 클라이언트 리스트에 클라이언트를 추가한다.
	 * 현재 리스트에 추가되었다고 해도 추후 정리가 필요하다.
	 */
	public void _addClient(Client client) throws InterruptedException {
		getMutexClientList().acquire();
		
		// 클라이언트 추가 시에 중복되는 것이 없는지(ip주소) 검사하고, 
		// 중복되는 것이 있다면 해당 인덱스 제거 후 다시 추가한다.
		for (int i = 0; i < clientList.size(); i++) {
			if (clientList.get(i).getIpAddr().compareTo(client.getIpAddr()) == 0) {
				clientList.remove(i);
				break;
			}
		}
		clientList.add(client);
		getMutexClientList().release();			

		// 마지막으로, MessageReceiver에서 사용되는 스트림을 갱신한다.
		if (msg_receiver != null) {
			// 초기화 시에는 receiver가 초기화되어 있지 않을 수도 있다.
			msg_receiver.needToBeRefresh = true;
		}
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
			try {
				_addClient(c);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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

	/**
	 * 브로드캐스트메세지에 대한 ANSWER 타입에 따라 해당 큐에 메세지를 추가한다.
	 */
	public void receiveBroadcastAnswer(Message msg) {
		Debug.print(TAG, "receiveBroadcastAnswer", "Add message to brcstAnswerQueue: " + msg.getDetail() + " from " + msg.getFrom());
		Queue<Message> q = brcstAnswerQueue.get(msg.getDetail());
		q.add(msg);
	}
	
	/*
	 * broadcast 메세지에 대한 핸들러들
	 * 핸들러로 정의된 메서드들은 brcstAnsHandler가 대기를 마치고 깨어나서 
	 * answer로 받아온 메세지를 처리한다.
	 */
	public static void handler_AttachNode() {
		getInstance()._handler_AttachNode();
	}
	
	public void _handler_AttachNode() {
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
	 * [broadcast] handler_FileList
	 * 메타파일 리스트 요청에 대한 ANSWER 메세지들을 처리한다.
	 */
	public static void handler_FileList() {
		getInstance()._handler_FileList();
	}
	private void _handler_FileList() {
		Queue<Message> msgQ = brcstAnswerQueue.get(MESSAGE_DETAIL.BROADCAST_FILE_LIST);
		// 전달받은 메세지 값을 통해서 파일 리스트를 생성하고 현재 파일과 비교하여 요청한다.
		
		Message	msg	= null;
		
		// 파일리스트(Key), 파일 송신자(Value)
		Hashtable<String, String> downloadList = new Hashtable<>();
		
		// 메세지로부터 받은 값을 바탕으로 한 메타파일 리스트
		// 각각의 클라이언트가 갖고 있는 리스트
		LinkedList<String> list 	= null;
		
		if (msgQ.size() == 0) {
			// 아무런 대답이 없는 경우 할당된 큐를 처리하고 ACK을 보낸다.
			brcstAnswerQueue.remove(MESSAGE_DETAIL.BROADCAST_FILE_LIST);
			Debug.error(TAG, "handler_FileList", "\nThere is nothing to do for filelist.");
			OperationManager.getInstance().getMsgQueue().add(new IPCMessage(
					IPCMessage.ACK_FILELIST, ""));
			return;
		}
		for (int i = 0; i < msgQ.size(); i++) {
			msg		= msgQ.poll();
			list	= MyConverter.convertStrToList(msg.getValue());

			for (int j = 0; j < list.size(); j++) {
				if (!CloudShareInfo.getInstance().checkFileExist(list.get(j))) {
					/* 다운로드 받아야할 파일이 있는 경우, 해당 파일에 대한 정보를 리스트에 저장한다. */
					downloadList.put(list.get(j), msg.getFrom());
				}
			}
		}
		
		if (downloadList.size() != 0) {
			Enumeration<String> k = downloadList.keys();
			for (int i = 0; i < downloadList.size(); i++) {
				String fname = k.nextElement();				// 파일 이름
				String sender = downloadList.get(fname);	// 파일 송신자
			
				send(new Message(
						MESSAGE_TYPE.REQUEST,
						MESSAGE_DETAIL.REQUEST_FILE_LIST,
						IpChecker.getPublicIP(),
						sender,
						fname
						));
			}
		}
		else {
			Debug.print(TAG, "_handler_FileList", "Nothing to download from " + msg.getFrom());
		}
		
		// 메세지를 보낸 후, Fuse-mounter에게 FILE_LIST 요청에 대해서 ACK을 보낸다. 
		// FILE_DOWNLOAD는 알아서 진행될 것이기 때문에 *
		OperationManager.getInstance().getMsgQueue().add(new IPCMessage(
				IPCMessage.ACK_FILELIST, ""));
		// answer 큐 정리
		brcstAnswerQueue.remove(MESSAGE_DETAIL.BROADCAST_FILE_LIST);
	}
	
	public static void handler_Upload() {
		getInstance()._handler_Upload();
	}

	private void _handler_Upload() {
		// 파일 업로드 브로드캐스팅 메세지 처리 
		Debug.print(TAG, "_handler_Upload", "Call file upload handler.");
		Queue<Message> msgQ = brcstAnswerQueue.get(MESSAGE_DETAIL.BROADCAST_FILE_UPLOAD);
		String file_path = brcstFilePath.get(MESSAGE_DETAIL.BROADCAST_FILE_UPLOAD);
				
		// 최대 파일 분할 크기를 정한다. division_cnt는 연관된 클라이언트의 갯수와 같다.
		int division_cnt = MyConstants.MAXIMUM_DIVISION_CNT > msgQ.size() ? msgQ.size() : MyConstants.MAXIMUM_DIVISION_CNT;
		
		// 나 자신도 포함되므로 +1
		division_cnt++;
		
		// 분할 하였을 때 offset과 기본 length를 정한다.
		File f = new File(file_path);
		long file_size = f.length();
		long offset = 0;
		long div_len = file_size / division_cnt;
		
		// 각 클라이언트에게 파일 전송을 요청하고 ACK으로서 공유링크를 얻어온다. 
		// division_cnt를 하나 뺀 이유는 count의 한 개가 나 자신이기 때문 
		for (int i = 0; i < division_cnt - 1; i++) {
			Message msg = msgQ.poll();
			
			if (offset + div_len > file_size) {
				div_len = file_size - offset;
			}
			
			// 상대방에게 파일(부분파일) 보내는 부분
			Debug.print(TAG, "_handle_Upload", "offset = " + offset + ", div_len = "+ div_len);
			FilePartSender f_sender = new FilePartSender(msg.getFrom(), f.getPath(), offset, div_len, i);
			f_sender.start();

			// 각각 파일 전송이 끝날 때까지 기다린다.
			try {
				f_sender.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// 상대방에게 공유링크를 요청한다.
			send(new Message(
					MESSAGE_TYPE.REQUEST,
					MESSAGE_DETAIL.REQUEST_FILE_LINK,
					IpChecker.getPublicIP(),
					msg.getFrom(),
					(f.getName() + "." + i)
					));
			offset += div_len;
		}

		// 나머지 부분을 쓰레드를 이용해서 저장한다.
		FilePartSaver postFileSaver = new FilePartSaver(f.getPath(), offset, division_cnt - 1);
        postFileSaver.start();
        
		// answer 큐 정리
		brcstAnswerQueue.remove(MESSAGE_DETAIL.BROADCAST_FILE_UPLOAD);
		brcstFilePath.remove(MESSAGE_DETAIL.BROADCAST_FILE_UPLOAD);
	}
	
	public static void handler_NewMetaFile(String type) {
		getInstance()._handler_NewMetaFile(type);
	}

	private void _handler_NewMetaFile(String type) {
		Debug.print(TAG, "handler_NewMetaFile", "type is " + type);
		
		// 현재 파라미터로 들어온 type값은 [MESSAGE_DETAIL] : [Metadata file name]
		// 과 같은 형태로 되어있다.
		String[] v = type.split(":");
		String meta_fname = v[1];
		
		// 새로운 메타파일을 클라이언트들에게 보낸다.
		Queue<Message> msgQ = brcstAnswerQueue.get(type);
		
		Debug.print(TAG, "handler_NewMetaFile", "You have " + msgQ.size() + " meta file request(s).");
		while( !msgQ.isEmpty() ) {
			Message msg = msgQ.poll();
			
			FileSender f_sender = new FileSender(msg.getFrom(), meta_fname + ".cs");
			f_sender.start();
			try {
				f_sender.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
		// answer 큐 정리
		brcstAnswerQueue.remove(type);
	}
	
	public FileListener getFile_listener() {
		return file_listener;
	}

	public void setFile_listener(FileListener file_listener) {
		this.file_listener = file_listener;
	}
}
