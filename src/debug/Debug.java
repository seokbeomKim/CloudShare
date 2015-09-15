package debug;

/*
 * Debug class
 * 	디버그 메세지 처리를 위한 클래스
 */
public class Debug {
	private static Debug instance;
	private boolean enabled;
	
	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
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
			System.out.println("["+class_name+"]["+func_name+"] "+ message);
		}
	}
}
