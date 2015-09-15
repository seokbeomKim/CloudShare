package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

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
		// FAILED
		return null;
	}
}
