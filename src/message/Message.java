package message;

import message.Message.MESSAGE_TARGET;

/*
 * ExternalService용 메세지
 * 메세지 구성요소 : 누가, 누구에게, 무엇을, (언제)
 */
public class Message {
	// 메세지 타입
	public static enum MESSAGE_TYPE {
		REQUEST,
		ANSWER,
		BROADCAST,
		NONE,
	};
	public static final String MESSAGE_TYPE_REQUEST = "request";
	public static final String MESSAGE_TYPE_BROADCAST = "broadcast";
	public static final String MESSAGE_TYPE_answer = "answer";
	
	public static enum REQUEST_TYPE {
		REQUEST_ATTACH_NEW_NODE,
		REQUEST_FILE_UPLOAD,
		REQUEST_FILE_DOWNLOAD,
	};
	public static final String REQUEST_ATTACH_NEW_NODE = "attachnewnode";
	public static final String REQUEST_FILE_UPLOAD = "upload";
	public static final String REQUEST_FILE_DOWNLOAD = "download";
	
	
	public static enum ANSWER_TYPE {
		ANSWER_NEW_NODE_APPEARED,
		ANSWER_ATTACH_NEW_NODE,
	};
	public static final String ANSWER_NEW_NODE_APPEARED = "newnodeappeared";
	public static final String ANSWER_ATTACH_NEW_NODE = "attachnewnode";
	
	
	public static enum BROADCAST_TYPE {
		BROADCAST_NEW_NODE_APPEARED,
	};
	
	public static enum MESSAGE_TARGET {
		TARGET_PARENT,
		TARGET_CHILDREN,
		TARGET_ANYONE,
	};
	
	public static final String MESSAGE_TOKEN = "::__::";
	public static final String MESSAGE_SPLITTER 	= ";;";
	
	private MESSAGE_TYPE msgtype;
	private MESSAGE_TARGET msgtarget;
	private Enum<?> msgdetail;
	
	private Object value;
	
	public Message() {}
	
	public Message(MESSAGE_TYPE msgtype, Enum<?> specific_type, MESSAGE_TARGET msgtarget) {
		this.setMsgtype(msgtype);
		this.setMsgtarget(msgtarget);
		this.setValue(null);
		this.setMsgdetail(specific_type);
	}
	
	public Message(MESSAGE_TYPE msgtype, Enum<?> specific_type, MESSAGE_TARGET msgtarget, String[] value) {
		this.setMsgtype(msgtype);
		this.setMsgtarget(msgtarget);
		this.setValue(value);
		this.setMsgdetail(specific_type);
	}

	public MESSAGE_TYPE getMsgtype() {
		return msgtype;
	}

	public void setMsgtype(MESSAGE_TYPE msgtype) {
		this.msgtype = msgtype;
	}

	public MESSAGE_TARGET getMsgtarget() {
		return msgtarget;
	}

	public void setMsgtarget(MESSAGE_TARGET msgtarget) {
		this.msgtarget = msgtarget;
	}

	public Enum<?> getMsgdetail() {
		return msgdetail;
	}

	public void setMsgdetail(Enum<?> msgdetail) {
		this.msgdetail = msgdetail;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	// 메세지가 서로 같은지 비교한다.
	// 서로 같으면 true, 다르면 false를 리턴한다.
	public boolean compareTo(Message prevMsg) {
		if (prevMsg.getMsgtype() == getMsgtype()) {
			if (prevMsg.getMsgdetail() == getMsgdetail()) {
				if (prevMsg.getValue() == prevMsg.getValue()) {
					return true;
				}
			}
		}
		return false;
	}
}
