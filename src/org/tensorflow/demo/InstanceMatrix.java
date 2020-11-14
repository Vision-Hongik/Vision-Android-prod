package org.tensorflow.demo;

import org.tensorflow.demo.Classifier.Recognition;

import java.util.ArrayList;
import java.util.Hashtable;

public class InstanceMatrix extends ArrayList<Hashtable<Integer,Integer>> {

    private int row;
    private int col;


    public InstanceMatrix(){
        super();
    }

    public void initMat(int row, int col){
        this.ensureCapacity(row*col);

        for(int i=0; i < row*col; i++)
            this.add(new Hashtable<Integer,Integer>());

        this.row = row;
        this.col = col;
    }
    public int getRow(){return this.row;}
    public int getCol(){return this.col;}

    public Hashtable<Integer,Integer> getPart_from_MatIdx(int row,int col){
        return this.get(row*this.row + col);
    }

    public void instanceClear(){
        for(int i=0; i< this.row*this.col; i++){
            this.get(i).clear();
        }
    }

    public void putRecog(Classifier.Recognition recognition){
        int key = recognition.getIdx();
        Classifier.Recognition.MatIdx matIdx = recognition.getMatIdx(this.row,this.col);
        int flat_matIdx = matIdx.rowIdx * row + matIdx.colIdx;

        if(this.get(flat_matIdx).containsKey(key))
            this.get(flat_matIdx).put(key,this.get(flat_matIdx).get(key)+1);
        else
            this.get(flat_matIdx).put(key,1);

    }

    public void announceInstance(){
        for(int row =0; row < this.row; row++){
            for(int col = 0; col < this.col; col++){

            }
        }

    }


}
