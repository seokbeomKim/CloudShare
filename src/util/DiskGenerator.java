package util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import debug.Debug;
import message.Message.MESSAGE_TARGET;
import util.IpChecker;

/*
 * 디스크 파일 생성 도우미
 * 
 * 사용자로부터 이메ᅟ일 정보를 받아 디스크의 소유자로 등록하고
 * 디스크의 이름, 각 클라이언트들이 공유하는 공간의 크기, 디스크 id 설정 
 * >> 단 추후 기능으로 디스크 id를 인증하는 방식으로 구현하여 인증서버를 두어 id를 설정하는 방식
 *    구현할 것
 * 현재 클라이언트의 Public IP 주소와 연결된 클라이언트들의 IP 주소 (IPC 통해서 실행중인 
 * 서비스로부터 메세지를 받아와야한다.
 * 입력 받은 정보를 바탕으로 디스크 파일을 생성한다. 
 */
public class DiskGenerator {
	private final String TAG = "DiskGenerator";
	
	private String email;
	private String diskname;
	private String diskid;
	private String myip;
	private String[] client_ip;
	
	private JSONObject o;
	
	public void openHelper() {
		readUserInput();
		prepareJSON();
		generateFile();
	}

	private void generateFile() {
		final String pwd = System.getenv("PWD");
		try {
			String diskfile_path = pwd + File.separator + diskname + ".csd";
			Debug.print(TAG, "generateFile", "output file path = " + diskfile_path);
			FileWriter of = new FileWriter(diskfile_path);
			System.out.println("Generating disk file...");
			of.write(o.toString());
			System.out.println("Succeed to generate file at " + diskfile_path);
			
			of.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readUserInput() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		// 사용자 입력 부분
		try {
			// 사용자 이메일
			// TODO 이메일 인증 받는 것 나중에 구현 - 메일 무단 사용 금지를 위해서 
			System.out.println("Please input email address: ");
			setEmail(reader.readLine());
			
			// 디스크 이름
			System.out.println("Disk name: ");
			setDiskname(reader.readLine());
			
			// 디스크 id
			// TODO 디스크 id 인증하는 부분 구현 필요
			Debug.print(TAG, "openHelper", "Set disk id as 0 temperately.");
			System.out.println("Disk id: 0");
			setDiskid("0");
			
			// 사용자 IP주소
			setMyip(IpChecker.getPublicIP());
			System.out.println("Your Public IP: "+getMyip());
		} catch (IOException e) {
			System.err.println(e);
			return;
		}
	}

	private void prepareJSON() {
		// 입력 받은 데이터를 바탕으로 파일 출력 (JSON format)
		o = new JSONObject();
		o.put("email", email);
		o.put("diskname", diskname);
		o.put("diskid", diskid);
		o.put("ip", myip);
		
		JSONArray cl_list = new JSONArray();
		if (client_ip != null) {
			for (String cl : client_ip) {
				cl_list.put(cl);
			}
		}
		o.put("clients", cl_list);
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the diskname
	 */
	public String getDiskname() {
		return diskname;
	}

	/**
	 * @param diskname the diskname to set
	 */
	public void setDiskname(String diskname) {
		this.diskname = diskname;
	}

	/**
	 * @return the myip
	 */
	public String getMyip() {
		return myip;
	}

	/**
	 * @param myip the myip to set
	 */
	public void setMyip(String myip) {
		this.myip = myip;
	}

	/**
	 * @return the client_ip
	 */
	public String[] getClient_ip() {
		return client_ip;
	}

	/**
	 * @param client_ip the client_ip to set
	 */
	public void setClient_ip(String[] client_ip) {
		this.client_ip = client_ip;
	}

	/**
	 * @return the diskid
	 */
	public String getDiskid() {
		return diskid;
	}

	/**
	 * @param diskid the diskid to set
	 */
	public void setDiskid(String diskid) {
		this.diskid = diskid;
	}
}
