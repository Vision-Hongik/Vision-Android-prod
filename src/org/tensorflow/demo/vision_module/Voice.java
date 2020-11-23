package org.tensorflow.demo.vision_module;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class Voice {

    //음성 인식용
    private Intent SttIntent;
    private SpeechRecognizer mRecognizer;

    //음성 출력용
    private TextToSpeech tts;
    private Activity activity;

    public Voice(Activity cThis, RecognitionListener listener){

        this.activity = cThis;

        //STT 설정
        SttIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        SttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.activity.getApplicationContext().getPackageName());
        SttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");//한국어 사용
        mRecognizer=SpeechRecognizer.createSpeechRecognizer(this.activity);
        mRecognizer.setRecognitionListener(listener);

        //TTS 설정
        this.tts=new TextToSpeech(this.activity, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status!= TextToSpeech.ERROR){
                    int lang = tts.setLanguage(Locale.KOREAN);
                    Log.e("TTS", "onInit: Succes");
                    if (lang == TextToSpeech.LANG_MISSING_DATA
                            || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // 언어 데이터가 없거나, 지원하지 않는경우
                        Log.e("TTS", "onInit: Fail 지원 언어 없음.");
                    }
                }
                else{
                    Log.e("TTS", "onInit: Fail");
                }
            }
        });

    }

    public void STT(){
        if(ContextCompat.checkSelfPermission(this.activity, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this.activity,new String[]{Manifest.permission.RECORD_AUDIO},1);
            //권한을 허용하지 않는 경우
        }else{
            //권한을 허용한 경우
            try {
                this.mRecognizer.startListening(this.SttIntent);
            }catch (SecurityException e){e.printStackTrace();}
        }
    }


    public void TTS(String OutMsg){
        if(OutMsg.length()<1) return;


        tts.setPitch(1.0f);//목소리 톤1.0
        tts.setSpeechRate(1.0f);//목소리 속도
        tts.speak(OutMsg,TextToSpeech.QUEUE_FLUSH,null,null);
        //어플이 종료할때는 완전히 제거

    }

    public boolean isSpeaking(){
        return tts.isSpeaking();
    }

    public void setRecognitionListener(RecognitionListener listener){
        this.mRecognizer.setRecognitionListener(listener);
    }

    public void ttsStop(){
        this.tts.stop();
    }

    public void close(){
        if(this.tts!=null){
            this.tts.stop();
            this.tts.shutdown();
            this.tts=null;
        }
        if(this.mRecognizer!=null){
            this.mRecognizer.destroy();
            this.mRecognizer.cancel();
            this.mRecognizer=null;
        }
    }

    public void init(RecognitionListener listener){
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this.activity);
        mRecognizer.setRecognitionListener(listener);
    }

}

