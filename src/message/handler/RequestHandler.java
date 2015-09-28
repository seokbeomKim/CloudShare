package message.handler;

import java.util.LinkedList;
import java.util.List;

import debug.Debug;
import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import server.ExternalService;
import util.IpChecker;

/*
 * Request 메세지를 처리하는 클래스
 * MessageHandler에서 메세지를 처리하지 않고 어떠한 타입인지 확인한 뒤 
 * RequestHandler의 적절한 메서드를 호출하도록 설계되었다.
 */
public class RequestHandler {
	private final String TAG = "RequestHandler";
	/*
	 * clientList: 클라이언트 리스트 요청 핸들러
	 * 
	 *  최초 접속 클라이언트의 경우, 최초에 연결되는 클라이언트는 (디스크 파일을 열면서) 한 개밖에 없으므로, 
	 * 연결된 클라이언트에게 리스트를 요청할 필요가 있다. 
	 */
	public void clientList(Message msg) {
		Debug.print(TAG, "clientList", "handler for request_clientlist message. ");
		
		// clientList IP주소 리스트를 만든다.
		List<String> cl_ipAddr = new LinkedList<String>();
		for (int i = 0; i < ExternalService.getClientList().size(); i++) {
			cl_ipAddr.add(ExternalService.getClientList().get(i).getIpAddr());
		}
		
		Debug.print(TAG, "clientList", "ClientList is " + cl_ipAddr.toString());
		
		// 리스트를 완성한 후 메세지를 완성하여 SendQueue에 추가한다.
		Message answer_clientList = new Message(
				MESSAGE_TYPE.ANSWER,
				MESSAGE_DETAIL.ANSWER_FILE_LIST,
				IpChecker.getPublicIP(),
				msg.getFrom(),
				cl_ipAddr.toString()
				);
		ExternalService.send(answer_clientList);
	}
	public void makePair(Message msg) {
		ExternalService.getInstance().disposeNewNode(msg.getFrom());
	}
}
