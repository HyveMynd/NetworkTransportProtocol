package edu.cs4480.protocol.stats;

/**
 * Created by andresmonroy on 3/16/14.
 */
public class NetStats {
	private static NetStats instance;

	private int droppedMsgs;
	private int transmittedMsg;
	private int lostPackets;
	private int corruptedPackets;
	private int totalPackets;
	private int deliveredMsgs;

	private NetStats(){
	}

	public static NetStats getInstance(){
		if (instance == null){
			instance = new NetStats();
		}
		return instance;
	}

	public void transMsg(){
		transmittedMsg++;
	}

	public void dropMsg(){
		droppedMsgs++;
	}

	public void lostPkt(){
		lostPackets++;
	}

	public void corruptPkt(){
		corruptedPackets++;
	}

	public void totalPkt(){
		totalPackets++;
	}

	public void msgDelivered(){
		deliveredMsgs++;
	}

	public String getStats(){
		return String.format(
				"\n-==NET STATS==-\n" +
				"Total Messages: %d\n" +
				"Transmitted Messages: %d\n" +
				"Dropped Messages: %d\n" +
				"Delivered Messages: %d\n" +
				"Total Packets: %d\n" +
				"Lost Packets: %d\n" +
				"Corrupted Packets: %d\n" +
				"Percent Lost: %f\n" +
				"Percent Corrupted: %f\n", (transmittedMsg + droppedMsgs), transmittedMsg, droppedMsgs, deliveredMsgs,
				totalPackets, lostPackets, corruptedPackets,
				((double)lostPackets/(double)totalPackets) * 100f,
				((double)corruptedPackets/(double)(totalPackets-lostPackets)) * 100f);
	}
}
