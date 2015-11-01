package message.handler;

import java.io.IOException;
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
	

	/**
	 * 클라이언트 리스트 요청에 대한 응답을 받았을 때 처리하는 메서드이다.
	 * DiskInfo 뿐만 아니라 ExternalService의 클라이언트 리스트도 초기화한다. 
	 * @param msg 클라이언트로부터 전달받은 클라이언트 리스트 정보
	 */
	public void clientList(Message msg) {
		Debug.print(TAG, "clientList", 
				"Refresh client list with message from " + msg.getFrom());
		
		DiskInfo.getInstance().getClients().clear();
		
		/* 파싱할 데이터 */
		LinkedList<String> items = new LinkedList<String>(
				Arrays.asList(msg.getValue().split("\\s*,\\s*")));
		/* 괄호 없앤 후 추가될 리스트 */
		LinkedList<String> cl_lst = new LinkedList<String>();
		
		for (int i = 0; i < items.size(); i++) {
			String cl = items.get(i);
			if (i == 0 || i == items.size() - 1) {
				try {
					cl = cl.replace("[", "");
					cl = cl.replace("]", "");
					cl_lst.add(cl);
				} catch (PatternSyntaxException ex) {
					// 패턴 매칭이 되지 않은 경우 == 들어온 값이 비정상
					System.err.println("Ignore: Error with parsing received data: "
							+ "Please check client list data from " + 
							msg.getFrom());
					System.err.println("Value is " + cl);
				}
			}
		}
		
		// 마지막으로 메세지 수신자의 경우, 자신의 IP주소는 갖고 있지 않을 것이기 
		// 때문에 수신자도 리스트에 추가한다.
		cl_lst.add(msg.getFrom());
		
		// 자기자신은 제외한다.
		cl_lst.remove(IpChecker.getPublicIP());
		
		// 디스크 정보 초기화
		for (int i = 0; i < cl_lst.size(); i++) {
			DiskInfo.getInstance().getClients().add(cl_lst.get(i));
		}
		// 디스크 정보 저장
		DiskInfo.getInstance().save();
		
		// DiskInfo를 초기화한 후에는 ExternalService의 클라이언트 리스트를 초기화한다.
		// 리스트 송신자에 대한 소켓은 이미 저장되어 있으므로 추가될 대상에서 제외시킨다.
		cl_lst.remove(msg.getFrom());
		for (int i = 0; i < cl_lst.size(); i++) {
			try {
				Debug.print(TAG, "clientList", "Add new client: " + cl_lst.get(i));
				ExternalService.getInstance().addNewClientWithIPAddr(cl_lst.get(i));
			} catch (IOException e) {
				Debug.print(TAG, "clientList", "Failed to add new client(" + cl_lst.get(i) + ")");
				e.printStackTrace();
			}
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
			Debug.print(TAG, "fileLink", "Notify new metafile(" + metaFile + ") to other clients....");
			ExternalService.getInstance().broadcastMessage(new Message(
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
