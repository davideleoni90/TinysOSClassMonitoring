#include "printf.h"
#include "ADXL345.h"
#include "Network.h"
module AdxlC {
	uses interface Boot;
	uses interface Leds;
	uses interface Timer<TMilli> as TimerAccel;
	uses interface Read<uint16_t> as Zaxis;
	uses interface Read<uint16_t> as Yaxis;
	uses interface Read<uint16_t> as Xaxis;
	uses interface Send;
	uses interface SplitControl as AccelControl;
	uses interface StdControl;
	uses interface SplitControl as RadioControl;
	uses interface CtpInfo;
	uses interface LinkEstimator;
	uses interface RadioChannel;
	uses interface Queue<acceleration_msg_t*>;
}
implementation {

	/*
	One link-layer packet, plus header and footer from upper
	layers and metadata (see "TinyOS - TEP 111")
	*/	
	
	message_t buffer;
	
	/*
	The three following variables hold the x,y and z acceleration values read by the accelerometer 
	*/
	
	uint16_t xAcceleration=0;
	uint16_t yAcceleration=0;
	uint16_t zAcceleration=0;
	
	/*
	Is the output queue full?
	*/
	
	error_t queueFull=FAIL;
	
	/*
	Is the mote already sending a packet?
	*/
	
	bool busySending=FALSE;
	
	/*
	Was the radio successfully turned on?
	*/
	
	error_t radioOn=EOFF;
	
	/*
	Was the Collection Tree infrastructure successfully started?
	*/
	
	error_t ctpOn=FAIL;
	
	/*
	The mote-ID of the parent in the collection tree
	*/
	
	am_addr_t parent;
	
	/*
	Pointer to the payload area of the radio message buffer
	*/
	
	acceleration_msg_t* radioPayload;
	
	/*
	The pointer to first element of the queue containing the packets to send
	*/
	
	acceleration_msg_t* queueHead;
	
	/*
	The pointer to the payload of packets that are going to be sent via radio
	*/
	
	acceleration_msg_t* newPacketPayload;
	
	/*
	The following three steps are crucial, so we want to perform them atomically,
	in a non-preemptable way: that's why we compute them within dedicated tasks.
	We keep all the three tasks quite short, so they don't degradate overall
	performances (in accordance to TinyOS guidelines)
	*/
	
	task void startAcc();
	
	task void startRadio();
	
	task void sendMessage();
	
	/*
	When this event is signaled, the mote is up and running, so two tasks are 
	started for turning on the accelerometer and the radio transceiver
	*/
	
	event void Boot.booted() {
		post startAcc();
		post startRadio();
	}
	
	/*
	When this event is signaled, it's time to read acceleration values
	from the accelerometer. Since values from all the axes X,Y and Z are
	required, it's important to make sure that these values will be present
	in the message sent by the mote to its parent.
	In order to achieve this, every time the timer is fired the mote 
	reads the value of the X acceleration, and only later, within the method
	handling the value read, it also requests a reading of the Y acceleration.
	The same holds for the readings of Y and Z acceleration values
	*/
	
	event void TimerAccel.fired() {
		call Xaxis.read();
	}
	
	/*
	If the accelerometer has been successfully started, a periodic timer is started
	(this period, i.e. SAMPLING_PERIOD, is defined in the header Acceleration.h):
	every time the timer is fired, values from the accelerometer are read.
	In case of any problem, a debugging message is printed and another attempt
	to turn on the accelerometer is performed
	*/
	
	event void AccelControl.startDone(error_t err) {
		if(err != SUCCESS) {
			printf("Could not start the accelerometer\n");
			printfflush();
			post startAcc();
		}
		else{
			
			/*
			Start the periodic timer
			*/
			
			call TimerAccel.startPeriodic(SAMPLING_PERIOD);
		}
		
	}
	
	/*
	This task starts the accelerometer by mean of its driver
	*/
	
	task void startAcc() {
		call AccelControl.start();
	}
	
	/*
	This task starts the radio subsystem through the HIL component ActiveMessageC;
	it also tries to start the CollectionTreeProtocol subsystem through CollectionC
	(see "TinyOS - TEP 111") and it tries until it's successfull
	*/
	
	task void startRadio() {
		call RadioControl.start();
		ctpOn=call StdControl.start();
		while(ctpOn!=SUCCESS){
			printf("Error while starting collection infrastructure");
			printfflush();
			call StdControl.start();
		}
	}
	
	/*
	Either the radio and the accelerometer are never stopped, so this event will never
	be signaled; anyway, according to nesC language, the handler for this event has
	to be defined
	*/
	
	event void AccelControl.stopDone(error_t err) {
		
	}
	
	/*
	Either the radio and the accelerometer are never stopped, so this event will never
	be signaled; anyway, according to nesC language,the handler for this event has
	to be defined
	*/
	
	event void RadioControl.stopDone(error_t err) {
		
	}
	
	/*
	If the radio has been successfully started, its channel (as defined by the standard
	IEEE 802.15.4, with which the Magonode platform is compliant) is set to the same
	channel as the default one used by TelosB, namely the number 26.
	This is mandatory in order for Magonodes and TelosBs to communicate.
	In case of any problem, a debugging message is printed and another attempt to turn 
	on the radio is performed
	*/
	
	event void RadioControl.startDone(error_t err){
		radioOn=err;
		if(err!=SUCCESS){
			printf("Could not initialize the radio\n");
			printfflush();
			post startRadio();
		}
		else{
			
			/*
			Set the same channel as the Telosb
			*/
			
			call RadioChannel.setChannel(26);
		}
	}
	
	/*
	The event is signaled when the radio channel of the Magonode has been 
	successfully set: print a confirmation message and turn on led 0
	*/
	
	event void RadioChannel.setChannelDone(){
		printf("Radio channel succssfully set to 26\n");
		printfflush();
		call Leds.led0On();
	}
	
	/*
	If the value of acceleration along X axis has been successfully read,
	a confirmation message is printed and the value is temporary stored in
	order to be sent later, after values of Y and Z acceleration will have
	been gathered too; also the mote samples the value for Y acceleration.
	In case of any problem, a debugging message is printed and another attempt
	to sample the X axis of the accelerometer is performed.
	*/
	
	event void Xaxis.readDone(error_t result, uint16_t data) {
		if(result!=SUCCESS){
			printf("Could not sample X axis of the accelerometer\n");
			call Xaxis.read();
			
		}
		else{
			printf("X axis acceleration:(%d) ", data);
			xAcceleration=data;
			call Yaxis.read();
		}
		printfflush();
	}
	
	/*
	Same as "Xaxis.readDone", but for Y acceleration
	*/
	
	event void Yaxis.readDone(error_t result, uint16_t data) {
		if(result!=SUCCESS){
			printf("Could not sample Y axis of the accelerometer\n");
			call Yaxis.read();
			
		}
		else{
			printf("Y axis acceleration:(%d) ", data);
			yAcceleration=data;
			call Zaxis.read();
		}
		printfflush();
	}
	
	/*
	If the value of acceleration along Z axis has been successfully read,
	a confirmation message is printed and the value is temporary
	stored in order to build the message to send to the parent node. 
	Then, the payload (type "acceleration_msg_t") of a new packet is
	initialised to the values of the acceleration along the three
	axes. Also the "hopcount" field is initialised to "1" (since Magonodes
	are the "producers" of messages within the network) and the mote ID 
	is written in the first position of the "message_path" array; furthermore
	the quality of the link to the parent node is stored in the first position
	of the "path_quality" array.
	In case of any problem with the reading of Z axis, a debugging message
	is printed and another attempt is performed.
	*/
	
	event void Zaxis.readDone(error_t result, uint16_t data) {
		if(result!=SUCCESS){
			printf("Could not sample Z axis of the accelerometer\n");
			call Zaxis.read();
		}
		else{
			
			/*
			This structure is the payload of the packet that is being created
			*/
			
			acceleration_msg_t payload;
			zAcceleration=data;
			newPacketPayload=&payload;
			
			/*
			Create the content message: set the value
			of acceleration along X axis
			*/
			
			newPacketPayload->x_acceleration=xAcceleration;
			
			/*
			Set the value of acceleration along Y axis
			*/
			
			newPacketPayload->y_acceleration=yAcceleration;
			
			/*
			Set the value of acceleration along Z axis
			*/
			
			newPacketPayload->z_acceleration=data;
			
			/*
			Since Magonode platforms act as "producers" within the collection tree,
			they represent the starting point of any message sent to the root of 
			the tree itself. That's why the "hopcount" field has to be set to "1"
			and also the first element in the array "message_path" has to be set to
			the current mote ID; moreover the value of the quality of the link to the
			parent is saved as first element in the array "path_quality".
			Every time this message will be intercepted by "processors"
			motes (i.e. Telosb), the hopcount will be incremented by 1 and the arrays 
			will be filled with the IDs and link qualities of the processors themselves.
			*/
			
			newPacketPayload->hopcount=1;
			newPacketPayload->message_path[0]=TOS_NODE_ID;
			
			/*
			The mote ID of the parent node
			*/
			
			call CtpInfo.getParent(&parent);
			
			/*
			The "bidirectional" quality of the link between this node and its parent.
			This is calculated as the result of the probability that a message sent by
			the node is received by the parent times the probability that a messages
			sent by the parent is received by the node
			*/
			
			newPacketPayload->path_quality[0]=call LinkEstimator.getLinkQuality(parent);
			
			/*
			Once the payload of the message has been set, it is put in the sending queue waiting to be sent
			*/
			
			queueFull=call Queue.enqueue(newPacketPayload);
			
			/*
			Check if the queue is already full: if so, the packet is discarded and a debug message is printed
			*/
			
			if(queueFull!=SUCCESS){
				printf("The sending queue is full or some other problem has occurred! Discarding packet...\n");
				printfflush();
			}
		}
		
		/*
		Schedule the task to send the queued packets
		*/
		
		post sendMessage();
		printfflush();
	}
	
	
	/*
	This task is in charge of sending messages to the neighbor node,
	with the values read from the accelerometer.
	At first, it's necessary to check if the radio is on and it's
	tuned on channel 26, otherwise it won't be possible to send any 
	message. This is achieved by checking the previously set variable
	"radioOn" and by calling the method "getChannel()" of the RadioChannel
	interface; in case the check is not successfull, the task "startRadio"
	is posted again. Once the radio is running and set up correctly,
	another check is performed: if the mote is already busy sending 
	another message, it won't be possible to send the message (otherwise
	the message buffer may be corruputed since it's concurrently accessed).
	Once the mote is no longer busy, the first packet in the queue is extracted
	and sent.
	*/
	
	task void sendMessage(){
		
		/*
		If this is an attempt to send the message after a preceeding failure,
		led2 has to be turned off
		*/
		
		call Leds.led2Off();
		if(radioOn==SUCCESS && call RadioChannel.getChannel()==26){
			if(!busySending){
				
				/*
				Initialize the pointer to the payload of the radio messages buffer
				*/
				
				radioPayload=(acceleration_msg_t*)call Send.getPayload(&buffer,sizeof(acceleration_msg_t));
				
				/*
				If this pointer is NULL, it means that the maximum space for the
				payload of the packets of the Magonode is not sufficient for the
				data that are trying to be sent: this is something that should never happen
				*/
				
				if(radioPayload!=NULL){
					
					/*
					Get the first element from the queue, but WITHOUT REMOVING IT from
					the queue itself. In fact, since we  want acceleration measures to
					be delivered in the same order as they are produced, if the packet
					was dequeued before actually being sent, it may be the case that
					some problem occurr during the sending phase and then it would be
					lost or, at least, it would only be possible to put it again in
					the queue as last element, thus messing up the order of packets.
					Since we are inside a task context, we don't have to worry about
					race conditions while accessing the queue data structure
					*/
					
					queueHead=call Queue.head();
					
					/*
					Check if the queue is empty
					*/
					
					if(queueHead!=NULL){
						
						/*
						Set the content of the packet to the values of the first payload in the queue
						*/
						
						memcpy(radioPayload,queueHead,sizeof(acceleration_msg_t));
						
						/*
						When the send request is issued, the led 1 is turned on to indicate that the mote
						is currently sending a message and also the boolean variable "busySending" is set.
						*/
						
						if((call Send.send(&buffer, sizeof(acceleration_msg_t)))==SUCCESS){
							busySending=TRUE;
							call Leds.led1On();
						}
						else{
							
							/*
							The code should never get here, since messages are sent only when the mote is not 
							already sending other messages and when the radio stack is on; a call to "send()"
							only fails when at least one of these conditions is not met
							*/
						}
					}
				}	
			}
		}
		else{
			post startRadio();
		}
	}
	
	/*
	This event is signaled when either the message has been successfully sent or
	some problem occurred. In the former case, a confirmation message is printed
	and the payload of packet just sent can be removed from the queue; also, in
	case the queue is not empty, a new task is scheduled to send the queued packets.
	In the latter case a debugging message is printed and led 2 is turned on
	*/
	
	event void Send.sendDone(message_t* msg, error_t error){
		
		/*
		The radio transceiver is no longer busy; turn off led 1
		*/
		
		busySending=FALSE;
		call Leds.led1Off();
		if(error==SUCCESS){
			printf("Message correctly sent\n");
			printf("X:%d",((acceleration_msg_t*)msg)->x_acceleration);
			printf("Y:%d",((acceleration_msg_t*)msg)->y_acceleration);
			printf("Z:%d",((acceleration_msg_t*)msg)->z_acceleration);
			
			/*
			Remove the first element from the queue, since it has been
			succesfully sent. The current event is signaled within a
			task, so we don't to worry aboout race conditions while
			accessing the queue data structure
			*/
			
			call Queue.dequeue();
		}
		else{
			printf("An error occurred while sending message\n");
			call Leds.led2On();
		}
		printfflush();
	}
	
	/*
	Event handler for the event "evicted" of the LinkEstimator, which is signaled
	when a neighbor node is deleted by the neighbor table: we don't need an handler
	for this event but we have to include this method because of the signature of
	the LinkEstimator interface => does nothing
	*/
	
	event void LinkEstimator.evicted(am_addr_t neighbor){
	}
}
