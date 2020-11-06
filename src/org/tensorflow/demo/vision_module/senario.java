package org.tensorflow.demo.vision_module;

import java.util.ArrayList;
import java.util.Arrays;

public class senario {

    public static final String startStationString = "어디 역에서 출발 하시나요?";
    public static final String startExitString = "몇번 출구에서 출발 하시나요?";
    public static final String destStationString = "어디 역으로 가시나요?";
    public static final String destExitString= "몇번 출구로 나가시나요?";


    static final ArrayList<String> senarioArray = new ArrayList<String>(Arrays.asList(senario.startStationString,senario.startExitString,senario.destStationString,senario.destExitString));

    static final public String getI(int i){
        return senarioArray.get(i);
    }


}
