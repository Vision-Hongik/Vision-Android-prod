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
import android.location.Location;
import android.location.LocationListener;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import org.tensorflow.demo.vision_module.MyCallback;
import org.tensorflow.demo.vision_module.MapRequest;
import org.tensorflow.demo.vision_module.MyGps;
import org.tensorflow.demo.vision_module.OcrRequest;
import org.tensorflow.demo.vision_module.Sector;
import org.tensorflow.demo.vision_module.Service;
import org.tensorflow.demo.vision_module.Voice;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
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
  private static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;

  private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.YOLO;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;

  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private long lastProcessingTimeMs1;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private float bitmapWidth;
  private float bitmapHeight;
  ArrayList< Hashtable<Integer, Classifier.Recognition>> instanceBuffer = new ArrayList<Hashtable<Integer, Classifier.Recognition>>();
  private static final int BUFFERTIME = 2;

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

  private boolean yoloFirstStartFlag = false;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    for(int i=0; i<4; i++){
      instanceBuffer.add(new Hashtable<Integer, Classifier.Recognition>());
    }

    // GPS가 꺼져있다면 On Dialog
    createLocationRequest();
    turn_on_GPS_dialog();


    //Gps
    myGps = new MyGps(DetectorActivity.this,locationListener);
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
      @Override
      public void run() {
        myGps.startGps();
        Log.e("thread", "run: start");
      }
    },0);


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

            if(DetectorActivity.this.service.getSectorArrayList().size() > 0){
              lines.add(service.getSource_Station() + " Receive Map Data!");
              lines.add("");
              for(int i = 0; i < DetectorActivity.this.service.getSectorArrayList().size(); i++){
                lines.add("Sector" + i);
                lines.add(" Name: " + service.getSectorArrayList().get(i).getName());
                lines.add(" GPS: " + service.getSectorArrayList().get(i).getGPS());
                lines.add("");
              }
            }

            lines.add("");
            lines.add("Instance Buffer");
            lines.add("");
            for(int i=0; i<4; i++){
              Set keySet = DetectorActivity.this.instanceBuffer.get(i).keySet();
              Iterator iterKey = keySet.iterator();
              String tmp = (i+1) +"사분면: ";
              while(iterKey.hasNext()){
                int nKey = (int) iterKey.next();
                tmp = tmp + " (" + DetectorActivity.this.instanceBuffer.get(i).get(nKey).getTitle() + ", "+ DetectorActivity.this.instanceBuffer.get(i).get(nKey).getCount()+")";
              }
              lines.add(tmp);
            }
            lines.add("");
            lines.add("GPS");
            lines.add(" Latitude: " + service.getLatitude());
            lines.add(" Longitude: " + service.getLongitude());
            lines.add("");
            lines.add("Src Station: " + service.getSource_Station());
            lines.add("Dst Station: " + service.getDest_Station());

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
              voice.TTS("로딩 완료 시작 가능합니다.");
            }
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            DetectorActivity.this.lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            bitmapWidth = croppedBitmap.getWidth() - 1;
            bitmapHeight = croppedBitmap.getWidth() - 1;

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

            for (final Classifier.Recognition result : results) {
              //Log.e("result", "=========================offset? : " + result.toString());
              final RectF location = result.getLocation();

              int flag;
              float centorX = (location.left + location.right) / 2;
              float centorY = (location.bottom + location.top) / 2;
              // 좌표를 기준으로 4개의 ImageSector로 구분
              if (0 < centorX && centorX <= bitmapWidth / 2 && 0 < centorY && centorY <= bitmapHeight / 2) flag = 0;
              else if(bitmapWidth / 2 < centorX && centorX <= bitmapWidth && 0 < centorY && centorY <= bitmapHeight / 2) flag = 1;
              else if(0 < centorX && centorX <= bitmapWidth / 2 && bitmapHeight / 2 < centorY && centorY <= bitmapHeight) flag = 2;
              else flag = 3;

              // Key값에 맞게 result 저장
              final int key = result.getIdx();
              final Classifier.Recognition value = instanceBuffer.get(flag).get(key);
              if(value == null) {
                result.setCount(1);
                instanceBuffer.get(flag).put(key, result);
              }
              else{
                result.setCount(value.getCount() + 1);
                instanceBuffer.get(flag).replace(key, result);
              }

              Log.e("result", "=========================result? : " + result + ", key: " + key);

              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
//                Log.e("mappedRecognitions", "=========================mappedRecognitions? : " + mappedRecognitions + i);
              }
            }

//          시간측정
            DetectorActivity.this.lastProcessingTimeMs1 += SystemClock.uptimeMillis() - startTime;
            Log.e("Time", "=========================Time? : " + lastProcessingTimeMs1);
            // 2초 지날때마다 갱신
            if(DetectorActivity.this.lastProcessingTimeMs1 >= BUFFERTIME * 1000){
              for(int i=0; i<4; i++){
                Set keySet = instanceBuffer.get(i).keySet();
                Iterator iterKey = keySet.iterator();
                while(iterKey.hasNext()){
                  int nKey = (int) iterKey.next();
                  Log.e("key",  (i+1) + "사분면, value: " + instanceBuffer.get(i).get(nKey));
                }
              }
              // board 짤라서 -> OCR 보내기

              // 초기화
              DetectorActivity.this.lastProcessingTimeMs1 = 0;
              for(int i=0; i<4; i++) {
                instanceBuffer.get(i).clear();
              }
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



//--Function----------------------------------------------------------------------------------------------------------------------------------------

  // 서비스에 필요한 변수들을 초기화한 후, 안내 시작 함수!
  public void initService(final MyCallback myCallback){

    final RecognitionListener sourceStationVoiceListener;
    final RecognitionListener destStationVoiceListener;
    final RecognitionListener confirmVoiceListener;

    // 마지막 변수 확정 리스너 -> 네, 아니요 답변에 따라, 재귀함수 시작 or navigate 함수 시작.
    confirmVoiceListener = new RecognitionListener() {
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
        voice.TTS("음성 에러 5초후 다시 말씀해주세요!");
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
        String key = "";
        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);

        String answer = mResult.get(0);
        Log.e("v", "answer: " + answer);

        try {
          Thread.sleep(2000);
          if(answer.charAt(0) != '네' && answer.charAt(0) != '내'){
            // 출발지, 도착지가 제대로 체크되지 않았다면, 함수 다시 시작!
            voice.TTS("다시 버튼을 눌러주세요.");
          }
          else{
            //제대로 체크됬다면 확정짓고 출발역의 맵데이터를 가져온다.
            Log.e("v", "Result src & dst: "+ service.getSource_Station() + " " + service.getDest_Station());

            // ~~~~

            // 맵데이터를 서비스에 셋팅을 완료한 후 navigate를 실행하기 위해, callback 함수를 통해 사용한다.
            getMapData_To_Service_From_Server("sangsu", new MyCallback() {
              @Override
              public void callback() {
                myCallback.callback();
              }
            });


          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onPartialResults(Bundle bundle) {
      }

      @Override
      public void onEvent(int i, Bundle bundle) {
      }

    };

    // 도착지 물어보는 리스너
    destStationVoiceListener = new RecognitionListener() {
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
        voice.TTS("음성 에러 5초후 다시 말씀해주세요!");
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
        String key = "";
        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);

        service.setDest_Station(mResult.get(0));
        Log.e("v", "End Station onResults: " + service.getDest_Station());


        try {
          Thread.sleep(2000);
          voice.TTS("출발지는 " + service.getSource_Station() + "도착지는 " + service.getDest_Station()+ "이 맞습니까? 네 아니요로 대답해주세요.");
          voice.setRecognitionListener(confirmVoiceListener);
          Thread.sleep(6000);
          voice.STT();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onPartialResults(Bundle bundle) {
      }

      @Override
      public void onEvent(int i, Bundle bundle) {
      }

    };

    // 출발지 물어보는 리스너
    sourceStationVoiceListener = new RecognitionListener() {
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
        voice.TTS("음성 에러 5초후 다시 말씀해주세요!");
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
        String key = "";
        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);

        service.setSource_Station(mResult.get(0));
        Log.e("v", "Start Station onResults: " + service.getSource_Station() );

        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        voice.TTS("어디 역으로 가시나요?");
        voice.setRecognitionListener(destStationVoiceListener);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        voice.STT();
      }

      @Override
      public void onPartialResults(Bundle bundle) {
      }

      @Override
      public void onEvent(int i, Bundle bundle) {
      }

    };


    // init 시작
    voice.TTS("어디 역에서 출발 하시나요?");
    voice.setRecognitionListener(sourceStationVoiceListener);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    voice.STT();
  }

  public void navigate(){
    Log.e("n", "Navigate 시작" );
    voice.TTS(service.getSource_Station() + "에서 " + service.getDest_Station() + "까지 경로 안내를 시작합니다.");

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

        //log 확인.
        Log.e("h", "Number of Sector : " + DetectorActivity.this.service.getSectorArrayList().size());
        for(int i = 0; i < DetectorActivity.this.service.getSectorArrayList().size(); i++){
          Log.e("h", "onResponse Name: " + service.getSectorArrayList().get(i).getName());
          Log.e("h", "onResponse ID: " + service.getSectorArrayList().get(i).getId());
          Log.e("h", "onResponse type: " + service.getSectorArrayList().get(i).getType());
          Log.e("h", "onResponse dot: " + service.getSectorArrayList().get(i).getDot());
          Log.e("h", "onResponse Line: " + service.getSectorArrayList().get(i).getLine());
          Log.e("h", "onResponse escalator: " + service.getSectorArrayList().get(i).getEscalator());
          Log.e("h", "onResponse Stairs: " + service.getSectorArrayList().get(i).getStairs());
          Log.e("h", "onResponse Pillar: " + service.getSectorArrayList().get(i).getPillar());
          Log.e("h", "onResponse signBoard: " + service.getSectorArrayList().get(i).getSignBoard());
          Log.e("h", "onResponse Topboard: " + service.getSectorArrayList().get(i).getTopBoard());
          Log.e("h", "onResponse subwayTracks: " + service.getSectorArrayList().get(i).getSubwayTracks());
          Log.e("h", "onResponse Exit: " + service.getSectorArrayList().get(i).getExit());
          Log.e("h", "onResponse enterGate: " + service.getSectorArrayList().get(i).getEnterGate());
          Log.e("h", "onResponse GPS: " + service.getSectorArrayList().get(i).getGPS() + "\n");
        }
        if(myCallback != null) myCallback.callback();
      } //onResponse
    };

    // Map api에 전송
    MapRequest jsonRequest = new MapRequest(stationName, jsonArrayListener);
    this.requestQueue.add(jsonRequest);
  }

  // OcrString을 얻어서 TTS
  public void getOcrString_AND_TTS(Bitmap bitmap,final MyCallback myCallback){
    Log.e("t", "POST : /ocr");

    Response.Listener<JSONObject> ocrListener = new Response.Listener<JSONObject>() {
      @Override
      public void onResponse(JSONObject response) {
        Log.e("h", "Response: " + response.toString());
        myCallback.callback();
      }
    };
    OcrRequest ocrRequest = new OcrRequest(bitmap,ocrListener);
    this.requestQueue.add(ocrRequest);
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

            myGps.startGps();
            // GPS를 켜고나면 다시 재부팅하라는 안내가 있어야함
            // GPS를 중간에
          }
        }
      }
    });
  }//turn_on_gps end


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
      initService(new MyCallback() {
        @Override
        public void callback() {
          navigate();
        }
      });
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }


  @Override
  public void onStart() {
    super.onStart();
    if (Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(DetectorActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
              0);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    voice.close();
  }

}
