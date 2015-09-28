package message.handler;

import java.util.Queue;

import debug.Debug;
import message.Message;
import server.ExternalService;

/*
 * 브로드캐스팅 메세지에 대한 응답 메세지 수집 및 처리하는 클래스
 * ExternalSerivce에서 브로드캐스팅 메세지를 보낼 시점에 생성된다.
 */
public class BrcstAnsHandler extends Thread {
	private final String TAG = "BrcstAnsHandler";
	
	Queue<Message> q;
	String type;
	long threshold = 10 * 1000;		// 메세지 수집 시간(sec * 1000)

	public BrcstAnsHandler(Queue<Message> q, String broadcast_type) {
		this.q = q;
		this.type = broadcast_type;
	}

	@Override
	public void run() {
		Debug.print(TAG, "run", "Collecting broadcast Answer... ");
		
		try {
			Thread.sleep(threshold);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// threshold만큼 기다린 후 다시 메세지 처리
		try {
			Debug.print(TAG, "run", "Stop waiting broadcast answer.");
			ExternalService.getInstance().answerMethods.get(type).invoke(null);
		} catch (Exception e) {
			Debug.error(TAG, "run", "Error occur! message type is " + type);
			e.printStackTrace();
		}
	}
}
