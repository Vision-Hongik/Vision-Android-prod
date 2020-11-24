package org.tensorflow.demo.vision_module;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Sector {

    private String id;
    private String name;
    private String type;
    private int index;
    private Boolean dot;
    private Boolean line;
    private Boolean upEscalator;
    private Boolean downEscalator;
    private Boolean upStair;
    private Boolean downStair;
    private Boolean pillar; //원기둥
    private Boolean board; //벽에 붙은 간판
    private Boolean upBoard; //천장에 달린 간판
    private Boolean subwayTracks; //탑승장
    private Boolean inSign;
    private Boolean outSign;
    private Boolean sign;
    private Boolean gate; //개찰구
    private JSONObject gps; // [위도, 경도]
    private JSONArray adjacent_idx;
    private JSONArray adjacent_dir;

    public Sector(){}

    public Sector(boolean init){
        this.dot = init;
        this.line = init;
        this.upEscalator = init;
        this.downEscalator = init;
        this.upStair = init;
        this.downStair = init;
        this.pillar = init;
        this.board = init;
        this.upBoard = init;
        this.inSign = init;
        this.outSign = init;
        this.sign = init;
        this.gate = init;
    }

    public Sector(Sector sec){
        this.id = sec.id;
        this.name = sec.name;
        this.type = sec.type;
        this.index = sec.index;
        this.dot = sec.dot;
        this.line = sec.line;
        this.upEscalator = sec.upEscalator;
        this.downEscalator = sec.downEscalator;
        this.upStair = sec.upStair;
        this.downStair = sec.downStair;
        this.pillar = sec.pillar;
        this.board = sec.board;
        this.upBoard = sec.upBoard;
        this.subwayTracks = sec.subwayTracks;
        this.inSign = sec.inSign;
        this.outSign = sec.outSign;
        this.sign = sec.sign;
        this.gate = sec.gate;
        this.gps = sec.gps;
        this.adjacent_idx = sec.adjacent_idx;
        this.adjacent_dir = sec.adjacent_dir;
    }

    public Sector(JSONObject job){
        try {

            this.id = job.getString("_id");
            this.name = job.getString("name");
            this.type = job.getString("type");
            this.index = job.getInt(("index"));
            this.dot = job.getBoolean("dot");
            this.line = job.getBoolean("line");
            this.upEscalator = job.getBoolean("upEscalator");
            this.downEscalator = job.getBoolean("downEscalator");
            this.upStair = job.getBoolean("upStair");
            this.downStair = job.getBoolean("downStair");
            this.pillar = job.getBoolean("pillar");
            this.board = job.getBoolean("board");
            this.upBoard = job.getBoolean("upBoard");
            this.subwayTracks = job.getBoolean("subwayTracks");
            this.inSign = job.getBoolean("inSign");
            this.outSign = job.getBoolean("outSign");
            this.sign = job.getBoolean("sign");
            this.gate = job.getBoolean("gate");
            this.gps = job.getJSONObject("gps");
            this.adjacent_idx = job.getJSONArray("adjacents_idx");
            this.adjacent_dir = job.getJSONArray("adjacents_dir");

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
    public void setIndex(int index) {this.index = index;}
    public void setDot(Boolean dot){
        this.dot=dot;
    }
    public void setLine(Boolean line){
        this.line = line;
    }
    public void setUpEscalator(Boolean upEscalator) {this.upEscalator=upEscalator; }
    public void setDownEscalator(Boolean downEscalator) {this.downEscalator=downEscalator; }
    public void setUpStair(Boolean upStair) {this.upStair = upStair; }
    public void setDownStair(Boolean downStair) {this.downStair = downStair; }
    public void setPillar(Boolean pillar){   this.pillar = pillar;    }
    public void setBoard(Boolean board) {this.board = board;}
    public void setUpBoard(Boolean upBoard) {this.upBoard = upBoard;}
    public void setSubwayTracks(Boolean subwayTracks) {this.subwayTracks=subwayTracks;}
    public void setInSign(Boolean inSign) {this.inSign = inSign;}
    public void setOutSign(Boolean outSign) {this.outSign = outSign;}
    public void setSign(Boolean sign) {this.sign= sign;}
    public void setGate(Boolean gate) { this.gate=gate;}
    public void setGPS(JSONObject gps) { this.gps = gps; }
    public void setAdjacentIdx(JSONArray adjacent_idx) { this.adjacent_idx = adjacent_idx; }
    public void setAdjacentDir(JSONArray adjacent_dir) { this.adjacent_dir = adjacent_dir; }


    public String getId(){ return this.id;}
    public String getName() { return this.name; }
    public String getType(){ return this.type; }
    public int getIndex() {return this.index;}
    public Boolean getDot() { return this.dot; }
    public Boolean getLine() {return this.line;}
    public Boolean getUpEscalator() { return this.upEscalator;}
    public Boolean getDownEscalator() { return this.downEscalator;}
    public Boolean getUpStair() { return this.upStair;}
    public Boolean getDownStair() { return this.downStair;}
    public Boolean getPillar() { return this.pillar;}
    public Boolean getBoard() { return this.board;  }
    public Boolean getUpBoard() { return this.upBoard; }
    public Boolean getSubwayTracks() { return this.subwayTracks; }
    public Boolean getInSign() {return this.inSign;}
    public Boolean getOutSign() {return this.outSign;}
    public Boolean getSign() {return this.sign;}
    public Boolean getGate() { return this.gate; }
    public JSONObject getGPS() { return this.gps;}
    public JSONArray getAdjacentIdx() { return this.adjacent_idx;}
    public JSONArray getAdjacentDir() { return this.adjacent_dir;}

    public void setCurSector(int idx) {
        switch (idx){
            case 0:
                this.setDot(true);
                break;
            case 1:
                this.setLine(true);
                break;
            case 3:
                this.setUpEscalator(true);
                break;
            case 4:
                this.setDownEscalator(true);
                break;
            case 5:
                this.setUpStair(true);
                break;
            case 6:
                this.setDownStair(true);
                break;
            case 7:
                this.setPillar(true);
                break;
            case 8:
                this.setBoard(true);
                break;
            case 9:
                this.setUpBoard(true);
                break;
            case 10:
                this.setInSign(true);
                break;
            case 11:
                this.setOutSign(true);
                break;
            case 12:
                this.setSign(true);
                break;
            case 13:
                this.setGate(true);
                break;
            default:
                break;
        }
    }

    public void reset(){
        final boolean init = false;
        this.dot = init;
        this.line = init;
        this.upEscalator = init;
        this.downEscalator = init;
        this.upStair = init;
        this.downStair = init;
        this.pillar = init;
        this.board = init;
        this.upBoard = init;
        this.inSign = init;
        this.outSign = init;
        this.sign = init;
        this.gate = init;
    }

    @Override
    public String toString() {
        String resultString = "";
        resultString += "upEscalator: " + upEscalator + "\n";
        resultString += "downEscalator: " + downEscalator + "\n";
        resultString += "upStair: " + upStair + "\n";
        resultString += "downStair: " + downStair + "\n";
        resultString += "pillar: " + pillar + "\n";
        resultString += "board: " + board + "\n";
        resultString += "upBoard: " + upBoard + "\n";
        resultString += "inSign: " + inSign + "\n";
        resultString += "outSign: " + outSign + "\n";
        resultString += "sign: " + sign + "\n";
        resultString += "gate: " + gate + "\n";

        return resultString;
    }
}


