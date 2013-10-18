package client;

import static common.Topics.GUESS;
import static common.Topics.HINT;
import static common.Topics.JOIN;

import common.Observer;

public class JPEGGuessingGameClient extends GuessingGameClient {
	JPEGGuessingGameClient c;
	Integer secretValue=3;
	int lastGuess=1;
	public static void main(String[] args) {
		try{
			new JPEGGuessingGameClient();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public JPEGGuessingGameClient(){
		String ip="localhost";
		int port=12345;
		String teamName="JPEG";
		// subscribe observers
		c=this;
		// new game: figure out role
		c.subscribe(JOIN, new Observer(){
			@Override
			public void onUpdate(Object response) {
				boolean isHinting=(Boolean)response;
				if(isHinting){
					// set new secret value for this round
					secretValue=3;
				}else{
					// send first guess
					lastGuess=1;
					try{
					c.publish(GUESS, lastGuess);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		});
		// receive guess: send updated hint
		c.subscribe(GUESS, new Observer(){
			@Override
			public void onUpdate(Object response) {
				try{
					c.publish(HINT, secretValue.compareTo((Integer)response));
				}catch(Exception e){
					
				}
			}
		});
		// receive hint: send updated guess
		c.subscribe(HINT, new Observer(){
			@Override
			public void onUpdate(Object response) {
				int hint=(Integer)response;
				
				if (hint > 0){ //if the secretValue is smaller (guess was too high)
					lastGuess = hint/2;
				}
				else if (hint < 0) //(guess was too low){
					lastGuess = hint*2;
				}
				try{
					c.publish(GUESS, lastGuess);
				}catch(Exception e){
					
				}
			}
		});
		try{
			c.openConnection(ip,port,teamName);
		}catch(Exception e){
			
		}
	}
}