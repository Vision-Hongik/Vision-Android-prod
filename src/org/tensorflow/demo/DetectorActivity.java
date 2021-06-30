/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import androidx.fragment.app.Fragment;
import android.annotation.TargetApi;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.icu.text.Edits;
import android.location.Location;
import android.location.LocationListener;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;
import org.tensorflow.demo.vision_module.Compass;
import org.tensorflow.demo.vision_module.InstanceHashTable;
import org.tensorflow.demo.vision_module.InstanceTimeBuffer;
import org.tensorflow.demo.vision_module.MyCallback;
import org.tensorflow.demo.vision_module.MapRequest;
import org.tensorflow.demo.vision_module.MyGps;
import org.tensorflow.demo.vision_module.OcrRequest;
import org.tensorflow.demo.vision_module.SOTWFormatter;
import org.tensorflow.demo.vision_module.Sector;
import org.tensorflow.demo.vision_module.Service;
import org.tensorflow.demo.vision_module.Voice;
import org.tensorflow.demo.vision_module.senario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity  implements OnImageAvailableListener {


  private static final Logger LOGGER = new Logger();

  // Configuration values for tiny-yolo-voc. Note that the graph is not included with TensorFlow and
  // must be manually placed in the assets/ directory by the user.
  // Graphs and models downloaded from http://pjreddie.com/darknet/yolo/ may be converted e.g. via
  // DarkFlow (https://github.com/thtrieu/darkflow). Sample command:
  // ./flow --model cfg/tiny-yolo-voc.cfg --load bin/tiny-yolo-voc.weights --savepb --verbalise

  private static final String YOLO_MODEL_FILE = "file:///android_asset/my-tiny-yolo.pb";
  private static final int YOLO_INPUT_SIZE = 416;
  private static final String YOLO_INPUT_NAME = "input";
  private static final String YOLO_OUTPUT_NAMES = "output";
  private static final int YOLO_BLOCK_SIZE = 32;

  private enum DetectorMode {
    YOLO;
  }
  private static final DetectorMode MODE = DetectorMode.YOLO;

  // Minimum detection confidence to track a detection.
  public static final float MINIMUM_CONFIDENCE_YOLO = 0.7f;

  private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.YOLO;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;

  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private long lastProcessingTimeMs1;
  private long lastDetectStartTime = 0;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private Bitmap cropSignBitmap = null;
  private float bitmapWidth = 0;
  private float bitmapHeight = 0;
  private int N = 5; // N * N 사분면

  private static final int BUFFERTIME = 3;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;
  private OverlayView trackingOverlay;

  private byte[] luminanceCopy;

  private BorderedText borderedText;

  private RequestQueue requestQueue;
  private LocationRequest locationRequest;
  private MyGps myGps;
  private Service service;
  private Voice voice;
  private Compass compass;
  private SOTWFormatter sotwFormatter;
  private Sector curSector = new Sector(false);
  private boolean dotFlag = false;
  private boolean yoloFirstStartFlag = false;

  public InstanceMatrix instanceMatrix = new InstanceMatrix();
  public InstanceTimeBuffer instanceTimeBuffer = new InstanceTimeBuffer();

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


    // 5 * 5 분면의 InstanceBuffer 초기화
    instanceMatrix.initMat(5,5);


    // GPS가 꺼져있다면 On Dialog
    createLocationRequest();
    turn_on_GPS_dialog();


    //Gps
    myGps = new MyGps(DetectorActivity.this,locationListener);
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
      @Override
      public void run() {
        myGps.startGps(DetectorActivity.this.service);
        Log.e("thread", "run: start");
      }
    },0);

    //Compass
    compass = new Compass(this);
    sotwFormatter = new SOTWFormatter(this); // 방향 포맷,,방위각 보고 N,NW ..써주는 친구..
    Compass.CompassListener cl = getCompassListener();
    compass.setListener(cl);

    // Voice
    voice = new Voice(this,null);

    // API Server
    requestQueue = Volley.newRequestQueue(DetectorActivity.this);  // 전송 큐

    // Service
    service = new Service();

  }



  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    detector = TensorFlowYoloDetector.create(
            getAssets(),
            YOLO_MODEL_FILE,
            YOLO_INPUT_SIZE,
            YOLO_INPUT_NAME,
            YOLO_OUTPUT_NAMES,
            YOLO_BLOCK_SIZE);

    int cropSize = YOLO_INPUT_SIZE;

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              //tracker.drawDebug(canvas);
            }
          }
        });

    addCallback(
            new DrawCallback() {
              @Override
              public void drawCallback(final Canvas canvas) {
                if (!isDebug()) {
                  return;
                }

                final Vector<String> lines = new Vector<String>();

                lines.add("");
                lines.add("InstanceTimeBuffer" + instanceTimeBuffer.getAcumCount());
                lines.add("");

                if(!DetectorActivity.this.instanceTimeBuffer.isEmpty()) {
                  InstanceHashTable lastInstanceBuffer = instanceTimeBuffer.getLast();
                  Iterator iterKey = lastInstanceBuffer.keySet().iterator();
                  while (iterKey.hasNext()) {
                    int nKey = (int) iterKey.next();
                    ArrayList<Classifier.Recognition> recognitionArrayList = lastInstanceBuffer.get(nKey);
                    for (int i = 0; i < recognitionArrayList.size(); i++) {
                      Classifier.Recognition recog = recognitionArrayList.get(i);
                      lines.add(recog.getTitle() + " No."+i +" TimeStamp:"+ recog.getTimeStamp() +" ("+ recog.getMatIdx(N,N).rowIdx + ","+recog.getMatIdx(N,N).colIdx+")");
   }
                  }
                }

                lines.add("");
                lines.add("Compass: " + sotwFormatter.format(service.getAzimuth()));
                lines.add("");
                lines.add("GPS");
                lines.add(" Latitude: " + service.getLatitude());
                lines.add(" Longitude: " + service.getLongitude());
                lines.add("");
                lines.add("Src Station: " + service.getSource_Station());
                lines.add("Src Exit: " + service.getSource_Exit());
                lines.add("Dst Station: " + service.getDest_Station());
                lines.add("Dst Exit: " + service.getDest_Exit());
                lines.add("");

                if(DetectorActivity.this.service.getSectorArrayList().size() > 0) {
                  String tmp = "Path";
                  for (Sector sec : service.getPath()) {
                    tmp = tmp + " -> " + sec.getIndex();
                  }
                  lines.add(tmp);
                  lines.add("Next Sector: " + service.getCurrent_Sector().getIndex());
                  lines.add("matchingFlag: " + service.getMatchingFlag());
                  lines.add("가장 근접한 sector: " + service.idx + ", Score: " + service.score);
                  lines.add("현재 sector: " + service.getUserSectorNum());
                  lines.add("Way: " + service.getWay());
                  lines.add("NextWay: " + service.getNextWay());
                  lines.add("");
                }

                borderedText.drawLines(canvas, 10, canvas.getHeight() - 100, lines);
              }
            });
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    byte[] originalLuminance = getLuminance();
    tracker.onFrame(
        previewWidth,
        previewHeight,
        getLuminanceStride(),
        sensorOrientation,
        originalLuminance,
        timestamp);
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    if (luminanceCopy == null) {
      luminanceCopy = new byte[originalLuminance.length];
    }
    System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @TargetApi(Build.VERSION_CODES.N)
          @Override
          public void run() {
            if(!DetectorActivity.this.yoloFirstStartFlag){
              DetectorActivity.this.yoloFirstStartFlag = true;
              voice.TTS("로딩 완료! Vision, 시작 가능합니다.");
            }
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();

            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            DetectorActivity.this.lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            if(bitmapHeight == 0 || bitmapWidth == 0) {
              DetectorActivity.this.bitmapHeight = croppedBitmap.getHeight();
              DetectorActivity.this.bitmapWidth = croppedBitmap.getWidth();
              Log.e("bitmapSize", "width: " + bitmapWidth );
              Log.e("bitmapSize", "height: " + bitmapWidth );
              instanceTimeBuffer.setBitmapHeight(DetectorActivity.this.bitmapHeight);
              instanceTimeBuffer.setBitmapWidth(DetectorActivity.this.bitmapWidth);
            }
            // Canvas On/Off 기능 생각해보기
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_YOLO;

            final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();


            // 일단은 그냥 아무거나 cropSignBitmap에 넣어보자
            // 제일 처음 뜬 instance crop!!
//            if(results.size() > 0) {
//              RectF cslocation = results.get(0).getLocation();
//              DetectorActivity.this.cropSignBitmap = cropBitmap(croppedBitmap,cslocation);
//            }


            for (final Classifier.Recognition resultb : results) {
              // dot block이 존재한다면 check
              Classifier.Recognition result = resultb.clone();
              if(result.getIdx() == 0) dotFlag = true;
              curSector.setCurSector(result.getIdx());

              //Log.e("result", "=========================offset? : " + result.toString());
              final RectF location = result.getLocation();

              instanceMatrix.putRecog(result);

              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
//                Log.e("mappedRecognitions", "=========================mappedRecognitions? : " + mappedRecognitions + i);
              }
            }

//--------------------------------------Instance Time Buffer 추가 -----------------------------------

            // Instance의 클래스 별로 Table 생성, 각 키별로 ArrayList..
            InstanceHashTable curTimeInstance = new InstanceHashTable();
            // 현재 발견된 instance를 Table화
            if(!results.isEmpty()) {
              for (final Classifier.Recognition result : results) {
                curTimeInstance.putRecog(result);
              }

              // instanceTimeBuffer는 자동으로 최대 사이즈(getMaxSize)를 유지한다.
              instanceTimeBuffer.add(curTimeInstance);

              Log.e("DetectorActivity", "instancLast accum: "+ instanceTimeBuffer.getAcumCount()+" " + instanceTimeBuffer.getLast().keySet());
            }
//----------------------------------------------------------------------------------------

//          시간측정
            DetectorActivity.this.lastProcessingTimeMs1 += SystemClock.uptimeMillis() - startTime;
            //Log.e("Time", "=========================Time? : " + lastProcessingTimeMs1);
            // 3초 지날때마다 갱신
            if(DetectorActivity.this.lastProcessingTimeMs1 >= BUFFERTIME * 1000){


              // navigate 실행, service is ready는 맵데이터를 받아 왔을때 부터 Ture된다.
              if(service != null && service.isReady()) {
                try {
                  navigate();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              // 초기화
              dotFlag = false;
              curSector.reset();
              DetectorActivity.this.lastProcessingTimeMs1 = 0;

              instanceMatrix.instanceClear();
            }

            tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
            trackingOverlay.postInvalidate();

            requestRender();
            computingDetection = false;
          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onSetDebug(final boolean debug) {
    detector.enableStatLogging(debug);
  }

//--Listener----------------------------------------------------------------------------------------------------------------------------------------


  // GPS Location 정보 획득시 리스너 객체
  final LocationListener locationListener = new LocationListener() {
    @Override
    public void onLocationChanged(Location location) {

      service.setLatitude(location.getLatitude());
      service.setLongitude(location.getLongitude());

      Log.e("t", "service 위도: " + service.getLatitude());
      Log.e("t", "service 경도: " + service.getLongitude()+ "\n..\n");

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.e("t", "startGps: 상태변화");
    }
    @Override
    public void onProviderEnabled(String provider) {
      Log.e("t", "startGps: 사용가능");
      //myGps.startGps();
    }
    @Override
    public void onProviderDisabled(String provider) {
      Log.e("t", "startGps: 사용불가");
    }
  };

  public RecognitionListener getRecognitionListner(final MyCallback myCallback){
    return new RecognitionListener() {
      @Override
      public void onReadyForSpeech(Bundle bundle) {

      }

      @Override
      public void onBeginningOfSpeech() {

      }

      @Override
      public void onRmsChanged(float v) {

      }

      @Override
      public void onBufferReceived(byte[] bytes) {

      }

      @Override
      public void onEndOfSpeech() {

      }

      @Override
      public void onError(int i) {
        voice.TTS("음성 에러. 다시 눌러주세요");
        String message;

        switch (i) {

          case SpeechRecognizer.ERROR_AUDIO:
            message = "오디오 에러";
            break;

          case SpeechRecognizer.ERROR_CLIENT:
            message = "클라이언트 에러";
            break;

          case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
            message = "퍼미션없음";
            break;

          case SpeechRecognizer.ERROR_NETWORK:
            message = "네트워크 에러";
            break;

          case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            message = "네트웍 타임아웃";
            break;

          case SpeechRecognizer.ERROR_NO_MATCH:
            message = "찾을수 없음";;
            break;

          case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            message = "바쁘대";
            break;

          case SpeechRecognizer.ERROR_SERVER:
            message = "서버이상";;
            break;

          case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            message = "말하는 시간초과";
            break;

          default:
            message = "알수없음";
            break;
        }
        Log.e("GoogleActivity", "SPEECH ERROR : " + message);
      }

      @Override
      public void onResults(Bundle results) {
        myCallback.callbackBundle(results);
      }

      @Override
      public void onPartialResults(Bundle bundle) {

      }

      @Override
      public void onEvent(int i, Bundle bundle) {

      }
    };
  }



//--Function----------------------------------------------------------------------------------------------------------------------------------------

  /* 한글을 영어로 변환 */
  //초성 - 가(의 ㄱ), 날(ㄴ) 닭(ㄷ)
  public static String[] arrChoSungEng = { "k","K","n","d","D","r", "m", "b","B","s","S",
          "a","j","J","ch","c","t","p","h"};

  //중성 - 가(의 ㅏ), 야(ㅑ), 뺨(ㅑ)
  public static String[] arrJungSungEng = {
          "a", "e", "ya", "ae", "eo", "e", "yeo", "e", "o", "wa", "wae", "oe",
          "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
  };

  //종성 - 가(없음), 갈(ㄹ)
  public static String[] arrJongSungEng = { "", "k", "K", "ks", "n", "nj", "nh",
          "d", "l", "lg", "lm", "lb", "ls", "lt", "lp", "lh", "m", "b", "bs", "s", "ss",
          "ng", "j", "ch", "c", "t", "p", "h"};

  //단일 자음 - ㄱ,ㄴ,ㄷ,ㄹ... (ㄸ,ㅃ,ㅉ은 단일자음(초성)으로 쓰이지만 단일자음으론 안쓰임)
  public static String[] arrSingleJaumEng = {"r", "R", "rt", "s", "sw", "sg", "e", "E", "f",
          "fr", "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "Q", "qt", "t", "T", "d", "w", "W",
          "c", "z", "x", "v", "g"};

  //어디 지하철 역인지 파악하는 메소드
  public String recognizeStation(String stt_Station) {
    String resultEng= "", targetStation="";

    if (stt_Station.contains("역")) {
      targetStation = stt_Station.split("역")[0];
    }
    else targetStation = stt_Station;
    Log.e("11", "stepppp done.");

    if (targetStation.contains("수") || targetStation.contains("상")) targetStation = "상수";
    else if (targetStation.contains("정") || targetStation.contains("합")) targetStation = "합정";
    Log.e("한번 가공후", "step done...");

    Log.e("최종결과는?", targetStation);
    return targetStation;
  };

  //몇번 출구인지 파악하는 메소드
  public String recognizeExit(String stt_Exit) {
        String exitNum = "", exitMatch = "";
        String targetExit = "1";

        if (stt_Exit.contains("번")) {
            exitNum = stt_Exit.split("번")[0];
        } else exitNum = stt_Exit; // 예시로 srcExitNumber="3" or "삼"
        //Log.e("한번 가공후", exitNum);

        if (exitNum.matches("^[0-9]+$")) {
            Log.e("srcExitNumber", "숫자임");
        } else {
            //Log.e("srcExitNumber", "한글임");
            exitMatch = exitNum;
            switch (exitMatch) {
                case "일":
                    targetExit = "1";
                    break;
                case "이":
                    targetExit = "2";
                    break;
                case "삼": case "산":
                    targetExit = "3";
                  break;
                case "사":
                    targetExit = "4";
                  break;
                case "오":
                    targetExit = "5";
                  break;
                case "육":
                    targetExit = "6";
                  break;
                case "칠": case "친":
                    targetExit = "7";
                  break;
                case "팔": case "판": case "팜":
                    targetExit = "8";
                  break;
                case "구":
                    targetExit = "9";
                    break;
                case "십":
                    targetExit = "10";
                    break;
            }
            exitNum = targetExit;
        }
        return exitNum;
    };

  private int initCompletedStatus = 0;

  // 서비스에 필요한 변수들을 초기화한 후, 안내 시작 함수!
  public void initService(int status,final MyCallback myCallback){

    final RecognitionListener sourceStationVoiceListener;
    final RecognitionListener destStationVoiceListener;
    final RecognitionListener confirmVoiceListener;
    final RecognitionListener destExitVoiceListener;
    final RecognitionListener sourceExitVoiceListener;

    // 마지막 변수 확정 리스너 -> 네, 아니요 답변에 따라, 재귀함수 시작 or navigate 함수 시작.
    confirmVoiceListener = getRecognitionListner(new MyCallback() {
      @Override
      public void callback() {

      }

      @Override
      public void callbackBundle(Bundle results) {
        String key = "";
        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);

        String answer = mResult.get(0);
        Log.e("v", "answer: " + answer);

        try {
          Thread.sleep(2000);

          if(answer.charAt(0) == '아' && answer.charAt(1) == '니') DetectorActivity.this.initCompletedStatus = 0;

          else if(answer.charAt(0) != '네' && answer.charAt(0) != '내' && answer.charAt(0) != '예'){
            // 출발지, 도착지가 제대로 체크되지 않았다면, 함수 다시 시작!
            voice.TTS("다시 버튼을 눌러주세요.");
          }

          else{
            //제대로 체크됬다면 확정짓고 출발역의 맵데이터를 가져온다.
            Log.e("v", "Result src & dst: "+ service.getSource_Station() + " " + service.getDest_Station());

            DetectorActivity.this.initCompletedStatus = 0;
            // ~~~~

            // 맵데이터를 서비스에 셋팅을 완료한 후 navigate를 실행하기 위해, callback 함수를 통해 사용한다.
            getMapData_To_Service_From_Server("sangsu", new MyCallback() {
              @Override
              public void callback() {
                myCallback.callback();
              }

              @Override
              public void callbackBundle(Bundle result) {
              }
            });


          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    // 도착 출구 리스너
    destExitVoiceListener = getRecognitionListner(new MyCallback() {
      @Override
      public void callback() {

      }

      @Override
      public void callbackBundle(Bundle results) {
        String key = "",stt_dstExit="";
        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);
        stt_dstExit = mResult.get(0);
        stt_dstExit = recognizeExit(stt_dstExit);
        service.setDest_Exit(stt_dstExit);
        Log.e("v", "Destination Exit onResults: " + service.getDest_Exit());

        DetectorActivity.this.initCompletedStatus = 4;

        try {
          Thread.sleep(1000);
          voice.TTS(service.getSource_Station() + "역 " + service.getSource_Exit() + "번 출구, 에서 출발, "  +
                  service.getDest_Station() + "역 " + service.getDest_Exit() + "번 출구, 도착이 맞습니까? 네, 아니요로 대답해주세요.");
          voice.setRecognitionListener(confirmVoiceListener);
          Thread.sleep(8200);
          voice.STT();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    // 도착역 리스너
    destStationVoiceListener = getRecognitionListner(new MyCallback() {
      @Override
      public void callback() {

      }

      @Override
      public void callbackBundle(Bundle results) {

        String key = "", stt_dstStation = "";

        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);
        stt_dstStation = mResult.get(0);
        stt_dstStation = recognizeStation(stt_dstStation);

        service.setDest_Station(stt_dstStation);
        Log.e("v", "End Station onResults: " + service.getDest_Station());

        DetectorActivity.this.initCompletedStatus = 3;

        try {
          Thread.sleep(2000);
          voice.TTS(senario.destExitString);
          voice.setRecognitionListener(destExitVoiceListener);
          Thread.sleep(2000);
          voice.STT();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    // 출발 출구 리스너
    sourceExitVoiceListener = getRecognitionListner(new MyCallback() {
      @Override
      public void callback() {

      }

      @Override
      public void callbackBundle(Bundle results) {
        String key = "", stt_srcExit="";

        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);
        stt_srcExit = mResult.get(0);
        stt_srcExit = recognizeExit(stt_srcExit);//모든 인식 경우에 대해 출구 결과값을 하나로 도출해냄
        service.setSource_Exit(stt_srcExit);
//        service.setCurrent_Sector(srcExitNumber); // 현재 Sector로 입력
//        service.setNext_Sector_Index(0);

        Log.e("v", "Start Exit onResults: " + service.getSource_Exit() );

        DetectorActivity.this.initCompletedStatus = 2;

        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        voice.TTS(senario.destStationString);
        voice.setRecognitionListener(destStationVoiceListener);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        voice.STT();
      }
    });

    // 출발역 리스너
    sourceStationVoiceListener = getRecognitionListner(new MyCallback() {
      @Override
      public void callback() {
      }

      @Override
      public void callbackBundle(Bundle results) {
        String key = "", stt_srcStation = "";

        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);
        stt_srcStation = mResult.get(0);
        stt_srcStation = recognizeStation(stt_srcStation);//입력받은 단어 파싱
        service.setSource_Station(stt_srcStation);
        Log.e("v", "Start Station onResults: " + service.getSource_Station() ); //입력값 파싱 후 역 이름 로그 찍어보기

        DetectorActivity.this.initCompletedStatus = 1;

        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        voice.TTS(senario.startExitString);
        voice.setRecognitionListener(sourceExitVoiceListener);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        voice.STT();
      }
    });
    ArrayList<RecognitionListener> ListenerArray = new ArrayList<RecognitionListener>(Arrays.asList(sourceStationVoiceListener, sourceExitVoiceListener,
            destStationVoiceListener,destExitVoiceListener, confirmVoiceListener));

    // init 시작
    try{
      voice.setRecognitionListener(ListenerArray.get(status));
      if(status == 4) {voice.TTS(service.getSource_Station() + "역 " + service.getSource_Exit() + "번 출구, 에서 출발, "  +
              service.getDest_Station() + "역 " + service.getDest_Exit() + "번 출구, 도착이 맞습니까? 네, 아니요로 대답해주세요.");
        Thread.sleep(8200);
      }
      else {
        voice.TTS(senario.getI(status));
        Thread.sleep(2500);
      }

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    voice.STT();
  }

  public void announceInstance(){
    ArrayList<Classifier.Recognition> annouceAbleInstance = instanceTimeBuffer.getAnnouncealbeInstance(SystemClock.currentThreadTimeMillis());
    for( Classifier.Recognition instance : annouceAbleInstance)
     while(true) {
       if(!voice.isSpeaking()) {
         instance.Announce(voice);
         break;
       }
     }
    // ...
  }

  public int matchSector() throws JSONException {
    // GPS Update후 비교
    myGps.startGps(DetectorActivity.this.service);
    Log.e("gps",  "gps: " + DetectorActivity.this.service.getLatitude() +",  " + DetectorActivity.this.service.getLongitude());

    // Instance를 통해 sector 찾기
    DetectorActivity.this.service.score = -100;
    int idx = 0;
    for(int i=5; i <= DetectorActivity.this.service.getSectorArrayListSize(); i++){
      if(i==6) continue;
      // i번째 Sector와 nextSector의 Instance들 비교해서 점수 반환
      int score = DetectorActivity.this.service.compareInstance(DetectorActivity.this.service.getMapdataFromIdx(i),
              curSector);
      Log.e("score", i + "번째 score: " + score);
      if(DetectorActivity.this.service.score < score){
        DetectorActivity.this.service.score = score; // maxScore값 변경
        idx = i; // 가장 가까운 위치의 Sector 번호 저장
      }
    }
    service.idx = idx;
    DetectorActivity.this.service.setUserSectorNum(0);
    // Sector 맞다면
    if(DetectorActivity.this.service.score > 0) {
      int current_Sector = DetectorActivity.this.service.getMapdataFromIdx(idx).getIndex();
      DetectorActivity.this.service.setUserSectorNum(current_Sector);
      if(current_Sector == DetectorActivity.this.service.getCurrent_Sector().getIndex()){
        // 가까운 Sector와 Path에서 nextSector의 번호 비교
        if(DetectorActivity.this.service.setCurrentSectorToNext()) return 2;
        // 매칭만 된 경우
        service.setCur_Idx(idx);
        return 1;
      }
    }

    // 매칭 안된 경우
    return 0;
  }

  final public static String[] WAY = {"우측", "우측", "우측", "우측", "뒤", "좌측", "좌측", "좌측"};

  public void navigate() throws JSONException {

    // 각 스텝은 몇초마다 실행될지도 정해야할듯?
    // 전부 3초마다 매번 실행되면 앱이 너무 시끄러울듯!

    announceInstance();
    // OCR send();

    // dot block이 있다면 섹터 여부 확인
    // 목적지 도착 2반환, 매칭만 된 경우 1반환, 매칭 안된 경우 0반환
    service.setMatchingFlag(-1);
    if(dotFlag) service.setMatchingFlag(matchSector());
    Log.e("matchingSector", "matchingSector: " + service.getMatchingFlag());

    // 매칭 된 경우 방향 정하기
    if(DetectorActivity.this.service.getMatchingFlag() == 1){
      if(service.getCur_Idx() == 8){
        voice.TTS("좌측 전방에 개찰구가 있습니다.");
      }
      else if(service.getCur_Idx() == 9){
        voice.TTS("우측으로 블럭따라 유턴하세요.");
      }
      else{
      // index 는 0~7, N 방향부터 시계방향으로
      int index = DetectorActivity.this.sotwFormatter.whereUserGo(DetectorActivity.this.service.getAzimuth(), DetectorActivity.this.service.getWay());
      Log.e("wayIndex", "wayIndex: " + index + ", " + WAY[index]);
//      // {"앞", "우측앞", "우", "우측뒤", "뒤", "좌측뒤", "좌", "좌측앞"} 으로 변환
//      DetectorActivity.this.service.setNextWay(WAY[index] + "으로 가세요. ");
      DetectorActivity.this.service.setNextWay("matching 되었습니다!" + WAY[index] + "으로 가세요. ");
      voice.TTS("" + WAY[index] + "으로 가세요.");
      }
    }


    // 목적지 도착 서비스 종료 TTS 구현
    else if(service.getMatchingFlag() == 2){
      voice.TTS("좌측 합정방향입니다.");
      DetectorActivity.this.service.setNextWay("탑승장입니다.");
    }
    else{
      DetectorActivity.this.service.setNextWay("길찾기 중...");
    }
  }

  // MapData를 서버로 부터 얻어서 Service 객체에 셋
  public void getMapData_To_Service_From_Server(String stationName, final MyCallback myCallback){
    Log.e("t", "GET : /mapdata/"+stationName);

    // Server에 셋팅하기 위한 리스너
    Response.Listener<JSONArray> jsonArrayListener = new Response.Listener<JSONArray>() {
      @Override
      public void onResponse(JSONArray response) {
        ArrayList<Sector> tmpMapdataList = new ArrayList<Sector>();

        for(int i = 0; i< response.length(); i++){
          try {
            tmpMapdataList.add(new Sector(response.getJSONObject(i)));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        DetectorActivity.this.service.setSectorArrayList(tmpMapdataList);

        // 경로 설정
        try {
          DetectorActivity.this.service.setPath(service.getSource_Exit(),service.getDest_Exit());
        } catch (JSONException e) {
          e.printStackTrace();
        }


        if(myCallback != null) myCallback.callback();
      } //onResponse
    };

    // Map api에 전송
    MapRequest jsonRequest = new MapRequest(stationName, jsonArrayListener);
    this.requestQueue.add(jsonRequest);
  }

  // OcrString을 얻어서 TTS
  public void getOcrString(Bitmap bitmap,final Response.Listener<JSONObject> ocrListener){
    Log.e("t", "POST : /ocr");

    OcrRequest ocrRequest = new OcrRequest(bitmap,ocrListener);
    this.requestQueue.add(ocrRequest);
  }

  private Compass.CompassListener getCompassListener() {
    return new Compass.CompassListener() {
      @Override
      public void onNewAzimuth(final float azimuth) {
          DetectorActivity.this.service.setAzimuth(azimuth);
      }
    };
  }

  Bitmap cropBitmap(Bitmap bitmap,RectF location){
    return Bitmap.createBitmap(bitmap,(int)location.left,(int)location.top,(int)(location.right-location.left),(int)(location.bottom-location.top));
  }


  @Override
  public boolean onKeyDown(final int keyCode, final KeyEvent event) {
    if ( keyCode == KeyEvent.KEYCODE_VOLUME_UP
            || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
      this.debug = !this.debug;
      requestRender();
      onSetDebug(debug);
      return true;
    }

    else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ){

      //비트맵 처리 한번 해보기!
//      Bitmap bitmap;
//      if(cropSignBitmap == null)
//        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ocrtest);
//      else
//        bitmap = cropSignBitmap;
//
//        getOcrString(bitmap, new Response.Listener<JSONObject>() {
//        @Override
//        public void onResponse(JSONObject response) {
//          Log.e("h", "OCR Response: " + response.toString());
//          try {
//            voice.TTS(response.getString("text"));
//          } catch (JSONException e) {
//            e.printStackTrace();
//          }
//        }
//      });


    //  서비스를 위한 초기화 작업 시작

      initService(initCompletedStatus, new MyCallback() {
        @Override
        public void callback() {
          Log.e("n", "Navigate 시작" );
          voice.TTS(service.getSource_Station() + "에서 " + service.getDest_Station() + "까지 경로 안내를 시작합니다.");
          service.setReadyFlag(true);
        }

        @Override
        public void callbackBundle(Bundle result) {

        }
      });

      //debugSangsuMapdata();

      return true;
    }
    return super.onKeyDown(keyCode, event);
  }


  public void debugSangsuMapdata(){
    service.setDest_Station("상수");
    service.setSource_Station("합정");
    service.setSource_Exit("2");
    service.setDest_Exit("3");
    getMapData_To_Service_From_Server("sangsu", new MyCallback() {
      @Override
      public void callback() {
        Log.e("n", "Navigate 시작" );
        voice.TTS(service.getSource_Station() + "에서 " + service.getDest_Station() + "까지 경로 안내를 시작합니다.");
        service.setReadyFlag(true);
      }

      @Override
      public void callbackBundle(Bundle results) {

      }
    });
  }

  // GPS 꺼져있을 경우 alert dialog
  protected void createLocationRequest()
  {
    locationRequest = LocationRequest.create();
    locationRequest.setInterval(10000);
    locationRequest.setFastestInterval(5000);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  //  GPS 켜는 dialog 뛰우기
  protected void turn_on_GPS_dialog()
  {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest);

    SettingsClient client = LocationServices.getSettingsClient(DetectorActivity.this);
    Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

    //GPS get에 실패시 (GPS가 꺼져있는 경우)
    task.addOnFailureListener(DetectorActivity.this, new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        if (e instanceof ResolvableApiException)
        {
          // Location settings are not satisfied, but this can be fixed
          // by showing the user a dialog.
          try
          {
            // Show the dialog by calling startResolutionForResult(),
            // and check the result in onActivityResult().
            ResolvableApiException resolvable = (ResolvableApiException) e;
            resolvable.startResolutionForResult(DetectorActivity.this,
                    0x1);
          }
          catch (IntentSender.SendIntentException sendEx)
          {
            // Ignore the error.
          }
          finally {

            myGps.startGps(DetectorActivity.this.service);
            // GPS를 켜고나면 다시 재부팅하라는 안내가 있어야함
            // GPS를 중간에
          }
        }
      }
    });
  }//turn_on_gps end



  @Override
  public void onStart() {
    super.onStart();
    if (Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(DetectorActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
              0);
    }
    Log.d("compass", "start compass");
    compass.start();
  }

  @Override
  public void onPause() {
    super.onPause();
    compass.stop();
  }

  @Override
  public void onResume() {
    super.onResume();
    compass.start();
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d("compass", "stop compass");
    compass.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    voice.close();
  }

}
