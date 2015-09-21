package message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.Queue;

import debug.Debug;
import server.ExternalService;

public class MessageReceiver extends Thread {
	private final String TAG = "MessageReceiver";
	private static MessageReceiver instance = null;
	public static MessageReceiver getInstance() {
		if (instance == null) {
			instance = new MessageReceiver();
		}
		return instance;
	}
	
	LinkedList<ObjectInputStream> is;
	private Queue<Message> receivedMessage;
	
	public MessageReceiver() {
		setReceivedMessage(new LinkedList<Message>());
		is = new LinkedList<>();
	}
	
	@Override
	public void run() {
		readMessage();
	}
	
	public void receive(Message msg) {
		receivedMessage.add(msg);
	}
	
	public Message poll() {
		return receivedMessage.poll();
	}
	
	public Queue<Message> getReceivedMessage() {
		return receivedMessage;
	}
	
	public void setReceivedMessage(Queue<Message> receivedMessage) {
		this.receivedMessage = receivedMessage;
	}

	public boolean hasMessage() {
		return !receivedMessage.isEmpty();
	}
	
	public boolean readMessage() {
		Debug.print(TAG, "readMessage", "Read message...");
		for (int i = 0; i < is.size(); i++) {
			try {
				Message obj = (Message)is.get(i).readObject();
				receive(obj);
				Debug.print(TAG, "readMessage", "A message is read");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/*
	 * refreshStream
	 * receiver가 가지고 있는 스트림의 리스트를 갱신한다.
	 */
	public void refreshStream() {
		
		if (ExternalService.getInstance().getClientSocket().size() !=
				is.size()) {
			is.clear();
			for (int i = 0; i < ExternalService.getInstance().getClientSocket().size(); i++) {
				try {
					is.add(new ObjectInputStream(ExternalService.getInstance().getClientSocket().get(i).getInputStream()));
				} catch (IOException e) {
					System.err.println("Failed to refresh objectinputstream with i = " + i);
					// 클라이언트 소켓 리스트에 문제가 생긴 것이므로 처리한다.
					ExternalService.getInstance().getClientSocket().remove(i);
				}
			}
		}
	}
}
