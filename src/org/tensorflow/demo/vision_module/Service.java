package org.tensorflow.demo.vision_module;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class Service {
    private double longitude;
    private double latitude;
    private String source_Station;
    private String source_Exit;
    private String dest_Station;
    private String dest_Exit;
    private String way;
    private String nextWay;
    private float azimuth;
    private int sectorArraySize;
    private boolean readyFlag;
    private int matchingFlag;
    private int userSectorNum;
    public int score;
    public int idx;
    private int cur_Idx;
    // 사용자가 현재 찾아갈 섹터
    private Sector current_Sector;
    //private jsonObject Array
    private ArrayList<Sector> sectorArrayList;
    //instances data structure class;
    private ArrayList<Sector> path;

    public Service(){
        this.sectorArrayList = new ArrayList<Sector>();
        this.path = new ArrayList<Sector>();
        this.current_Sector = new Sector();
    }

    // 한 노드의 인접노드 array_list를 구하는 메소드
    public ArrayList searchAdjacentList(int sector_idx) throws JSONException {
        JSONArray adjacent_idx;
        ArrayList<Integer> adj_idx_list = new ArrayList<Integer> ();

        adjacent_idx = this.getSectorArrayList().get(sector_idx-1).getAdjacentIdx();
        for(int i =0; i < adjacent_idx.length(); i++) {
            int adjacentIdx = (int) adjacent_idx.get(i);
            adj_idx_list.add(adjacentIdx);
        }

        return adj_idx_list;
    }

    public void BFS_WithShortestPath(int src, int dst) throws JSONException {
        Log.e("setPath 시작", "src: " + src+",   dst: "+ dst);
        /* 필요한 변수 선언부 */
        int v=src, node=0;
        int[] visited= new int[10];  // 노드 방문 여부를 표현하는 배열 (실제 인덱스는 idx-1)
                                     // 임의의 지하철역의 sector 갯수는 최대 10개로 제한, 초기값=0
        ArrayList adj_idx_list; // 인접 노드 검색을 위한 array list
        Queue<Integer> queue = new LinkedList<Integer>(); // bfs 전체 path를 위한 queue
        Stack<Integer> pathStack = new Stack<Integer>(); // 최단거리 구하기 위한 stack

        /* BFS 시작을 위한 초기화:
            자료구조에 출발지(src) push 및 visited=true로 표시 */
        queue.add(v);
        pathStack.add(v);
        visited[v-1] =1;

        /* BFS 실행 */
        while(queue.size() != 0) {
            v = queue.poll();
            adj_idx_list = searchAdjacentList(v);

            for(int i =0; i < adj_idx_list.size(); ++i) {
                node = (int) adj_idx_list.get(i);
                if(visited[node-1] == 0) {
                    visited[node-1] = 1;
                    queue.add(node);
                    pathStack.add(node);
                    Log.e("pathStack", "pathStack.add("+node+")");
                    if(v == dst) {
                        queue.add(dst);
                        break;
                    }
                }
            }
        }

        //예시결과로... BFS(src=1일때): 1 5 2 7 6 8 3 4 9 10

        /* 최단 경로 구하기:
            BFS 실행결과를 담은 stack 활용 */
        int pathNode = 0, currentSrc=dst; //별도의 변수 선언
        ArrayList<Integer> shortest_path_reversed = new ArrayList<Integer> ();
        int temp_idx= pathStack.search(dst);
//        Log.e("temp_idx", String.valueOf(temp_idx));
        for (int k=0; k < (temp_idx -1); k++) pathStack.pop();

        while(!pathStack.isEmpty()) { // 최단경로 구하기
            pathNode = pathStack.pop();
            if(searchAdjacentList(currentSrc).contains(pathNode)) {
                shortest_path_reversed.add(currentSrc);
                currentSrc = pathNode;
                if(pathNode == src){
                    shortest_path_reversed.add(pathNode);
                    break;
                }
            }
        }

        for (int j=0; j<(shortest_path_reversed.size()); j++) {
            int push_idx = shortest_path_reversed.get(shortest_path_reversed.size()-j-1);
            this.path.add(this.getMapdataFromIdx(push_idx));
        } //최종 path를 stack에서 꺼내 sector array에 add
    }


    // 경로 설정
    public void setPath(String src, String dst) throws JSONException {
        int srcSector = Integer.parseInt(src);
        int dstSector;

        if(this.getSource_Station() != this.getDest_Station()) // 다른역으로 간다면 탑승장 Sector번호까지 목적지로 설정
            dstSector = 10;
        else  dstSector = Integer.parseInt(dst);

        try {
             BFS_WithShortestPath(srcSector, dstSector);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
// 2 5 7 8 9 10
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

    public boolean setCurrentSectorToNext() throws JSONException {
        // 현재 Sector의 Index 찾기
        int idx = this.path.indexOf(getCurrent_Sector());
        // 현재 Sector가 마지막 Sector인 경우 true 반환
        if(idx == this.path.size() - 1) { return true; }

        // 현재 사용자의 위치에 있는 섹터
        Sector sec = new Sector(getCurrent_Sector());

        // 다음 섹터로 currentSector변경
        this.setCurrent_Sector(idx + 1);

        // 이전 섹터에서 다음 섹터로 방향을 지정
        for(int i =0; i < sec.getAdjacentIdx().length(); i++){
            int adjacentIdx = (int) sec.getAdjacentIdx().get(i);
            Log.e("way", "adjacentIdx, nextIdx ? " + adjacentIdx + ", " + getCurrent_Sector().getIndex() + ", way: " + sec.getAdjacentDir().get(i));
            if(adjacentIdx != getCurrent_Sector().getIndex()) continue;

            this.setWay((String)sec.getAdjacentDir().get(i));
            break;
        }

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

    public String getWay() { return way; }

    public String getNextWay() { return nextWay; }

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

    public int getMatchingFlag() { return matchingFlag; }

    public void setReadyFlag(boolean flag) {this.readyFlag = flag;}

    public void setWay(String way) { this.way = way; }

    public void setNextWay(String nextWay) { this.nextWay = nextWay; }

    public void setMatchingFlag(int matchingFlag) { this.matchingFlag = matchingFlag; }

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
    private int Score = 0;

    public void plusScore(int n){
        this.Score += n;
    }
    public void minusScore(int n){
        this.Score -= n;
    }

    // 두 섹터의 13개 인스턴스 일치율 반환
    public int compareInstance(Sector sec1, Sector sec2){
        this.Score = 0;
        Log.e("instance", this.Score + "");
        if(sec1.getUpEscalator() && sec2.getUpEscalator()) plusScore(4);
        else if(sec1.getUpEscalator() != sec2.getUpEscalator()) minusScore(4);
        Log.e("instance", this.Score + "");
        if(sec1.getDownEscalator() && sec2.getDownEscalator()) plusScore(4);
        else if(sec1.getDownEscalator() != sec2.getDownEscalator()) minusScore(4);
        Log.e("instance", this.Score + "");
        if(sec1.getUpStair() && sec2.getUpStair()) plusScore(4);
        else if(sec1.getUpStair() != sec2.getUpStair()) minusScore(4);
        Log.e("instance", this.Score + "");
        if(sec1.getDownStair() && sec2.getDownStair()) plusScore(4);
        else if(sec1.getDownStair() != sec2.getDownStair()) minusScore(4);
        Log.e("instance", this.Score + "");
        if(sec1.getPillar() && sec2.getPillar()) plusScore(4);
        else if(sec1.getPillar() != sec2.getPillar()) minusScore(4);
        Log.e("instance", this.Score + "");
        if(sec1.getBoard() && sec2.getBoard()) plusScore(3);
        else if(sec1.getBoard() != sec2.getBoard()) minusScore(3);
        Log.e("instance", this.Score + "");
        if(sec1.getUpBoard() && sec2.getUpBoard()) plusScore(2);
        else if(sec1.getUpBoard() != sec2.getUpBoard()) minusScore(2);
        Log.e("instance", this.Score + "");
        if(sec1.getInSign() && sec2.getInSign()) plusScore(4);
        else if(sec1.getInSign() != sec2.getInSign()) minusScore(7);
        Log.e("instance", this.Score + "");
        if(sec1.getOutSign() && sec2.getOutSign()) plusScore(3);
        else if(sec1.getOutSign() != sec2.getOutSign()) minusScore(3);
        Log.e("instance", this.Score + "");
        if(sec1.getSign() && sec2.getSign()) plusScore(1);
        else if(sec1.getSign() != sec2.getSign()) minusScore(1);
        Log.e("instance", this.Score + "");
        if(sec1.getGate() && sec2.getGate()) plusScore(4);
        else if(sec1.getGate() != sec2.getGate()) minusScore(4);
        Log.e("instance", this.Score + "");
        return Score;
    }

    public int getUserSectorNum() {
        return userSectorNum;
    }

    public void setUserSectorNum(int userSectorNum) {
        this.userSectorNum = userSectorNum;
    }

    public int getCur_Idx() {
        return cur_Idx;
    }

    public void setCur_Idx(int cur_Idx) {
        this.cur_Idx = cur_Idx;
    }
}


