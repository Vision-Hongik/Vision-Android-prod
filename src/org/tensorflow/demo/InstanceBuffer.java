package org.tensorflow.demo;

import org.tensorflow.demo.Classifier.Recognition;

import java.util.ArrayList;
import java.util.Hashtable;

public class InstanceBuffer  extends ArrayList<Hashtable<Integer, Recognition>> {

    private int row;
    private int col;


    public InstanceBuffer(){
        super();
    }

    public void initMat(int row, int col){
        this.ensureCapacity(row*col);

        for(int i=0; i < row*col; i++)
            this.add(new Hashtable<Integer,Recognition>());

        this.row = row;
        this.col = col;
    }
    public int getRow(){return this.row;}
    public int getCol(){return this.col;}

    public Hashtable<Integer,Recognition> getMatIdx(int row,int col){
        return this.get(row*this.row + col);
    }

    public void instanceClear(){
        for(int i=0; i< this.row*this.col; i++){
            this.get(i).clear();
        }
    }

    public void announceInstance(){
        for(int row =0; row < this.row; row++){
            for(int col = 0; col < this.col; col++){

            }
        }

    }


}
