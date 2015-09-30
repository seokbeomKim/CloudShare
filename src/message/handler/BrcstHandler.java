package message.handler;

import java.util.List;

import debug.Debug;
import disk.CloudShareInfo;
import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import server.Client;
import server.ExternalService;
import util.IpChecker;

/*
 * BocstHandler: broadcast handler
 */
public class BrcstHandler {
	public void attachNewNode(Message msg) {
		/*
		 * 새로운 노드가 들어왔다고 브로드캐스팅을 한다. 
		 */
		List<Client> cl = ExternalService.getClientList();
		Message m = new Message(msg);
		m.setFrom(IpChecker.getPublicIP());
		
		for (int i = 0; i < cl.size(); i++) {
			if (cl.get(i).getIpAddr().compareTo(msg.getFrom()) != 0) {
				// 메세지 송신자는 제외
				m.setTo(cl.get(i).getIpAddr());
				ExternalService.send(m);
			}
		}
		
		// 메세지 전달 후에는 자신의 상태를 판단하여 새로운 노드를 받을 수 있는지 없는지 확인하여 
		// 메세지를 전달한다.
		if (ExternalService.getInstance().checkFamilyAvailable()) {
			Message ans = new Message(
					MESSAGE_TYPE.ANSWER,
					MESSAGE_DETAIL.ANSWER_ATTACH_NEW_NODE,
					IpChecker.getPublicIP(),
					msg.getValue(),
					null
					);
			ExternalService.send(ans);
		}
	}

	/*
	 * fileList
	 * 파일 리스트 요청 브로드 캐스트 메세지를 받았을 때 호출되는 메서드 
	 */
	public void fileList(Message msg) {
		// 파일 리스트 요청을 전달한다.
		List<Client> cl = ExternalService.getClientList();
		Message m = new Message(msg);
		m.setFrom(IpChecker.getPublicIP());
		
		for (int i = 0; i < cl.size(); i++) {
			if (cl.get(i).getIpAddr().compareTo(msg.getFrom()) != 0) {
				// 메세지 송신자는 제외
				m.setTo(cl.get(i).getIpAddr());
				ExternalService.send(m);
			}
		}
		// 메세지 전달 후에는 해당 클라이언트에게 파일 리스트를 보낸다.
		Message ans = new Message(
				MESSAGE_TYPE.ANSWER,
				MESSAGE_DETAIL.ANSWER_FILE_LIST,
				IpChecker.getPublicIP(),
				msg.getValue(),
				CloudShareInfo.getInstance().getFileListStr()
				);
		ExternalService.send(ans);
	
	}

}
