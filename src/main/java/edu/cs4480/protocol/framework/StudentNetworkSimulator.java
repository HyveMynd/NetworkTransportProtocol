package edu.cs4480.protocol.framework;

import edu.cs4480.protocol.stats.NetStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAX_DATA_SIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData):
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a aSequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a aSequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's aSequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's aSequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
	private static final Logger logger = LoggerFactory.getLogger(StudentNetworkSimulator.class.getName());
	private boolean aSequence;
	private int aCurrentSequence;
	private boolean aIsTransmitting;
	private Packet aCurrentPacket;
	private NetStats stats;
	private double aCountdown;
	private int bPreviousSequence;

	/**
	 * Creates a Packet from the message.
	 * @param seq The aSequence number to use for this packet
	 * @param msg The message to be converted to a packet
	 * @return The newly created packet
	 */
	private Packet toPacket(int seq, Message msg) {
		int ack = 0;
		int check = getChecksum(seq, ack, msg.getData());
		return new Packet(seq, 0, ~check, msg.getData());
	}

	/**
	 * Create a default ACK packet.
	 * @param seq The aSequence number to use for this packet
	 * @return The newly created packet representing an ACK
	 */
	private Packet createAck(int seq){
		int ack = 1;
		int check = getChecksum(seq, ack, "");
		return new Packet(seq, ack, ~check);
	}

	/**
	 * Create a default NACK packet
	 * @return The newly created packet representing a NACK
	 */
	private Packet createNack(){
		int seq = aSequence ? 0 : 1;
		int ack = 1;
		int check = getChecksum(seq, ack, "");
		return new Packet(seq, ack, ~check);
	}

	/**
	 * Creates a checksum from the given parameters. NOTE: The returned sum is not inverted.
	 * @param seq The aSequence number
	 * @param ack The acknowledge number
	 * @param data The payload
	 * @return A calculated checksum. NOTE: The checksum is not inverted.
	 */
	private int getChecksum(int seq, int ack, String data) {
		int sum = 0;
		for (int i = 0; i < data.length(); i++) {
			sum += Character.getNumericValue(data.charAt(i));
		}
		sum += seq;
		sum += ack;
		return sum;
	}

	/**
	 * Checks the packet for corruption.
	 * @param pkt The packet to check for corruption.
	 * @return True if the packet is corrupted
	 */
	private boolean isCorrupted(Packet pkt){
		int checksum = getChecksum(pkt.getSeqnum(), pkt.getAcknum(), pkt.getPayload());
		logger.debug("packet checksum: {}, checksum: {}", pkt.getChecksum(), checksum);
		int sum = pkt.getChecksum() + checksum;
		logger.debug("sum is: {}", sum);
		return sum != -1;
	}

	/**
	 * Check if the packet is an acknowledge. NOTE: Packet must be checked for corruption
	 * before using this method.
	 * @param pkt The packet to check for acknowledge
	 * @return True if the packet is an ACK
	 */
	private boolean isAck(Packet pkt){
		return pkt.getAcknum() == 1 && pkt.getSeqnum() == aCurrentSequence;
	}

	/**
	 * Convenience method to do all necessary operations associated with packet transmission.
	 * @param entity 0 for entity A, 1 for entity B
	 * @param pkt the packet to transmit
	 * @param startTimer true if the timer should start
	 */
	private void transmitPacket(int entity, Packet pkt, boolean startTimer){
		toLayer3(entity, pkt);
		stats.totalPkt();
		if (startTimer){
			startTimer(entity, aCountdown);
		}
	}

	/**
	 * Conevnience method to initialize a message for transmission.
	 * @param msg the message to send
	 * @return the packet ready for transmission
	 */
	private Packet initPacket(Message msg){
		stats.transMsg();
		aCurrentSequence = aSequence ? 1 : 0;
		return toPacket(aCurrentSequence, msg);
	}

	// This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
		if (!aIsTransmitting){
			aIsTransmitting = true;
			logger.info("aOutput: received message: {}", message.getData());
			aCurrentPacket = initPacket(message);
			logger.info("aOutput: transmitting packet: {}",aCurrentPacket.toString());
			transmitPacket(0, aCurrentPacket, true);
		} else {
			stats.dropMsg();
			logger.info("aOutput: Dropping message. Already transmitting. Message: {}", message.getData());
		}
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
		stopTimer(0);
		logger.debug("aInput packet: " + packet.toString());
		if (isCorrupted(packet)){
			logger.info("aInput: Received corrupt packet. Retransmitting.");
			stats.corruptPkt();
			transmitPacket(0, aCurrentPacket, true);
		} else {
			if (isAck(packet)){
				logger.info("aInput: Packet intact. Successful transfer.");
				aSequence = !aSequence; // Change to next aSequence
				aIsTransmitting = false;
			} else {
				logger.info("aInput: Got Nack, retransmitting.");
				transmitPacket(0, aCurrentPacket, true);
			}
		}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
		stats.lostPkt();
		logger.info("aTimer: Lost packet. Retransmitting");
		transmitPacket(0, aCurrentPacket, true);
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
		stats = NetStats.getInstance();
		aSequence = false;
		aIsTransmitting = false;
		aCurrentSequence = 0;
		aCurrentPacket = null;
		aCountdown = 100;
	}
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
		// check corruption
		logger.debug("bInput packet: {}", packet.toString());
		if (isCorrupted(packet)){
			logger.info("bInput: Corrupt Packet. Sending Nack.");
			stats.corruptPkt();
			transmitPacket(1, createNack(), false);
		} else {
			logger.info("bInput: Packet ok. Sending Ack.");
			transmitPacket(1, createAck(packet.getSeqnum()), false);
			if (bPreviousSequence != packet.getSeqnum()){
				logger.info("bInput: New Message. Sending to layer 5");
				bPreviousSequence = packet.getSeqnum();
				toLayer5(1, packet.getPayload());
			} else {
				logger.info("bInput: Duplicate message. Not resending to layer 5.");
			}
		}
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
		// init to the opposite to simulate ready to receive next packet
		bPreviousSequence = aSequence ? 0 : 1;
    }
}
