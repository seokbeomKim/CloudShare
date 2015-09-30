package util;

import java.io.File;

import debug.Debug;
import disk.DiskInfo;
import operation.OperationManager;
import server.ExternalService;

/*
 * DiskOpener
 * 
 * 생성된 디스크 파일을 여는 데 사용되는 클래스 
 * 디스크 파일을 여는 경우는 두 가지가 존재한다. 1. 현재 CloudShare 서비스가 실행 중인 상태와
 * 2. CloudShare 서비스가 실행 중이지 않은 상태가 존재한다. 
 * 먼저 관련 데몬이 실행 중인지 확인하고 실행 중이지 않으면 FileOpen을 하여 서비스 실행, 
 * 실행 중이라면 프로그램을 종료한다.
 */
public class DiskOpener {
	private final String TAG = "DiskOpener";
	private String disk_path;
	
	/*
	 * diskOpen: 쓰레드 초기화 및 실행
	 * 
	 *  서버의 주 구성인 OperationManager(메인쓰레드)와 External Service를 실행한다.
	 * (디스크파일을 여는 행위 자체가 프로그램 실행과 동일개념이라고 판단하였다.)
	 */
	public boolean diskOpen(String disk_path) {
		// disk_path 는 상대 경로로 전달되었기 때문에 이를 절대 경로로 바꾸어 저장한다.
		String env_path = getPathWithEnv(disk_path);
		File f = new File(env_path);
		
		// Argument로 받은 경로에 있는 디스크 파일을 체크하고 쓰레드 실행
		if (f.exists()) {
			Debug.print(TAG, "diskOpen", "Succeed to check file existance at " + env_path);
			
			// 파일이 존재하는 경우에는 파일의 내용을 통해서 환경변수를 설정한다. 			
			setDisk_path(f.getAbsolutePath());
			
			// ShutdownHooker 실행
			// (종료 이벤트 처리용)
//			Runtime.getRuntime().addShutdownHook(new ShutdownHooker());
			
			// OperationManager를 통해 OpenDisk를 호출한다.
			// OperationManager가 메인 쓰레드로 실행되고 external service는 
			// 다른 곳에서 실행되어야 한다.
			OperationManager.OpenDisk(env_path);
			
			// ExternalService를 실행한다.
			// 여기서는 ExternalService 객체 초기화와 관련 쓰레드를 실행한다.
			ExternalService.startService();
		
			// 메인 쓰레드 실행 (FUSE-mounter와의 IPC 통신)
			OperationManager.startOperator();
			
			// Fuse-mounter 실행 
//			OperationManager.mount();

			return true;
		}
		else {
			System.err.println("Failed to open file at " + env_path);
			return false;
		}
	}

	/*
	 * 파일 경로의 특별 문자(i.e. ~)들을 처리한다.
	 */
	private String getPathWithEnv(String disk_path) {
		Debug.print(TAG, "getPathWithEnv", "disk_path = " + disk_path);
		
		String rValue = disk_path;
		rValue = rValue.replaceAll("~", System.getenv("PWD"));
		
		Debug.print(TAG, "getPathWithEnv", "return value = " + rValue);
		return rValue;
	}
	/**
	 * @return the disk_path
	 */
	public String getDisk_path() {
		return disk_path;
	}

	/**
	 * @param disk_path the disk_path to set
	 */
	public void setDisk_path(String disk_path) {
		this.disk_path = disk_path;
		DiskInfo.getInstance().setDiskpath(disk_path);
	}
}
