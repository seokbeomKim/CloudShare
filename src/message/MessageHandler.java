package message;

import debug.Debug;
import server.ExternalService;

/*
 * Message Handler.
 * External Service가 가지고 있는 수신된 메세지를 보관하고 있는 큐를 검사하여
 * 큐가 비어있지 않다면 처리하고 비어있다면 계속해서 큐를 감시한다.
 */
public class MessageHandler extends Thread {
	private final String TAG = "MessageHandler";
	private static MessageHandler instance = null;
	public static MessageHandler getInstance() {
		if (instance == null) {
			instance = new MessageHandler();
		}
		return instance;
	}

	// 메세지 큐 감시 속도
	private long latency = 500;
	
	// 
	public void handle(Message msg)
	{
		Debug.print(TAG, "handle", "Handle message");
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
