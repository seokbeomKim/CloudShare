package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import debug.Debug;

/*
 * IpChecker
 * 자신의 Public IP를 확인하는 클래스이다.
 */
public class IpChecker {
	private static final String TAG = "IpChecker";
	
	/*
	 * getPublicIP() 
	 * ip checker사이트에 접속하여 해당 내용을 파싱한다. 
	 * 성공하면 IP주소를, 실패하면 NULL 반환
	 * 
	 * @return Public IP Address
	 */
	public static String getPublicIP() {
		if (!Debug.enabled()) {
			// 디버그 플래그가 꺼져있는 경우에는 public ip를 리턴한다.
			try {
				URL ipUrl = new URL("http://checkip.amazonaws.com");
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(ipUrl.openStream())
						);
				
				String ip = reader.readLine();
				return ip;
			}
			catch (MalformedURLException e) {
				// 사용자가 페이지에 접속하지 못한 경우
				Debug.print(TAG, "getPublicIP", "Failed to access page: http://checkip.amazonaws.com"
						+ " Please check your internet connection.");
				System.err.println(e);
			} catch (IOException e) {
				Debug.print(TAG, "getPublicIP", "Failed to read ip string.");
				System.err.println(e);
			}
		}
		else {
			// 디버그 시에는 로컬 IP를 리턴한다.
			// checkLocalIP를 통해 원하는 인터페이스의 IP를 얻어온다.
			String ip = checkLocalIP("vboxnet0");
			if (ip == null) {
				// 가상머신에서는 vboxnet0이 아니라 eth?으로 인터페이스가 명명되기 때문에 
				ip = checkLocalIP("eth1");
			}
//			Debug.print(TAG, "getPublicIP", "IP for debugging : " + ip);
			return ip;
		}
		// FAILED
		return null;
	}
	
	/*
	 * checkLocalIP
	 * 디버그를 위해 제공하는 메서드 - 현재 컴퓨터의 네트워크 인터페이스와 IP를 확인한다.
	 * 현재 개발 방법에서는 virtualbox 를 사용하므로 vboxnet0 인터페이스의 IP를 얻어온다.
	 * 이 메서드는 자신의 네트워크 인터페이스와 IP를 확인하고 어떻게 사용할 것인지 PublicIP를 다시 재구현하는 데 사용한다.  
	 */
	@SuppressWarnings("rawtypes")	// Enumeration에 대한 Warning 제거
	private static String checkLocalIP(String if_name) {
		try {
			Enumeration ex = null;
			try {
				ex = NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
			while(ex.hasMoreElements())
			{
			    NetworkInterface n = (NetworkInterface) ex.nextElement();	
			    Enumeration ee = n.getInetAddresses();

			    // 먼저 네트워크 인터페이스의 이름을 확인한다.
				if (if_name.compareTo(n.getName()) != 0) {
					// 해당 네트워크가 아닌 경우 다음 element로 
					continue;
				}
				
				// 확인 후에는 ip주소값을 얻어온다.
				@SuppressWarnings("unused")
				String name = n.getName(), ip = null;
				ee.nextElement();	// 첫번째 MAC 주소
			    while (ee.hasMoreElements())
			    {
			        InetAddress i = (InetAddress) ee.nextElement();
		        	ip = i.getHostAddress();
			    }
			    
//			    Debug.print(TAG, "checkLocalIP", "ip = "+ ip + ", name = " + name);
			    return ip;
			}
		} catch (Exception e) {
			Debug.print(TAG, "getPublicIP", "Failed to get local ip");
			System.err.println(e);
		}
		return null;
	}
}
