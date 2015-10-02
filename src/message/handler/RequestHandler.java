package message.handler;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import debug.Debug;
import fm.FileSender;
import message.Message;
import message.Message.MESSAGE_DETAIL;
import message.Message.MESSAGE_TYPE;
import server.ExternalService;
import util.CloudLogReader;
import util.IpChecker;
import util.MyConverter;

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
				MESSAGE_DETAIL.ANSWER_CLIENT_LIST,
				IpChecker.getPublicIP(),
				msg.getFrom(),
				cl_ipAddr.toString()
				);
		ExternalService.send(answer_clientList);
	}
	public void makePair(Message msg) {
		ExternalService.getInstance().disposeNewNode(msg.getFrom());
	}
	
	/*
	 * fileDownload
	 * 상대방이 파일 다운로드를 하겠다고 요청하는 메세지를 보냈을 때 처리
	 * 이 때, 메세지 안에는 파일 리스트가 담겨있다.
	 */
	public void fileList(Message msg) {
		// 보내야할 파일 리스트 
		LinkedList<String> fileList = MyConverter.convertStrToList(msg.getValue());
		
		Debug.print(TAG, "fileList", "file list = " + fileList.toString());
		
		// Handler에서 파일 다운로드/업로드의 경우 시간이 오래 걸릴 수 있으므로 쓰레드를 만들어 처리
		for (int i = 0; i < fileList.size(); i++) {
			File f = new File(fileList.get(i));
			FileSender f_sender = new FileSender(msg.getFrom(), fileList.get(i));
			f_sender.start();	
		}
	}
	public void fileDownload(Message msg) {
		LinkedList<String> fileList = MyConverter.convertStrToList(msg.getValue());
		
		Debug.print(TAG, "fileDownload", "file list = " + fileList.toString());
	}
	
	/*
	 * 클라이언트(새로운 파일을 업로드하고자 하는)로부터 file upload 요청이 들어왔을 때 처리
	 * 상대방이 이미 파일을 보낸 상태에서 request를 보내는 것이기 때문에 상대방에게 공유 링크를
	 * ACK으로 보내준다.
	 */
	public void fileLink(Message msg) {
		/*
		 * FUSE-mounter로부터 CloudShare 에 파일이 업로드 되는 경우 해당 파일의 공유링크를
		 * $HOME/.cslog에 저장하도록 설계하였다. 공유링크는 해당 파일로부터 값을 읽어 리턴해준다.
		 */
		msg.getInfo();
		String shared_link = null;
		
		// Request 요청한 클라이언트가 필요로 하는 링크의 파일 이름
		String fname = msg.getValue();
		shared_link = CloudLogReader.getLink(fname);
		
		Message answer_clientList = new Message(
				MESSAGE_TYPE.ANSWER,
				MESSAGE_DETAIL.ANSWER_FILE_LINK,
				IpChecker.getPublicIP(),
				msg.getFrom(),
				shared_link
				);
		ExternalService.send(answer_clientList);		
	}
}
