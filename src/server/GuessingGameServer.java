package server;

import static common.Topics.GUESS;
import static common.Topics.GUESSER;
import static common.Topics.HINT;
import static common.Topics.HINTER;
import static common.Topics.JOIN;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import common.Message;
import common.Observer;

class GuessingGameServer {
	private static List<GameThread> teams=new LinkedList<GameThread>();
	private static class GameThread extends Thread{
		HashMap<String,List<Observer>> observermap=new HashMap<String,List<Observer>>();
		Socket clientSocket;
		ObjectOutputStream out;
		ObjectInputStream in;
		String team;
		public int score;
		public GameThread(Socket c) throws IOException, ClassNotFoundException{
			clientSocket=c;
			out=new ObjectOutputStream(c.getOutputStream());
			out.flush();
			in=new ObjectInputStream(c.getInputStream());
			// receive team
			Message m=(Message) in.readObject();
			team=(String)m.value;
		}
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
			synchronized(observers){
				observers.remove(observer);
			}
		}
		public void publish(String topic, Object value) throws IOException{
			Message m = new Message(topic,value);
			out.writeObject(m);
		}
		public void run(){
			while(clientSocket.isConnected()){
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
						synchronized(observers){
							for(Observer o : observers){
								o.onUpdate(m.value);
							}
						}
					}
				}
			}
		}
	}
	private static class PassThroughObserver extends CountingObserver{
		GameThread t2;
		String topic;
		public PassThroughObserver(GameThread t2, String topic){
			this.t2=t2;
			this.topic=topic;
		}
		public void onUpdate(Object value){
			super.onUpdate(value);
			if(count>3) return;
			try{
				t2.publish(topic,value);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	private static class CountingObserver implements Observer{
		public int count=0;
		public int lastValue=-1;
		public void onUpdate(Object value){
			count++;
			this.lastValue=(Integer)value;
		}
	}
	private static class GuessObserver extends CountingObserver{
		public void onUpdate(Object value){
			super.onUpdate(value);
			System.out.println("Guess "+count+" was: "+lastValue);
		}
	}
	private static class HintObserver extends CountingObserver{
		public void onUpdate(Object value){
			super.onUpdate(value);
			System.out.println("Hint "+count+" was: "+lastValue);
			if(lastValue==0||count>=3){
				// game finished
			}
		}
	}
	public static void main(String[] args) throws IOException, InterruptedException{
		Scanner sc = new Scanner(System.in);
		int port=Integer.valueOf(args[0]);
		int desiredTeams=Integer.valueOf(args[1]);
		ServerSocket serverSocket = new ServerSocket(port);
		Socket clientSocket=null;
		for(int i=0; i<desiredTeams; i++){
			clientSocket=serverSocket.accept();
			GameThread t=null;
			try {
				t = new GameThread(clientSocket);
				teams.add(t);
				t.start();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				i--;
			}
		}
		// bracket
		while(teams.size()>1){
			System.out.println("Teams competing:");
			for(GameThread t : teams){
				System.out.println(t.team);
			}
			sc.nextLine();
			List<GameThread> nextTeams=new LinkedList<GameThread>();
			for(int i=0; i<teams.size()-1; i+=2){
				// play pair of teams
				GameThread t1=teams.get(i);
				GameThread t2=teams.get(i+1);
				t1.score=0;
				t2.score=0;
				System.out.println(t1.team+" vs "+t2.team);
				// game
				int round=1;
				while(round<=3){
					// t1 will guess first, so pass guesses to t2
					PassThroughObserver o1=new PassThroughObserver(t2,GUESS);
					t1.subscribe(GUESS, o1);
					// listen for 3 guesses
					GuessObserver serverGuessListener = new GuessObserver();
					t1.subscribe(GUESS, serverGuessListener);
					// t2 will provide hints, so pass hints to t1
					PassThroughObserver o2=new PassThroughObserver(t1,HINT);
					t2.subscribe(HINT, o2);
					// listen for 3 hints
					HintObserver serverHintListener = new HintObserver();
					t2.subscribe(HINT, serverHintListener);
					// notify to join game
					t1.publish(JOIN,GUESSER);
					t2.publish(JOIN,HINTER);
				
					while(serverHintListener.lastValue!=0&&serverHintListener.count<3){
						Thread.sleep(100);
					}
					if(serverHintListener.lastValue==0){
						// t1 won
						t1.score++;
						System.out.println(t1.team+" won the round!");
						if(t1.score>=2){
							System.out.println(t1.team+" won the game!");
							break;
						}
					}else{
						// t2 won
						t2.score++;
						System.out.println(t2.team+" won the round!");
						if(t2.score>=2){
							System.out.println(t2.team+" won the game!");
							break;
						}
					}
					// unsubscribe
					t1.unsubscribe(GUESS, o1);
					t1.unsubscribe(GUESS, serverGuessListener);
					t2.unsubscribe(HINT, o2);
					t2.unsubscribe(HINT, serverHintListener);
					// switch sides
					GameThread t3=t1;
					t1=t2;
					t2=t3;
				}
				if(t1.score>t2.score){
					nextTeams.add(t1);
				}else{
					nextTeams.add(t2);
				}
			}
			teams=nextTeams;
		}
	}
}
