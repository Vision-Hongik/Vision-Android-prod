/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.demo;

import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.demo.vision_module.Voice;

import java.util.List;

/**
 * Generic interface for interacting with different recognition engines.
 */
public interface Classifier {
  /**
   * An immutable result returned by a Classifier describing what was recognized.
   */
  public class Recognition {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private String id;

    /**
     * Display index for the recognition.
     */
    private final int idx;

    /**
     * Display name for the recognition.
     */
    private final String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private final Float confidence;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;

    private float bitmapWidth;
    private float bitmapHeight;
    private int count;
    private boolean announced;
    private int timeStamp;
    private boolean inherited;

    public Recognition(
        final String id, final int idx, final String title, final Float confidence, final RectF location,float bitmapWidth,float bitmapHeight) {
      this.id = id;                   /** 고유 offset */
      this.idx = idx;                 /** class num */
      this.title = title;             /** class name */
      this.confidence = confidence;   /** 정확도 0~1 */
      this.location = location;       /** leftTop, rightBottom 좌표 */
      this.count = 0;
      announced = false;
      timeStamp = 0;
      this.bitmapWidth = bitmapWidth;
      this.bitmapHeight = bitmapHeight;
      this.inherited = false;
    }

    public String getId() { return id; }

    public void setId(String id){this.id = id;}

    public int getIdx() { return idx; }

    public String getTitle() { return title; }

    public Float getConfidence() { return confidence; }

    public RectF getLocation() { return new RectF(location); }

    public void setLocation(RectF location) { this.location = location; }

    public int getCount() { return count; }

    public void setCount(int count) { this.count = count; }

    public int getTimeStamp() {
      return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
      this.timeStamp = timeStamp;
    }

    public boolean isAnnounced() {
      return announced;
    }

    public void setAnnounced(boolean announced) {
      this.announced = announced;
    }

    public void setInherited(boolean inherited) { this.inherited = inherited; }

    public boolean isInherited() { return inherited; }

    public Recognition clone(){
      Recognition copyRecog = new Recognition(this.id, this.idx, this.title, this.confidence, this.location,this.bitmapWidth,this.bitmapHeight);
      RectF coRect = new RectF(this.location.left,this.location.top,this.location.right,this.location.bottom);
      copyRecog.setLocation(coRect);
      return copyRecog;
    }

    @Override
    public String toString() {
      String resultString = "";
      if (id != null) {
        resultString += "[" + id + "] ";
      }

      if (title != null) {
        resultString += idx + " ";
      }

      if (title != null) {
        resultString += title + " ";
      }

      if (confidence != null) {
        resultString += String.format("(%.1f%%) ", confidence * 100.0f);
      }

      if (location != null) {
        resultString += location + " ";
      }

      resultString += "개수: " + count + " ";

      return resultString.trim();
    }

    public class MatIdx{
      int rowIdx;
      int colIdx;
      int rowDim;
      int colDim;
      MatIdx(int rowIdx,int colIdx, int rowDim, int colDim){
        this.rowIdx = rowIdx;
        this.colIdx = colIdx;
        this.rowDim = rowDim;
        this.colDim = colDim;
      }
    }

    //public ArrayList<>

    public MatIdx getMatIdx(int rowDim, int colDim){
      int rowIdx = (int) (this.location.centerX() / (this.bitmapWidth / rowDim));
      int colIdx = (int) (this.location.centerY() / (this.bitmapHeight / colDim));
      return new MatIdx(rowIdx,colIdx,rowDim,colDim);
    }

    public void Announce(Voice voice){
      switch (this.idx){
        case 0:
        case 1: voice.TTS("경로를 잘 따라가는 중입니다.");
                break;

        case 2: if(this.count >= 3 )
                  voice.TTS("전방에 사람이 혼잡합니다.");
                break;

        case 3:
          break;

        case 4:
          voice.TTS("내려가는 계단이 계속 진행 중입니다.");
          break;

        case 5:
          break;

        case 6:
          break;

        case 7:
          voice.TTS("전방에 기둥이 있습니다.");
          break;
      }
      this.announced = true;
    }

  }

  List<Recognition> recognizeImage(Bitmap bitmap);

  void enableStatLogging(final boolean debug);

  String getStatString();

  void close();
}
