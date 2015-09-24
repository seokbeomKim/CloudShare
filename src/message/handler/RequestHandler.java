package message.handler;

import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import server.ExternalService;
import util.IpChecker;

/*
 * Request 메세지를 처리하는 클래스
 */
public class RequestHandler {

	public void attachNewNode(Message msg) {
		/*
		 * attachNewNode request 메세지를 처리한다.
		 * 
		 * 해당 클라이언트에게 attach할 수 있는지 확인하고 가능하면 Answer을,
		 * 가능하지 않다면 현재 연결되어 있는 자식노드들에게 메세지를 전달한다.
		 */
		if (ExternalService.nodeAttachAble()) {
			// 가능한 자리가 있다면 해당 자리에 해당 클라이언트의 소켓을 연결
			ExternalService.attachNode(ExternalService.getClientSocketWithIpAddr(msg.getValue()));
			// 해당 클라이언트에게 ACK을 보낸다.
			ExternalService.sendMessageToFamily(new Message(
					MESSAGE_TYPE.ANSWER,
					MESSAGE_DETAIL.ANSWER_ATTACH_NEW_NODE,
					IpChecker.getPublicIP(),
					msg.getValue(),
					msg.getValue()
					));
		}
		else {
			// 직접적으로 연결되어 있는 클라이언트에게 메세지를 보낸다.
			ExternalService.sendMessageToChildren(msg);
		}
	}

}
