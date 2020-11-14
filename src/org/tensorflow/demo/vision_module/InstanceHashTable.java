package org.tensorflow.demo.vision_module;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.InstanceMatrix;

import java.util.ArrayList;
import java.util.Hashtable;

public class InstanceHashTable extends Hashtable<Integer, ArrayList<Classifier.Recognition>> {


    //private InstanceMatrix matrix;
    int matRowSize;
    int matColSize;
    public InstanceHashTable(int matRowSize,int matColSize){
        super();
        this.matRowSize = matRowSize;
        this.matColSize = matColSize;
        //this.matrix = new InstanceMatrix();
        //this.matrix.initMat(matRowSize,matColSize);
    }

    public void putRecog(Classifier.Recognition recognition){
        if(!this.containsKey(recognition.getIdx())) this.put(recognition.getIdx(),new ArrayList<Classifier.Recognition>());
        this.get(recognition.getIdx()).add(recognition);
        //this.matrix.putRecog(recognition);
    }

    //public InstanceMatrix getMatrix(){return this.matrix;}
}
