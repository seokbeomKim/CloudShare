package message;

import debug.Debug;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import message.Message.WHAT;
import message.handler.AnswerHandler;
import message.handler.BrcstHandler;
import message.handler.RequestHandler;
import server.ExternalService;

/*
 * Message Handler.
 * External Service가 가지고 있는 수신된 메세지를 보관하고 있는 큐를 검사하여
 * 큐가 비어있지 않다면 처리하고 비어있다면 계속해서 큐를 감시한다.
 */
public class MessageHandler extends Thread {
	private final String TAG = "MessageHandler";
	RequestHandler	reqHandler = new RequestHandler();
	AnswerHandler	ansHandler = new AnswerHandler();
	BrcstHandler  	brcstHandler = new BrcstHandler();
	
	private static MessageHandler instance = null;
	public static MessageHandler getInstance() {
		if (instance == null) {
			instance = new MessageHandler();
		}
		return instance;
	}

	// 메세지 큐 감시 속도
	private long latency = 250;
	
	// 메세지 처리하는 부분
	public void handle(Message msg)
	{
		Debug.print(TAG, "handle", "Handle message: ");
		msg.getInfo();
		switch (msg.getType()) {
		case MESSAGE_TYPE.REQUEST:
			handle_request(msg);
			break;
		case MESSAGE_TYPE.ANSWER:
			handle_answer(msg);
			break;
		case MESSAGE_TYPE.BROADCAST:
			handle_broadcast(msg);
			break;
		default:
			System.err.println("Unknown message type");
		}
	}
	
	private void handle_request(Message msg) {
		Debug.print(TAG, "handle_request", "Handle request type message");
		if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.REQUEST_CLIENT_LIST)) {
			// 클라이언트 리스트 요청 메세지를 받았을 때
			reqHandler.clientList(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.REQUEST_MAKE_PAIR)) {
			// 새로운 노드로써 요청 받았을 때 
			reqHandler.makePair(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.REQUEST_FILE_LIST)) {
			reqHandler.fileList(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.REQUEST_FILE_DOWNLOAD)) {
			// 파일 리스트에 대해서 다운로드 요청이 들어왔을 때
			reqHandler.fileDownload(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.REQUEST_FILE_LINK)) {
			// 파일 업로드에 대한 요청을 받았을 때
			// 타겟 클라이언트가 파일 업로드를 하고자 해서 해당 클라이언트에게 분할 파일을 보내고자 하는 경우 
			reqHandler.fileLink(msg);
		}
	}
	
	private void handle_answer(Message msg) {
		Debug.print(TAG, "handle_answer", "Handle answer type message");

		if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_CLIENT_LIST)) {
			// 클라이언트 리스트 요청 메세지에 대한 응답을 받았을 때  
			ansHandler.clientList(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_ATTACH_NEW_NODE)) {
			// 새로운 네트워크 노드 위치 요청 메세지에 대한 응답을 받았을 때
			// 브로드캐스팅 메세지에 대한 응답
			ansHandler.attachNewNode(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_MAKE_PAIR)) {
			// 1:1 연결 요청
			Debug.error(TAG, "handle_answer", "There is nothing to do with answer_make_pair message.");
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_FILE_LIST)) {
			ansHandler.fileList(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_FILE_UPLOAD)) {
			ansHandler.fileUpload(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_FILE_LINK)) {
			ansHandler.fileLink(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.ANSWER_NEW_METAFILE)) {
			ansHandler.newMetaFile(msg);
		}
	}
	
	private void handle_broadcast(Message msg) {
		Debug.print(TAG, "handle_broadcast", "Handle broadcast type message");

		if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.BROADCAST_ATTACH_NEW_NODE)) {
			// 클라이언트 리스트 요청 메세지에 대한 브로드캐스팅 메세지를 받았을 때  
			brcstHandler.attachNewNode(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.BROADCAST_FILE_LIST)) {
			// 파일 리스트 요청 메세지에 대한 브로드캐스팅 메세지를 받았을 때
			brcstHandler.fileList(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.BROADCAST_FILE_UPLOAD)) {
			// 파일 업로드 브로드캐스팅 메세지를 받았을 때
			brcstHandler.fileUpload(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.BROADCAST_NEW_METAFILE)) {
			// 새로운 메타파일이 있다는 브로드캐스팅 메세지ᅟ를 받았을 때 
			brcstHandler.newMetaFile(msg);
		}
		else if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.BROADCAST_FILE_UNLINK)) {
			// 새로운 메타파일이 있다는 브로드캐스팅 메세지ᅟ를 받았을 때 
			brcstHandler.fileUnlink(msg);
		}
	}
	
	@Override
	public void run() {
		while (true) {
			// poll된 메세지를 처리하기 위한 레퍼런스 변수
			Message targetMsg = null;
			
			// External Service의 recv 큐를 검사한 후 메세지를 처리한다.
			try {
				ExternalService.recvMutex().acquire();
				
				// 만약 비어있지 않다면 메세지를 처리한다.
				if (!ExternalService.getRecvQueue().isEmpty()) {
					targetMsg = ExternalService.getRecvQueue().poll();
				}
				ExternalService.recvMutex().release();
				
				// 처리할 메세지가 있다면 처리한다.
				if (targetMsg != null) {
					handle(targetMsg);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// latency 처리
			try {
				Thread.sleep(latency);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
