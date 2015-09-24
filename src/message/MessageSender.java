package message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import debug.Debug;
import message.Message.MESSAGE_TYPE;
import server.ExternalService;

/*
 * Message Sender.
 * 메세지 송신 담당 (ExternalService <----> 외부 클라이언트)
 */
public class MessageSender extends Thread {
	private final String TAG = "MessegeSender";
	private static MessageSender instance = null;
	public static MessageSender getInstance() {
		if (instance == null) {
			instance = new MessageSender();
		}
		return instance;
	}
	
	/*
	 * 이전 메세지에 대한 기록을 남긴다. 이유는 FUSE에서 operation 작동시 하나의 연산이 여러번 실행되는 경우가
	 * 있기 때문에 REQUEST 메세지가 여러번 보내지는 경우가 있을 수 있기 때문에 이를 방지하기 위해 이전 메세지에
	 * 대한 기록을 남긴다.
	 */
	private Message prevMsg;
	private long latency = 1000;
	
	/*
	 * CheckConnection
	 * 클라이언트와의 접속을 확인하기 위한 방법은 두 가지가 있다.
	 * 첫 번째는 프로그램을 종료하는 클라이언트가 자신의 종료사실을 다른 클라이언트들에게 알리는 것과
	 * 두 번째는 접속확인을 원하는 클라이언트가 해당 지점에 PING을 보내어 확인하는 방법이다.
	 * 
	 * 첫 번째를 위해 ShutdownHooker를 구현하였지만 프로그램 종료 시 메세지 전달을 위한 쓰레드가 종료될 위험이 있기
	 * 때문에 두 번째 선택지인 PING을 사용하는 방법을 택했다. checkConnection 메서드는 이를 위해 만들어진 메서드이다.
	 */
	public void checkConnection() {
		for (int i = 0; i < ExternalService.getInstance().getClientSocket().size(); i++) {
			try {
				PrintWriter w = new PrintWriter(ExternalService.getInstance().getClientSocket().get(i)
						.getOutputStream(), true);

				w.println(MESSAGE_TYPE.PING);
				
				if (w.checkError()) {
					// 만약 해당 타겟의 클라이언트와의 접속 문제가 있는 경우
					System.err.println("There might be a problem. Remove this client node from your client list.");
					ExternalService.getInstance().getClientList().remove(i);
					ExternalService.getInstance().getClientSocket().remove(i);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		while (true) {
			checkConnection();
			
			if (!ExternalService.getSendQueue().isEmpty()) {
				Debug.print(TAG, "run", "Message exists in sendqueue. Send a message...");
				// send 메세지큐에 메세지가 존재할 경우 타겟에게 메세지를 보낸다.
				Message msg = ExternalService.getSendQueue().poll();
				send(msg);
			}	
			try {
				Thread.sleep(latency);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
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
		String target = (String)msg.getTo();
		Debug.print(TAG, "do_send", "try to send message to " + target);
		Socket s = ExternalService.getClientSocketWithIpAddr(target);
		if (s == null) {
			System.err.println("Cannot find target from the list. do_send failed.");
			Debug.print(TAG, "do_send", "Can't find matched client socket");
		}
		try {
			PrintWriter w = new PrintWriter(s.getOutputStream(), true);
			// 메세지 전송
			w.println(msg.getType());
			w.println(msg.getDetail());
			w.println(msg.getFrom());
			w.println(msg.getTo());
			w.println(msg.getValue());
			Debug.print(TAG, "do_send", "Send message object to " + target);
		} catch (IOException e) {
			System.err.println(e);
		} 
	}	
}
