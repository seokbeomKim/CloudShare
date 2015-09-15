package message;

import debug.Debug;

/*
 * MessageSender
 * 메세지 송신 담당 (ExternalService <----> 외부 클라이언트)
 */
public class MessageSender {
	private final String TAG = "MessegeSender";
	
	/*
	 * 이전 메세지에 대한 기록을 남긴다. 이유는 FUSE에서 operation 작동시 하나의 연산이 여러번 실행되는 경우가
	 * 있기 때문에 REQUEST 메세지가 여러번 보내지는 경우가 있을 수 있기 때문에 이를 방지하기 위해 이전 메세지에
	 * 대한 기록을 남긴다.
	 */
	private Message prevMsg;
	public void send(Message msg) {
		Debug.print(TAG, "send", "Send message");
		if (prevMsg == null) {
			// 이전 메세지에 대한 기록이 없다면 바로 보낸다.\
			do_send(msg);
		}
		else {
			// 이전 메세지에 대한 기록이 있다면 해당 메세지와 비교 후 
			// 같은지 확인 후에 메세지를 보낸다.
			if (!msg.compareTo(prevMsg)) {
				do_send(msg);
			}
		}
	}
	
	// 실제로 전송하는 부분
	private void do_send(Message msg) {
		
	}
	
	
}
