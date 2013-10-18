package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import common.Message;
import common.Observer;



public class GuessingGameClient {
	private Socket s;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private HashMap<String,List<Observer>> observermap=new HashMap<String,List<Observer>>();
	public void subscribe(String topic,Observer observer){
		List<Observer> observers=null;
		if(observermap.containsKey(topic)){
			observers=observermap.get(topic);
		}else{
			observers=new LinkedList<Observer>();
			observermap.put(topic,observers);
		}
		observers.add(observer);
	}
	public void unsubscribe(String topic,Observer observer){
		List<Observer> observers=observermap.get(topic);
		observers.remove(observer);
	}
	/**
	 * Publishes specified value concerning specified topic to other people subscribed to that topic.
	 * @param topic
	 * @param value
	 * @throws IOException
	 */
	public void publish(String topic, Object value) throws IOException{
		Message m = new Message(topic,value);
		out.writeObject(m);
	}
	/**
	 * 
	 * @param IP	IP of the machine hosting the server (get from instructor).
	 * @param port	The port on the server to connect to (get from instructor)
	 * @param teamName	Your team's name.
	 * @return The Thread listening for new topic info. Your main method should call join() on this thread after everything else is set up.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Thread openConnection(String IP, int port, String teamName) throws UnknownHostException, IOException{
		s = new Socket(IP, port);
		out = new ObjectOutputStream(s.getOutputStream());
		// flush initial header per JavaDoc
		out.flush();
		in = new ObjectInputStream(s.getInputStream());
		// send team
		Message m = new Message("team",teamName);
		out.writeObject(m);
		// new thread to handle responses
		Thread t = new Thread(){
			public void run(){
				while(s.isConnected()){
					// readObject is a blocking call so no need to wait()
					Object value=null;
					try {
						value = in.readObject();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
					if(value!=null){
						// notify subscribers
						Message m = (Message) value;
						if(observermap.containsKey(m.topic)){
							List<Observer> observers=observermap.get(m.topic);
							for(Observer o : observers){
								o.onUpdate(m.value);
							}
						}
					}
				}
			}
		};
		t.start();
		return t;
	}
	
	/**
	 * TODO: Create and subscribe Observers (implementing the Observer interface) for JOIN, GUESS, and HINT. Then call openConnection with the IP and port number your instructor specifies.
	 * <p>JOIN observer gets one argument, isHinter. If true, you need to start publishing hints and wait for guesses. Otherwise, you publish guesses in response to hints.
	 * <p>GUESS observer gets one argument, the guess you received from the other player. Publish a hint with the value: truenumber.compareTo(guess) 
	 * <p>HINT observer gets one argument, the hint you received from the other player. The hint will tell you to guess lower (<0), higher (>0), or that you got it right. If you got it wrong, publish a new guess.
	 * @param args None at this time, but feel free to use them to specify the IP, port, and team name arguments for openConnection.
	 */
	public static void main(String[] args){
		throw new UnsupportedOperationException("Not implemented");
	}

}
