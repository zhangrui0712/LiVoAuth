package idpservice;

import java.util.ArrayList;
import java.util.Collections;

public class ChallengeCode {

    public static final String[] challengeCode = new String[]{
            "0", "1", "2","3", "4", "5","6","7", "8", "9",
            "A", "B",  "C", "D", "E",
//            "F", "G", "H", 
//            "I", "J", "K", "L", "M", "N", "O", "P", "Q", 
//            "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    public static int getCount() {

        return challengeCode.length;
    }


    public static String[] generateRandomChallenageCode(int count) {
    	if(count>challengeCode.length)
    		count = challengeCode.length;
    
    	ArrayList<String> code = new ArrayList<String>();
    	for(String s:challengeCode){
    		code.add(s);
    	}
    	Collections.shuffle(code);
    	
    	String codeArray[]=new String[count];
    	for(int i=0;i<count;i++){
    		codeArray[i]=code.get(i);
    	}
    	
    	return codeArray;
    	
    }

    public static String[] generateRegChallenageCode() {
        return challengeCode;
    }
}
