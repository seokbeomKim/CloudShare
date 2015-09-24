package message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import debug.Debug;
import message.Message.MESSAGE_TYPE;
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
	
	LinkedList<BufferedReader> is;
	
	public MessageReceiver() {
		is = new LinkedList<>();
	}
	
	private long latency = 1000;
	
	@Override
	public void run() {
		while (true) {
			// 현재 receiver가 가지고 있는 InputStream을 갱신한다.
			// InputStream은 소켓의 목록에서부터 받아오는데 연결된 소켓 리스트가 변경되었을 수도 있기 때문에
			// 수시로 갱신해주어야한다.
			refreshStream();
			readMessage();
			
			try {
				Thread.sleep(latency);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void receive(Message msg) {
		try {
			ExternalService.getInstance().getRecvQMutex().acquire();
			ExternalService.getRecvQueue().add(msg);
			ExternalService.getInstance().getRecvQMutex().release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean readMessage() {
		Debug.print(TAG, "readMessage", "You have " + is.size() + " input stream(s).");

		for (int i = 0; i < is.size(); i++) {
			Message obj = new Message();
			try {
				Debug.print(TAG, "readMessage", "Check the message is available");
				if (is.get(i).ready()) {
					// MESSAGE_TYPE으로 PING이 들어올 수 있으니 확인
					String r = is.get(i).readLine();
					if (r.compareTo(MESSAGE_TYPE.PING) == 0) {
						Debug.print(TAG, "readMessage", "client sent PING. Ignore this message.");
						return false;
					}
					obj.setType(r);
					obj.setDetail(is.get(i).readLine());
					obj.setFrom(is.get(i).readLine());
					obj.setTo(is.get(i).readLine());
					obj.setValue(is.get(i).readLine());
					receive(obj);
					Debug.print(TAG, "readMessage", "A message is read");
					return true;
				}
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
		Debug.print(TAG, "refreshStream", "Refresh the stream... you have " + ExternalService.getInstance().getClientSocket().size()
				+ " socket(s).");
		
		if (ExternalService.getInstance().getClientSocket().size() !=
				is.size()) {
			is.clear();
			Debug.print(TAG, "refreshStream", "Refresh client input stream from socket...");
			for (int i = 0; i < ExternalService.getInstance().getClientSocket().size(); i++) {
				Debug.print(TAG, "refreshStream", "client socket at " + i + "'s ip address: " 
						+ ExternalService.getInstance().getClientSocket().get(i));
				try {
					is.add(new BufferedReader(
							new InputStreamReader(ExternalService.getInstance().getClientSocket().get(i).getInputStream())));
				} catch (IOException e) {
					System.err.println("Failed to refresh inputstream with i = " + i);
					// 클라이언트 소켓 리스트에 문제가 생긴 것이므로 처리한다.
					ExternalService.getInstance().getClientSocket().remove(i);
				}
			}
		}
	}
}
