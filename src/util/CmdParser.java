package util;
/*
 * CmdParser
 * 프로그램 실행시 사용자가 입력한 Argument에 따라서 명령을 처리한다.
 */
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import debug.Debug;

/*
 * 사용자로부터 받은 커맨드 처리 클래스 
 */
public class CmdParser {
	@SuppressWarnings("unused")
	private final String TAG = "CmdParser";
	private String[] args;
	public CmdParser(String args[]) {
		this.args = args;
	} 
	
	public void doParse() {
		Options opt = new Options();
		CmdLineParser parser = new CmdLineParser(opt);
		try {
			parser.parseArgument(args);
			opt.handle();
		} catch (CmdLineException e) {
			// Argument를 찾지 못해 실패한 경우에 도움말 메세지를 표시한다.
		    System.err.println("CloudShare [disk file]\n" 
		    		+ "\nOptions\n" 
		    		+ "-g:\tInitialize new disk and generate disk file.\n"
		    		+ "-l:\tShow licenses\n");
		    return;
		}
	}
	
	/*
	 * 옵션 정의 및 각 옵션에 대해서 어떻게 처리할 것인지 정의한다.
	 */
	private class Options {
		private final String TAG = "Options";
		
		@Option(name="-g", usage="CloudShare -g")
	    private boolean doGenerate = false;
	    @Option(name="-l", usage="CloudShare -l")
	    private boolean showLicenses = false;
	    
	    @Argument
	    private List<String> arguments = new ArrayList<String>();
	 
	    @SuppressWarnings("deprecation")
		public void handle() throws CmdLineException {
	    	if (doGenerate) {
	    		Debug.print(TAG, "handle", "Generating disk file.");
	    		DiskGenerator diskGen = new DiskGenerator();
	    		diskGen.openHelper();
	        }
	    	
	    	else if (showLicenses) {
	    		Debug.print(TAG, "handle", "Show licenses");
	    	}

	    	else {
	    		Debug.print(TAG, "handle", "Open disk file..");
	    		Debug.print(TAG,  "handle", "arguments = " + arguments.size());
	    		// Argument 체크. 아무 것도 입력하지 않았다면 error
	    		if (arguments.size() == 0) {
	    			throw new CmdLineException("arguments is empty");
	    		}
	    		
	    		// 파일 생성하는 경우가 아니면서 Argument가 존재한다면, 디스크 파일 열기로 처리한다.
	    		DiskOpener opener = new DiskOpener(); 
	    		if (!opener.diskOpen(arguments.get(0))) {
	    			// 파일 열기가 실패했을 경우 
	    			System.err.println("File does not exist: " + arguments.get(0));
	    			return;
	    		}
	    	}
	    }
	}
}
