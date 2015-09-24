package message;

import java.io.Serializable;

/*
 * ExternalService용 메세지
 * 메세지 구성요소 : 누가, 누구에게, 무엇을, (언제)
 */
public class Message implements Serializable {
	public static class MESSAGE_TYPE {
		public static final String REQUEST = "request";
		public static final String BROADCAST = "broadcast";
		public static final String ANSWER = "answer";
		public static final String PING	= "ping";
	};
	public static class MESSAGE_DETAIL {
		public static final String REQUEST_ATTACH_NEW_NODE	= "AttachNewNode";
		public static final String REQUEST_FILE_UPLOAD 		= "Upload";
		public static final String REQUEST_FILE_DOWNLOAD 	= "Download";
		public static final String REQUEST_FILE_LIST		= "Filelist";
		
		public static final String ANSWER_ATTACH_NEW_NODE	= "AttachNewNode";
		
		public static final String BROADCAST_CLIENT_EXIT	= "ClientExit";
	};
	public static class ANSWER_TYPE {
		public static final String ANSWER_NEW_NODE_APPEARED = "NewNodeAppeared";
		public static final String ANSWER_ATTACH_NEW_NODE = "AttachNewNode";
	};
	public static class BROADCAST_TYPE {
		public static final String BROADCAST_NEW_NODE_APPEARED = "NewNodeAppeared";
	};
	
	public static final String MESSAGE_TOKEN = "::__::";
	public static final String MESSAGE_SPLITTER 	= ";;";
	public static final String MESSAGE_NOTHING = "nothing";
	
	private String type;			// 메세지 타입
	private String to;					// 메세지 수신자
	private String from;				// 메세지 송신자
	private String detail;				// 메세지 내용 1 (구체적 타입) 
	
	private String value;				// 메세지 내용 2 (메세지 값)
	
	public Message() {}
	
	public Message(String msgtype, String specific_type, String from, String to) {
		this.setType(msgtype);
		this.setFrom(from);
		this.setTo(to);
		this.setValue(null);
		this.setDetail(specific_type);
	}
	
	public Message(String msgtype, String specific_type, String from, String to, String value) {
		this.setType(msgtype);
		this.setFrom(from);
		this.setTo(to);
		this.setValue(value);
		this.setDetail(specific_type);
	}
	
	public static enum WHAT {
		TYPE,
		DETAIL,
		FROM,
		TO,
		VALUE,
	};
	
	/*
	 * what		A is B
	 * (type)
	 */
	public static boolean is(WHAT what, String A, String B) {
		boolean r = false;
		if (A.compareTo(B) == 0) {
			r = true;
		}
		return r;
	}

	// 메세지가 서로 같은지 비교한다.
	// 서로 같으면 true, 다르면 false를 리턴한다.
	public boolean compareTo(Message prevMsg) {
		if (prevMsg.getType() == getType()) {
			if (prevMsg.getDetail() == getDetail()) {
				if (prevMsg.getValue() == prevMsg.getValue()) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * TODO IPC 메세지로의 변환
	 */
	public IPCMessage toIPCMessage() {
		IPCMessage r = new IPCMessage();
		
		return null;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
