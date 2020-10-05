package org.tensorflow.demo.vision_module;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Sector {

    private String id;
    private String name;
    private String type;
    private Boolean dot;
    private Boolean line;
    private String escalator;
    private Boolean stairs;
    private Boolean pillar; //원기둥
    private Boolean signBoard; //벽에 붙은 간판
    private Boolean topBoard; //천장에 달린 간판
    private Boolean subwayTracks; //탑승장
    private Boolean exit;
    private Boolean enterGate; //개찰구
    private JSONObject gps; // [위도, 경도]


    public Sector(){

    }

    public Sector(JSONObject job){
        try {
            this.id = job.getString("_id");
            this.name = job.getString("name");
            this.type = job.getString("type");
            this.dot = job.getBoolean("dot");
            this.line = job.getBoolean("line");
            this.escalator = job.getString("escalator");
            this.stairs = job.getBoolean("stairs");
            this.pillar = job.getBoolean("pillar");
            this.signBoard = job.getBoolean("signBoard");
            this.topBoard = job.getBoolean("topBoard");
            this.subwayTracks = job.getBoolean("subwayTracks");
            this.exit = job.getBoolean("exit");
            this.enterGate = job.getBoolean("enterGate");
            this.gps = job.getJSONObject("gps");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("object error", "Sector: " + e );
        }
    }

    public void setId(String id){
        this.id = id;
    }
    public void setName(String name) { this.name = name; }
    public void setType(String type){
        this.type = type;
    }
    public void setDot(Boolean dot){
        this.dot=dot;
    }
    public void setLine(Boolean line){
        this.line = line;
    }
    public void setEscalator(String escalator) {this.escalator=escalator; }
    public void setStairs(Boolean stairs){ this.stairs = stairs; }
    public void setPillar(Boolean pillar){   this.pillar = pillar;    }
    public void setSignBoard(Boolean signBoard) {this.signBoard = signBoard;}
    public void setTopBoard(Boolean topBoard) {this.topBoard = topBoard;}
    public void setSubwayTracks(Boolean subwayTracks) {this.subwayTracks=subwayTracks;}
    public void setExit(Boolean exit) {this.exit = exit;}
    public void setEnterGate(Boolean enterGate) { this.enterGate = enterGate;}
    public void setGPS(JSONObject gps) { this.gps = gps; }


    public String getId(){ return this.id;}
    public String getName() { return this.name; }
    public String getType(){ return this.type; }
    public Boolean getDot() { return this.dot; }
    public Boolean getLine() {return this.line;}
    public String getEscalator() { return this.escalator;}
    public Boolean getStairs() { return this.stairs;}
    public Boolean getPillar() { return this.pillar;}
    public Boolean getSignBoard() { return this.signBoard;  }
    public Boolean getTopBoard() { return this.topBoard; }
    public Boolean getSubwayTracks() { return this.subwayTracks; }
    public Boolean getExit() { return this.exit; }
    public Boolean getEnterGate() { return this.enterGate; }
    public JSONObject getGPS() { return this.gps;}
}
