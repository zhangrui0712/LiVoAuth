package util;

public class Log {
	
	public static void v(String tag, String msg){
		System.out.println(tag + ": " + msg);
	}
	public static void e(String tag, String msg){
		System.err.println(tag + ": " + msg);
	}
}
