package message;

import debug.Debug;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import message.Message.WHAT;
import message.handler.RequestHandler;
import server.ExternalService;

/*
 * Message Handler.
 * External Service가 가지고 있는 수신된 메세지를 보관하고 있는 큐를 검사하여
 * 큐가 비어있지 않다면 처리하고 비어있다면 계속해서 큐를 감시한다.
 */
public class MessageHandler extends Thread {
	private final String TAG = "MessageHandler";
	RequestHandler reqHandler = new RequestHandler();
	
	private static MessageHandler instance = null;
	public static MessageHandler getInstance() {
		if (instance == null) {
			instance = new MessageHandler();
		}
		return instance;
	}

	// 메세지 큐 감시 속도
	private long latency = 500;
	
	// 메세지 처리하는 부분
	public void handle(Message msg)
	{
		Debug.print(TAG, "handle", "Handle message");
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
	
	// TODO handle_request
	private void handle_request(Message msg) {
		Debug.print(TAG, "handle_request", "Request type message");

		// Request "attach new node"
		// 클라이언트가 프로그램 실행을 통해 네트워크에 새로 접속시에 클라이언트에게 노드 자리를 요청하는 메세지
		if (Message.is(WHAT.DETAIL, msg.getDetail(), MESSAGE_DETAIL.REQUEST_ATTACH_NEW_NODE)) {
			reqHandler.attachNewNode(msg);
		}
	}
	
	// TODO handle_answer
	private void handle_answer(Message msg) {
		Debug.print(TAG, "handle_answer", "Answer type message");
		
	}
	
	// TODO handle_broadcast
	private void handle_broadcast(Message msg) {
		Debug.print(TAG, "handle_broadcast", "Broadcast type message");

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
