package org.tensorflow.demo.vision_module;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Service {

    private double longitude;
    private double latitude;
    private String source_Station;
    private String source_Exit;
    private String dest_Station;
    private String dest_Exit;
    private float azimuth;
    private Sector current_Sector;
    private int sectorArraySize;
    private boolean readyFlag;

    //private jsonObject Array
    private ArrayList<Sector> sectorArrayList;
    //instances data structure class;
    private ArrayList<Sector> path;

    public Service(){
        this.sectorArrayList = new ArrayList<Sector>();
        this.path = new ArrayList<Sector>();
    }

    public Service(String source_Station, String source_Exit, String dest_Station, String dest_Exit){
        // if 출발지점이 null 이라면, 알아서 계산한다.
        this.source_Station = source_Station;
        this.source_Exit = source_Exit;
        this.dest_Station = dest_Station;
        this.dest_Exit = dest_Exit;
        this.sectorArrayList = new ArrayList<Sector>();
    }

    public void startService(){

        // voice 출발지  -> 모르면 GPS 찾기  -> GPS랑 mapdata를 사용해서 찾는 함수 구현. 상수
        // 몇번출구? -> 2번
        // voice 도착 지점 -> 광흥창
        // 광흥창 몇번출구? -> 2번

        // 출발...

        // GPS.get()                                     *
        // &                                             *
        // Yolo instance.get() -> dot 다 잡아내면,      *****  -> dot,stair x-> 비상  & board 찾기,  Sector 찾기 힌트,, 개찰구, 계단, 장애물 ->  Text 2 Speech
        // -> 똑같은 인스턴스 여러번잡을떄도 같은 거라고 판단 할줄 알아야돼.
        // &
        // Mapdata.get()   -> 4군데 ->>> 갈림 2개랑, gate, 지하철 근처 갈림길, **지하철 타는 곳**   <이전-섹터>  가정 절대 길을 잃어버리지 않았다.


        // 정리1  (섹터)
        // 섹터 찾기전 -> GPS & 이전 섹터 버퍼 활용 (출구도 섹터 간단히 만들자)  ||
        // 섹터 찾음 ->  ex)삼거리, 도트에 붙어있는 라인 블럭 Y좌표 차이로 구분.  ||

        // 정리2 (섹터x)
        // 계단, 에스컬레이터, 보드 OCR, 장애물, 타는 곳 ,나가는곳 instance랑 OCR로 정보 주면 시각장애인이 알아서 판단해서 걸어가.

        // 방향물어보면 자이로. 최후의 수단 교수님이 욕을하면,,,,
    }



    // 경로 설정
    public void setPath(){
//        int Source_Sector_Idx = Integer.parseInt(source_Exit);
//        int Dest_Sector_Idx;
//        // 다른역으로 간다면 탑승장 Sector번호까지 목적지로 설정
//        if(this.getSource_Station() != this.getDest_Station())
//            Dest_Sector_Idx = 10;
//        else
//            Dest_Sector_Idx = Integer.parseInt(this.getDest_Exit());

        //this.sectorArrayList

        // 소현이가 구현하기 전까지 스태틱으로 하겠슴니더..
        this.path.add(this.getMapdataFromIdx(2) ); // 시작 출구
        this.path.add(this.getMapdataFromIdx(5) );
        this.path.add(this.getMapdataFromIdx(7) );
        this.path.add(this.getMapdataFromIdx(8) );
        this.path.add(this.getMapdataFromIdx(9) );
        this.path.add(this.getMapdataFromIdx(10) ); // 탑승장

        this.setCurrent_Sector(1); // 현재 Sector를 시작 출구 다음 Sector로 지정 ex) 5번 Sector
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public void setSource_Station(String source_Station){ this.source_Station = source_Station; }

    public void setSource_Exit(String source_Exit){ this.source_Exit = source_Exit; }

    public void setDest_Station(String dest_Station){ this.dest_Station = dest_Station; }

    public void setDest_Exit(String dest_Exit){ this.dest_Exit= dest_Exit; }

    public void setAzimuth(float azimuth) {this.azimuth = azimuth;}

    public void setCurrent_Sector(int number) { this.current_Sector = this.path.get(number); }

    public boolean setCurrent_Sector_Next(){
        // 현재 Sector의 Index 찾기
        int idx = this.path.indexOf(getCurrent_Sector());

        // 다음 Sector가 마지막 Sector인 경우 true 반환
        if(idx + 1 == this.path.size() - 1) { return true; }

        this.setCurrent_Sector(idx + 1);
        return false;
    }

    public double getLongitude(){
        return this.longitude;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public String getSource_Station() {return this.source_Station;}

    public String getSource_Exit() {return this.source_Exit;}

    public String getDest_Station() {return this.dest_Station;}

    public String getDest_Exit() {return this.dest_Exit;}

    public float getAzimuth() { return this.azimuth;}

    public Sector getCurrent_Sector() { return this.current_Sector; }

    public void setSectorArrayList(ArrayList<Sector> mapList){
        // 정렬 한 뒤에 넣는다.
        Collections.sort(mapList, new Comparator<Sector>() {
            @Override
            public int compare(Sector s1, Sector s2) {
                if (s1.getIndex() < s2.getIndex()) {
                    return -1;
                } else if (s1.getIndex() > s2.getIndex()) {
                    return 1;
                }
                return 0;
            }
        });

        this.sectorArrayList = mapList;
        this.sectorArraySize = this.sectorArrayList.size();
    }

    public ArrayList<Sector> getPath() { return this.path; }

    public int getSectorArrayListSize() { return this.sectorArraySize; }

    public void push_backMapdata(Sector md){
        this.sectorArrayList.add(md);
    }

    // 얕 복사본을 넘겨준다.은 .. -> Mapdata 클래스의 깊은 복사자를 만들어야되는데 귀찮다..ㅜ
    public ArrayList<Sector> getSectorArrayList() {
        return this.sectorArrayList;
    }

    public Sector getMapdataFromIdx(int index){
        // DB는 1부터 10까지 sectorArrayList는 0부터 9까지이기에 index - 1을 해줌.
        // ex) 2번 Sector를 불러오고 싶으면 sectorArrayList의 1번 Index에서 찾아야 함.
        return this.sectorArrayList.get(index - 1);
    }

    public void setReadyFlag(boolean flag) {this.readyFlag = flag;}

    public boolean isReady(){
        if(this.sectorArrayList.isEmpty()){
            Log.e("service", "isReady: Sector Array is Empty" );
            return false;
        }

        return this.readyFlag;
    }

    // 두 섹터의 13개 인스턴스 일치율 반환
    public int comp(Sector sec1, Sector sec2){
        int num = 0;
        if(sec1.getDot() == sec2.getDot()) num++;
        if(sec1.getLine() == sec2.getLine()) num++;
        if(sec1.getUpEscalator() == sec2.getUpEscalator()) num++;
        if(sec1.getDownEscalator() == sec2.getDownEscalator()) num++;
        if(sec1.getUpStair() == sec2.getUpStair()) num++;
        if(sec1.getDownStair() == sec2.getDownStair()) num++;
        if(sec1.getPillar() == sec2.getPillar()) num++;
        if(sec1.getBoard() == sec2.getBoard()) num++;
        if(sec1.getUpBoard() == sec2.getUpBoard()) num++;
        if(sec1.getInSign() == sec2.getInSign()) num++;
        if(sec1.getOutSign() == sec2.getOutSign()) num++;
        if(sec1.getSign() == sec2.getSign()) num++;
        if(sec1.getGate() == sec2.getGate()) num++;
        return num;
    }

}




