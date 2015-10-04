package message.handler;

import java.util.Queue;

import debug.Debug;
import message.Message;
import message.Message.MESSAGE_DETAIL;
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
		Debug.print(TAG, "run", "Collecting broadcast ("+type+") Answer... ");
		
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
			
			// 핸들러가 정해지지 않은 경우 메타파일인지 확인한다.
			String[] v = type.split(":");
			if (v[0].compareTo(MESSAGE_DETAIL.BROADCAST_NEW_METAFILE) == 0) {
				try {
					ExternalService.getInstance().answerMethods.get(MESSAGE_DETAIL.BROADCAST_NEW_METAFILE).invoke(
							new String(), type);
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			}
		}
	}
}
