package org.tensorflow.demo.vision_module;

import android.icu.text.Edits;
import android.util.Log;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.TensorFlowYoloDetector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


public class InstanceTimeBuffer extends LinkedList<InstanceHashTable> {

    private int maxSize = 10;
    private float bitmapHeight;
    private float bitmapWidth;

    private int acumCount;
    public InstanceTimeBuffer(){
        super();
        this.bitmapHeight = 0;
        this.bitmapWidth = 0;
        this.acumCount = 0;
    }


    // 리스트에 추가시 항상 maxSize 유지.
    // 마지막 instanceHashTable은 마지막 것과 싱크를 맞춰서 삽입한다.
    @Override
    public boolean add(InstanceHashTable instanceHashTable) {
        if(this.size() == maxSize) this.removeFirst();
        if(!this.isEmpty()) syncInstanceBetweenPreNCur(this.getLast(),instanceHashTable);
        if(this.size() >= 2) syncInstanceBetweenPreNCur(this.get(this.size()-2),instanceHashTable);
        this.acumCount++;
        //Log.e("InstanceTimeBuffer", "add : 누적개수: "+this.acumCount + " 현재 개수 :" +(this.size()+1));
        return super.add(instanceHashTable);
    }

    // 이전 InstanceHashTable과 현재 InstanceTable을 동기화한다.(= 같은 instance인지 체크한다)
    public void syncInstanceBetweenPreNCur(InstanceHashTable preInstance, InstanceHashTable curInstance){
       Iterator it = curInstance.keySet().iterator();
       while(it.hasNext()){
           int key = (int)it.next();
           if(preInstance.containsKey(key)){
               ArrayList<Classifier.Recognition> curSameClassArray = curInstance.get(key);
               ArrayList<Classifier.Recognition> preSameClassArray = preInstance.get(key);
               boolean [] synced = new boolean[14];
               // 각 Recog별로 이전 Recog들과 위치를 비교한다.
               for(int i=0; i < curSameClassArray.size(); i++){
                  if( curSameClassArray.get(i).isInherited()) continue;
                   for(int j=0; j < preSameClassArray.size(); j++){

                       if(synced[j]) continue; //이미 짝지어짐

                        if(checkSameInstance(preSameClassArray.get(j),curSameClassArray.get(i))){
                            synced[j] = true; // 짝지음 표시
                            //이전 instance와 동일하다고 판단된다면, Announce, 고유ID, timeStamp를 +1 해서 상속한다
                            curSameClassArray.get(i).setAnnounced(preSameClassArray.get(j).isAnnounced());
                            curSameClassArray.get(i).setTimeStamp(preSameClassArray.get(j).getTimeStamp()+1);
                            curSameClassArray.get(i).setId(preSameClassArray.get(j).getId());
                            curSameClassArray.get(i).setInherited(true);
                        }
                   }
               }
           }
       }
    }

    // Instance간에 서로 같은 Instance인지 체크한다.
    public boolean checkSameInstance(Classifier.Recognition ins1,Classifier.Recognition ins2){
        if(ins1.getIdx() != ins2.getIdx()) return false; //같은 클래스가 아니면 거짓

        float rowDistance = Math.abs(ins1.getLocation().centerX() - ins2.getLocation().centerX());
        float colDistance = Math.abs(ins1.getLocation().centerY() - ins2.getLocation().centerY());
        // 전체 비트맵 크기에 비해서, 너무 멀다면 false.
        if(this.bitmapWidth == 0 || this.bitmapHeight == 0) {
            Log.e("InstanceTimebuffer", "checkSameInstance: bitmapSize is Zero!");
            return false;
        }

        Log.e("InstanceTimeBuffer", "checkSameInstance Differ: accumCount"+this.acumCount);
        Log.e("InstanceTimeBuffer", "checkSameInstance Differ: 실제 가로" + rowDistance +", 기준 가로"+ (this.bitmapWidth/4));
        Log.e("InstanceTimeBuffer", "checkSameInstance Differ: 실제 세로" + colDistance +", 기준 세로"+ (this.bitmapHeight/4));
        if(rowDistance > this.bitmapWidth/4) return false;
        if(colDistance > this.bitmapHeight/4) return false;

        Log.e("InstanceTimeBuffer", "checkSameInstance: True!!" + " "+ins2.getTitle() + " "+ins2.getTimeStamp());
        return true;
    }

    public ArrayList<Classifier.Recognition> getAnnouncealbeInstance(){
        ArrayList<Classifier.Recognition> announceableList = new ArrayList<>();
        return announceableList;
    }


    public int getMaxSize(){return this.maxSize;}
    public void setMaxSize(int ms) {this.maxSize = ms;}
    public float getBitmapWidth() { return bitmapWidth; }
    public void setBitmapWidth(float bitmapWidth) { this.bitmapWidth = bitmapWidth; }

    public float getBitmapHeight() { return bitmapHeight; }
    public void setBitmapHeight(float bitmapHeight) { this.bitmapHeight = bitmapHeight; }

    public int getAcumCount() { return acumCount; }
}
