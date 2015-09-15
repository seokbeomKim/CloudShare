package disk;

import java.util.LinkedList;
import java.util.List;

/*
 * 디스크의 정보 관리 클래스
 */
public class DiskInfo {
	private String owner;
	private String diskname;
	private String diskid;
	private String diskip;
	private List<String> clients;
	private String capacity;	// MB 단위
	
	@SuppressWarnings("unused")
	private final String TAG = "DiskInfo";
	private static DiskInfo instance;
	public static DiskInfo getInstance() {
		if (instance == null) {
			instance = new DiskInfo();
		}
		return instance;
	}

	private DiskInfo() {
		setCapacity("100");
		clients = new LinkedList<String>();
	}
	
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getDiskname() {
		return diskname;
	}

	public void setDiskname(String diskname) {
		this.diskname = diskname;
	}

	public String getDiskid() {
		return diskid;
	}

	public void setDiskid(String diskid) {
		this.diskid = diskid;
	}

	public String getCapacity() {
		return capacity;
	}

	public void setCapacity(String capacity) {
		this.capacity = capacity;
	}

	public List<String> getClients() {
		return clients;
	}

	public void setClients(List<String> clients) {
		this.clients = clients;
	}

	public String getDiskip() {
		return diskip;
	}

	public void setDiskip(String diskip) {
		this.diskip = diskip;
	}
}
