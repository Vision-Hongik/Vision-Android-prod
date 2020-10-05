package org.tensorflow.demo.vision_module;

import java.util.ArrayList;

public class Service {

    private double longitude;
    private double latitude;
    private String source_Station;
    private String source_Exit;
    private String dest_Station;
    private String dest_Exit;

    //private jsonObject Array
    private ArrayList<Sector> sectorArrayList;
    //instances data structure class;

    public Service(){
        this.sectorArrayList = new ArrayList<Sector>();
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

    public void setSectorArrayList(ArrayList<Sector> mapList){
        this.sectorArrayList = mapList;
    }

    public void push_backMapdata(Sector md){
        sectorArrayList.add(md);
    }

    // 얕 복사본을 넘겨준다.은 .. -> Mapdata 클래스의 깊은 복사자를 만들어야되는데 귀찮다..ㅜ
    public ArrayList<Sector> getSectorArrayList() {
        return this.sectorArrayList;
    }

    public Sector getMapdataFromIdx(int index){
        return this.sectorArrayList.get(index);
    }


}




