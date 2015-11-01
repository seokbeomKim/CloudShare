import util.CmdParser;

/*
 * CloudShare
 * 	CloudShare의 메인 클래스
 * 
 * author: Sukbeom Kim (chaoxifer@gmail.com)
 */

public class CloudShare {
	public static void main(String[] args) {		
		CmdParser parser = new CmdParser(args);
		parser.doParse();
	}
}
