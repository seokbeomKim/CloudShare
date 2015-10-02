package message.handler;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.PatternSyntaxException;

import debug.Debug;
import disk.DiskInfo;
import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import server.ExternalService;
import util.CSFileRecorder;
import util.IpChecker;

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

	public void fileList(Message msg) {
		ExternalService.getInstance().receiveBroadcastAnswer(msg);		
	}

	public void fileUpload(Message msg) {
		ExternalService.getInstance().receiveBroadcastAnswer(msg);
	}

	/*
	 * fileLink [request-answer]
	 * 상대방으로부터 공유링크를 받으면 이를 바탕으로 메타 데이터를 만든다.
	 */
	public void fileLink(Message msg) {
		Debug.print(TAG, "fileLink", "Received message from " + msg.getFrom());
		// 공유 링크를 받으면 메타파일에 해당 링크를 추가한다.
		String m = msg.getValue();
		String[] v = m.split(": ");
		
		String metaFile = CSFileRecorder.getMetaFileNameFromPartFileName(v[0]);
		int num = Integer.parseInt(v[0].substring(v[0].length() - 1));
		CSFileRecorder.addLink(metaFile, v[1], num);
		
		// 공유링크를 담아놓은 후에는 파일이 전부 있는지 확인한다.
		// 전부 있는 경우에는 브로드캐스팅을 한다.
		// 이 때 MESSAGE_DETAIL 부분에 파일명을 붙여서 보낸다. 
		if (CSFileRecorder.checkCompletedMetaFile(metaFile + ".cs")) {
			ExternalService.getInstance().allocateBrcstAnswersQueue(MESSAGE_DETAIL.BROADCAST_NEW_METAFILE + ":" + metaFile);
			ExternalService.sendMessageToFamily(new Message(
					MESSAGE_TYPE.BROADCAST,
					MESSAGE_DETAIL.BROADCAST_NEW_METAFILE,
					IpChecker.getPublicIP(),
					null,
					IpChecker.getPublicIP(),
					metaFile	// hidden value
					));
		}
		else {
			Debug.error(TAG, "fileLink", "Failed to broadcast new meta file..(not completed)");
		}
	}

	/*
	 * newMetaFile [broadcast-answer]
	 */
	public void newMetaFile(Message msg) {
		ExternalService.getInstance().receiveBroadcastAnswer(msg);		
	}

}
