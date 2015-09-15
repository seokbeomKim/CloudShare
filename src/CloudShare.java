import util.CmdParser;

/*
 * CloudShare
 * 	CloudShare의 메인 클래스
 * 
 * author: Sukbeom Kim (chaoxifer@gmail.com)
 */

public class CloudShare {
	// 디버그를 위한 변수
	@SuppressWarnings("unused")
	private final String TAG = "CloudShare";
	
	public static void main(String[] args) {		
		CmdParser parser = new CmdParser(args);
		parser.doParse();
	}
}
