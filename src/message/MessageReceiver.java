package message;

import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.Queue;

public class MessageReceiver {
	private final String TAG = "MessageReceiver";
	
	
	private Queue<Message> receivedMessage;
	
	public MessageReceiver() {
		setReceivedMessage(new LinkedList<Message>());
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
}
