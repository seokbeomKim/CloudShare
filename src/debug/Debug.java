package debug;

import org.apache.commons.lang3.StringUtils;

/*
 * Debug class
 * 	디버그 메세지 처리를 위한 클래스
 */
public class Debug {
	private static Debug instance;
	private boolean enabled;
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	public static boolean enabled() {
		return getInstance().isEnabled();
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	private Debug() 
	{
		setEnabled(true);
	}

	static public Debug getInstance() {
		if (instance == null) {
			instance = new Debug();
		}
		return instance;
	}

	static public void print(String class_name, String func_name, String message) {
		if (getInstance().isEnabled()) {
			String str = class_name+":"+func_name;
			str = StringUtils.abbreviate(str, 40);
			
			String padded = StringUtils.rightPad(str, 40, " ");
			
			System.out.println(padded + " >> " + message);
		}
	}
	
	static public void error(String class_name, String func_name, String message) {
		if (getInstance().isEnabled()) {
			String str = class_name+":"+func_name;
			str = StringUtils.abbreviate(str, 40);
			
			String padded = StringUtils.rightPad(str, 40, " ");
			
			System.err.println(padded + " >> " + message);
		}
	}
}
