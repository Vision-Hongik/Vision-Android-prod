package org.tensorflow.demo.vision_module;

import org.tensorflow.demo.Classifier;

import java.util.ArrayList;
import java.util.Hashtable;

public class InstanceHashTable extends Hashtable<Integer, ArrayList<Classifier.Recognition>> {


    public InstanceHashTable(){
        super();
    }

    public void putRecog(Classifier.Recognition recognition){
        if(!this.containsKey(recognition.getIdx())) this.put(recognition.getIdx(),new ArrayList<Classifier.Recognition>());
        this.get(recognition.getIdx()).add(recognition);
    }
}
