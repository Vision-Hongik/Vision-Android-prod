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
    private final String id;

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

    private int count;

    public Recognition(
        final String id, final int idx, final String title, final Float confidence, final RectF location) {
      this.id = id;                   /** 고유 offset */
      this.idx = idx;                 /** class num */
      this.title = title;             /** class name */
      this.confidence = confidence;   /** 정확도 0~1 */
      this.location = location;       /** leftTop, rightBottom 좌표 */
      this.count = 0;
    }

    public String getId() { return id; }

    public int getIdx() { return idx; }

    public String getTitle() { return title; }

    public Float getConfidence() { return confidence; }

    public RectF getLocation() { return new RectF(location); }

    public void setLocation(RectF location) { this.location = location; }

    public int getCount() { return count; }

    public void setCount(int count) { this.count = count; }

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
  }

  List<Recognition> recognizeImage(Bitmap bitmap);

  void enableStatLogging(final boolean debug);

  String getStatString();

  void close();
}
