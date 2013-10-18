package common;

import java.io.Serializable;

public class Message implements Serializable{
	final static int serialVersionUID=1;
	public String topic;
	public Object value;
	public Message(String topic, Object value){
		this.topic=topic;
		this.value=value;
	}
}
