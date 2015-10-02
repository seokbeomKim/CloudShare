package message;

/*
 * FUSE-Mounter와 Operator간 소켓 통신에 사용되는 IPC용 메세지
 */
public class IPCMessage {
	/*
	 * 메세지 타입 정의
	 */
	public static final String REQUEST_REFRESH	= "request_refresh";
	public static final String ACK_REFRESH		= "ack_refresh";
	public static final String REQUEST_FILELIST	= "request_FileList";
	public static final String ACK_FILELIST		= "ack_FileList";
	public static final String REQUEST_UPLOAD		= "request_Upload";
	public static final String ACK_UPLOAD			= "ack_Upload";
	public static final String REQUEST_DOWNLOAD	= "request_Download";
	public static final String ACK_DOWNLOAD		= "ack_Download";
	public static final String REQUEST_CHECK_CONNECTION	= "request_checkconnection";
	public static final String ACK_CHECK_CONNECTION		= "ack_checkconnection";
	public static final String ACK_VALUE					= "ack_value";
	public static final String NOTHING					= "nothing";
	
	/*
	 * 메세지 구성
	 * 종류(type)와 값(value)으로 구성된다.
	 */
	private String type;
	private String value;
		

	public static String findAppropriateACKFromRequest(String request_type) {
		String ACK_type = null;
		if (request_type.compareTo(REQUEST_CHECK_CONNECTION) == 0) {
			ACK_type = ACK_CHECK_CONNECTION;
		}
		else if (request_type.compareTo(REQUEST_DOWNLOAD) == 0) {
			ACK_type = ACK_DOWNLOAD;
		}
		else if (request_type.compareTo(REQUEST_FILELIST) == 0) {
			ACK_type = ACK_FILELIST;
		}
		else if (request_type.compareTo(REQUEST_REFRESH) == 0) {
			ACK_type = ACK_REFRESH;
		}
		else if (request_type.compareTo(REQUEST_UPLOAD) == 0) {
			ACK_type = ACK_UPLOAD;
		}
		else {
			ACK_type = NOTHING;
		}
		return ACK_type;
	}
	
	/*
	 * Constructors
	 */
	public IPCMessage() {
		this.type = NOTHING;
		this.value = null;
	}
	
	public IPCMessage(String type, String value) {
		this.type = type;
		this.value = value;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
