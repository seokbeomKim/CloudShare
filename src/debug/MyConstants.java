package debug;

/*
 * Constants
 * 
 * 프로그램 내에서 사용되는 상수에 대해 정의해놓은 클래스이다. 시스템 종료 시 값을 정의하기 위해
 * 클래스를 만들었다.
 */
public class MyConstants {
	// 에러 코드
	public static final int NEED_TO_RUN_VIRTUALBOX	= 2;
	public static final int NULL_CLIENT_SOCKET_EXCEPTION = 3;
	public static final int NO_CLIENT_AVAILABLE = 4;
	public static final int NO_NDRIVE_MNTPOINT = 5;

	// 기타 설정 관련
	public static final int MAXIMUM_DIVISION_CNT = 5;		// 최대 파일 분할 횟수
	public static final int FILE_BUFFER_SIZE = 8192;
}
