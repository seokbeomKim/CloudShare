package message.handler;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.PatternSyntaxException;

import debug.Debug;
import disk.DiskInfo;
import message.Message;
import server.ExternalService;

/*
 * Answer 메세지를 처리하는 클래스
 */
public class AnswerHandler {
	private final String TAG = "AnswerHandler";
	
	/* 
	 * clientList
	 * 
	 * 클라이언트 리스트 요청에 대한 응답을 받았을 때 처리하는 메서드이다.
	 * 클라이언트 리스트는 단순히 디스크 파일을 위한 것이므로 DiskInfo에 저장한다.
	 */
	
	public void clientList(Message msg) {
		Debug.print(TAG, "clientList", "Refresh client list with message from " + msg.getFrom());
		
		DiskInfo.getInstance().getClients().clear();
		
		// 메세지 송신자를 추가하여 리스트에 추가한다.
		LinkedList<String> items = new LinkedList<String>(Arrays.asList(msg.getValue().split("\\s*,\\s*")));
		
		for (int i = 0; i < items.size(); i++) {
			String cl = items.get(i);
			if (i == 0 || i == items.size() - 1) {
				try {
					cl = cl.replace("[", "");
					cl = cl.replace("]", "");
				} catch (PatternSyntaxException ex) {
					// 패턴 매칭이 되지 않은 경우 == 들어온 값이 비정상
					System.err.println("Ignore: Error with parsing received data: Please check client list data from " + 
							msg.getFrom());
					System.err.println("Value is " + cl);
				}
			}
		}
		// 마지막으로 메세지 수신자의 경우, 자신의 IP주소는 갖고 있지 않을 것이기 때문에 수신자도 리스트에 추가한다.
		items.add(msg.getFrom());
		
		// 디스크 파일 초기화
		for (int i = 0; i < items.size(); i++) {
			DiskInfo.getInstance().getClients().add(items.get(i));
		}
	}

	/*
	 * attachNewNode
	 * 새로운 노드로써 새로운 연결 요청 브로드 캐스팅에 대한 응답 처리.
	 * 일정 시간 동안 브로드캐스팅에 대한 응답을 받은 후 클라이언트 리스트 중 
	 * 한 개를 ping테스트를 통해 속도테스트 후 가장 빠른 클라이언트로 연결 요청을
	 * 하여 노드를 구성한다.
	 */
	public void attachNewNode(Message msg) {
		ExternalService.getInstance().receiveBroadcastAnswer(msg);
	}

	public void makePair(Message msg) {
		ExternalService.getInstance().makeClientToFamily(msg);
	}

}
