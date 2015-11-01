package util;

import operation.OperationManager;

/*
 * 프로그램 종료시 - 셧다운 이벤트가 발생하면 ShutDownHooker의 run 메서드가 실행된다.
 * 정상적으로 종료 전, 연결되어 있는 클라이언트들에게 종료 사실을 알린다.
 */
public class ShutdownHooker extends Thread {
	@SuppressWarnings("unused")
	private final String TAG = "ShutdownHooker";
	private static ShutdownHooker instance = null;
	public static ShutdownHooker getInstance() {
		if (instance == null) {
			instance = new ShutdownHooker();
		}
		return instance;
	}

	public void run() {
		System.out.println("Preparing exit the program...");
		OperationManager.getInstance().closeFUSEProgram();
//		ExternalService.brodcastProgramExit();
	}
	
	public static void startService() {
		getInstance().start();
	}
}
