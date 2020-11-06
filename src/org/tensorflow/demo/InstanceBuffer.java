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
    public InstanceBuffer(int row, int col){
        super(row*col);
        this.row = row;
        this.col = col;
    }
    public int getRow(){return this.row;}
    public int getCol(){return this.col;}

    public Hashtable<Integer,Recognition> getMatIdx(int row,int col){
        return this.get(row*this.row + col);
    }


}
