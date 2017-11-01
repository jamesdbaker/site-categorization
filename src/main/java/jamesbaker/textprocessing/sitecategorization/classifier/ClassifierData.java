package jamesbaker.textprocessing.sitecategorization.classifier;

import java.util.ArrayList;
import java.util.List;

public class ClassifierData {
	private List<String> data = new ArrayList<>();
	private String key;
	
	public ClassifierData(String key) {
		this.key = key;
	}
	
	public void addData(String data) {
		this.data.add(data);
	}
	public List<String> getData(){
		return data;
	}
	
	public String getKey() {
		return key;
	}
}
