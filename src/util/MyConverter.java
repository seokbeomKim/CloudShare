package util;

import java.util.LinkedList;

/*
 * MyConverter
 * 자료형 변환에 사용되는 클래스
 */
public class MyConverter {
	public static LinkedList<String> convertStrToList(String str) {
		LinkedList<String> r = new LinkedList<String>();
		str = str.replace("]", "");
		str = str.replace("[", "");
		
		String[] v = str.split(",");
		for (int i = 0; i < v.length; i++) {
			r.add(v[i].replaceAll("\\s",""));
		}
		return r;
	}
}
