package nachos.threads;

import nachos.machine.*;
import java.util.Random;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
		commlock = new Lock ();
		speaker = new Condition(commlock);
		listener = new Condition(commlock);

    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
		commlock.acquire();
		while(listenersWaiting == 0 || sharedWordInUse)
			speaker.sleep();

		sharedWordInUse = true;
		sharedWord = word;

		listener.wake();
        commlock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
		commlock.acquire();

		listenersWaiting++;
		Lib.debug(dbgCommunicator, "Waiting Listeners " + listenersWaiting);
		speaker.wake();
		listener.sleep();

		int word = sharedWord;
		sharedWordInUse = false;
		listenersWaiting--;
		Lib.debug(dbgCommunicator, "Waiting Listeners " + listenersWaiting);

		commlock.release();


	return word;
    }

	public static void selfTest(){
		Lib.debug(dbgCommunicator, "Testing Communicator");
		KThread speak, listen;

        final Communicator com = new Communicator();

		//test that at least one speaker will pair up with one listener
		//also tests if the listener blocks until speaker available
		speak = new KThread(new Runnable(){
			public void run(){
                   com.speak(10);
			}
		});

		listen = new KThread (new Runnable (){
			public void run(){
                  int word = com.listen();
				  Lib.debug(dbgCommunicator, (word == 10 ? "Pass" : "Fail" ) + " Successful Transfer.");

			}
		});

		listen.fork(); speak.fork();
		listen.join(); speak.join();

		//test that speaker blocks waiting for a listener

		speak = new KThread (new Runnable(){
			public void run(){
			com.speak(10);

			}
		});

		listen = new KThread(new Runnable(){
			public void run(){
			com.listen();

			}
		});

		speak.fork();
        Lib.debug(dbgCommunicator, "Testing blocking speaker waiting for listener.");
		Lib.debug(dbgCommunicator, "Waiting listener "  + com.listenersWaiting);

		listen.fork();
		speak.join();
		listen.join();

		//testing to see if all speaker and listener threads exit properly.
		//Waiting Listeners should be 0 at the end.
		for(int i = 0; i < 5; i++){
			listen = new KThread(new Runnable(){
				public void run(){
					com.listen();
				}
			});
			listen.fork();
			//Lib.debug(dbgCommunicator, "Waiting Listeners " + com.listenersWaiting);
		}
		//spawns speaker threads to pair off with listener threads
		for(int i = 0; i < 5; i++){
			KThread speaker = new KThread(new Runnable(){
				public void run(){
					com.speak(0);
				}
			});
			speaker.fork();
			//Lib.debug(dbgCommunicator, "Waiting Listeners " + com.listenersWaiting);
			listen.join();
			speaker.join();
		}


		Lib.debug(dbgCommunicator, (com.listenersWaiting == 0 ? "Pass" : "Fail"));

		int sum;

		speak = new KThread(new Runnable(){
			public void run(){
				com.speak(9);
			}
		});

		KThread speak2 = new KThread(new Runnable(){
			public void run(){
				com.speak(9);
			}
		});

		for(int i = 0; i < 2 ; i++){
			new KThread(new Runnable(){
				public void run(){
					com.listen();
				}
			});
		}
		//Lib.debug(dbgCommunicator, (sum == 18 ? "Pass" : "Fail") + "No overwriting happened.");



		speak = new KThread(new Runnable(){
			public void run(){
		        Random generator = new Random();
				int random = generator.nextInt();
				com.speak(random);
			}
		});

		listen = new KThread(new Runnable(){
			public void run(){
				int word = com.listen();
				Lib.debug(dbgCommunicator, (word > 0 ? "Pass" : "Fail") + " Successful Transfer.");
			}
		});

		speak.fork();
		listen.fork();
		speak.join();
		listen.join();


	}
	private static final char dbgCommunicator = 'c';

	private Lock commlock;
	private Condition speaker;
	private Condition listener;
	private int sharedWord;
	private boolean sharedWordInUse = false;
	private int listenersWaiting = 0; //keeps a tally of how many listeners are
								  //currently waiting.
}
