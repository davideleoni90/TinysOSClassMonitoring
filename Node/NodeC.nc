#include "printf.h"
#include "Acceleration.h"
#include "AM.h"
#include "TinyError.h"
module NodeC{
	uses interface Boot;
	uses interface StdControl;
	uses interface SplitControl as RadioControl;
	uses interface Receive as RootReceive;
	uses interface AMSend;
	uses interface RootControl;
	uses interface Leds;
	uses interface CtpInfo;
	uses interface LinkEstimator;
	uses interface Intercept;
	uses interface Queue<message_t*>;
}
implementation{
	
	/*
	Is the mote already sending a packet through its serial port (UART)?
	*/
	
	bool serialBusy=FALSE;
	
	/*
	These memory location holds exactly one link-layer packet each, plus header and footer from upper layers and metadata; is
	used to hold packets to be sent to the UART (see "TinyOS - TEP 111")
	*/
	
	message_t serialMessage;
	
	/*
	Pointer to first element of the queue containing the packets to send to uart
	*/
	
	message_t* queueHead;
	
	/*
	Is the sending queue full?
	*/
	
	error_t queueFull=FAIL;
	
	/*
	Was the mote successfully set as the root node?
	*/
	
	error_t rootOn=FAIL;
	
	/*
	Was the radio successfully turned on?
	*/
	
	error_t radioOn=EOFF;
	
	/*
	Was the Collection Tree infrastructure successfully started?
	*/
	
	error_t ctpOn=FAIL;
	
	/*
	This variable indicates whether the "send" request has been successfully accepted by the serial communication stack or not
	*/
	
	error_t sendingToSerial;
	
	/*
	Pointer to "acceleration_msg_t" structures (see header "Acceleration.h"), representing the payload of message buffers
	from the serial port
	*/
	
	acceleration_msg_t* payloadSerial;
	
	/*
	Pointers to the payload of incoming and outgoing packets
	*/
	
	acceleration_msg_t* dataReceived;
	acceleration_msg_t* dataToSend;
	
	/*
	Another field in a packet, representing the ID of the parent of the mote within the collection tree
	*/
	
	am_addr_t parent;
	
	task void startRadio();
	
	task void sendSerialMessage();
	
	/*
	When this event is signaled, the mote is up and running, so we a task for turning on the radio transceiver
	*/
	
	event void Boot.booted() {
		post startRadio();
	}
	
	/*
	This task starts the radio subsystem through the HIL component ActiveMessageC; it also tries to start the CollectionTreeProtocol
	subsystem through CollectionC (see "TinyOS - TEP 111") and it tries until it's successfull
	*/
	
	task void startRadio() {
		call RadioControl.start();
		ctpOn=call StdControl.start();
		while(ctpOn!=SUCCESS){
			printf("Error while starting collection infrastructure\n");
			printfflush();
			call StdControl.start();
		}
	}
	
	/*
	If the radio has been successfully started, a confirmation message is printed and, in case the ID of the mote is the same
	as the variable "ROOT_ID" (see header "Acceleration.h"), namely it is designed to be the root of the collection tree, the mote sets himself as
	the root of the collection tree through the command "setRoot"; moreover the led0 is turned on to indicate that the radio subsystem is
	correctly working. In case of any problem, a debugging message is printed and another attempt to turn on the radio is performed
	*/
	
	event void RadioControl.startDone(error_t error){
		radioOn=error;
		if(error!=SUCCESS){
			printf("Could not initialize the radio\n");
			printfflush();
			post startRadio();
			return;
		}
		call Leds.led0On();
		if(TOS_NODE_ID==ROOT_ID){
			
			/*
			Set this mote as the root of the collection tree protocol
			*/
			
			rootOn=call RootControl.setRoot();
			while(rootOn!=SUCCESS){
				printf("Could not set the mote as root of the collection tree\n");
				call RootControl.setRoot();
			}
		}
		printfflush();
	}
	
	/*
	The radio is never stopped, so this event will never be signaled; anyway, according to nesC language,
	the handler for this event has to be present
	*/
	
	event void RadioControl.stopDone(error_t error){
		
	}
	
	/*
	This event is signaled when the mote intercepts a packet whose destination is the root node:it updates the "hopcount",
	"message_path" and "path_quality" fields of the payload in order to trace the path of the message, and then forwards 
	the packet. Note that when a packet is received by the underlying layer of the collection tree protocol, two distinct
	events may be signaled: "receive" (from the Receive interface) in case the mote is the root of the collection tree,
	"forward" (from the Intercept interface) otherwise. (see "TinyOS - TEP 119" and component "CtpForwardingEngineP").
	*/
	
	event bool Intercept.forward(message_t* msg, void* payload, uint8_t len){
		dataReceived=(acceleration_msg_t*)payload;
		if(dataReceived!=NULL && sizeof(acceleration_msg_t)==len){
		
			/*
			Fill the "message_path" array with the current mote ID
			*/
			
			dataReceived->message_path[dataReceived->hopcount]=TOS_NODE_ID;
			
			/*
			The mote ID of the parent node
			*/
			
			call CtpInfo.getParent(&parent);
			
			/*
			Fill the "path_quality" array with the quality of the link to
			the parent node of this mote
			*/
			
			dataReceived->path_quality[dataReceived->hopcount]=call LinkEstimator.getLinkQuality(parent);;
			
			/*
			Update the counter of motes along the path from accelerometer
			to host
			*/
			
			dataReceived->hopcount++;
			
			/*
			Toggle the led1 to indicate that the mote is forwarding messages
			*/
			
			call Leds.led1Toggle();
			
			/*
			The current packet can be forwarded
			*/
			
			return TRUE;
		}
		printf("Packet not well formed: discarding it...\n");
		printfflush();
		
		/*
		The current packet can't be forwarded since it's not well formed
		*/
		
		return FALSE;
	}
	
	/*
	When a message is received by underlying layer of the collection tree protocol and the mote is the root of the tree, the event
	"receive" is signaled: the mote acts as a gateway towards the Internet, hence the message has to be sent to the UART so that
	data will be updated on Parse by a pc. The mote maintains a FIFO queue containing the packets received: every time the serial
	port is not busy, the first element of the queue is sent through the serial port itself. In this way it is guaranteed that no packet
	is discarded and that they are sent to the pc (hence uploaded on Parse) in the same order as they are received by the root mote.
	Obviously this holds iff the arrival rate of packets is not such that the queue gets full before the packets are sent through the 
	UART; the size of the queue can be tuned, as well as the sampling period, by mean of variables in the header file "Acceleration.h"
	*/
	
	event message_t* RootReceive.receive(message_t* msg, void* payload, uint8_t len){
		dataReceived=(acceleration_msg_t*)(payload);
		
		/*
		Check if the packet is well formed, namely the "len" parameter is equal to the size of the payload of the message
		*/
		
		if(dataReceived!=NULL && len==sizeof(acceleration_msg_t)){
			
			/*
			If the message is well formed, put it into the queue and then start a task to send it to the serial port
			*/
			
			queueFull=call Queue.enqueue(msg);
			
			/*
			Check if the queue is already full: if so, the packet is discarded and a debug message is printed
			*/
			
			if(queueFull!=SUCCESS){
				printf("The sending queue is full or some other problem has occurred! Discarding packet...\n");
				printfflush();
			}
		}
		else{
			
			/*
			If the message is not well formed, discard it
			*/
			
			printf("Packet not well formed: discarding it...\n");
			printfflush();
		}
		
		/*
		Start the task to send the queued packets
		*/
		
		post sendSerialMessage();
		
		/*
		The interface "Receive" has a buffer-swap policy (see "TinyOS - TEP 116"): this forces the handler of the event "receive"
		to always return the message buffer (msg) to the signaler of the event, because this prevent lower layers of the communication
		stack from being blocked by upper layers; in this way any problem related with the management of the packets by a component (for
		example "NodeC") doesn't involve other components.
		*/
		
		return msg;
	}
	
	/*
	This task is in charge of sending the packets (received from the radio subsystem) to the gateway terminal through the serial port.
	In case the mote is already busy sending another packet to the UART, the task returns without doing nothing, otherwise it
	sends a packet through the serial port using the broadcast address
	*/
	
	task void sendSerialMessage(){
		if(!serialBusy){
			
			/*
			Get the pointer to the payload area of the serial message buffer (serialMessage)
			*/
			
			payloadSerial=(acceleration_msg_t*)call AMSend.getPayload(&serialMessage,sizeof(acceleration_msg_t));
			
			/*
			If this pointer is NULL, it means that the maximum space for the payload of the packets of the TelosB
			is not sufficient for the data that are trying to be sent: this is something that should never happen
			*/
			
			if(payloadSerial!=NULL){
				
				/*
				Get the first element from the queue, but WITHOUT REMOVING IT from the queue itself. In fact, since we 
				want packets to be delivered to uart in the same order as they are received via radio, if the packet was dequeued before 
				actually being sent, it may be that some problem occurr during the sending phase and then it would be
				lost or, at least, it would only be possible to put it again in the queue as last element, thus messing up
				the order of packets
				*/
				
				queueHead=call Queue.head();
				
				/*
				Check if the queue is empty
				*/
				
				if(queueHead!=NULL){
					dataToSend=(acceleration_msg_t*)call AMSend.getPayload(queueHead,sizeof(acceleration_msg_t));
					
					/*
					The packet that is going to be sent through the UART has the same payload as the packet taken from the
					queue, so the C function "memcpy" is used to copy the payload of the radio packet into the serial buffer
					*/
					
					memcpy(payloadSerial,dataToSend,sizeof(acceleration_msg_t));
					
					/*
					Send the packet trough uart
					*/
					
					sendingToSerial=call AMSend.send(0xffff,&serialMessage,sizeof(acceleration_msg_t));
					
					/*
					In case of any problem while sending the message, turn on led 1
					*/
					
					if(sendingToSerial!=SUCCESS){
						call Leds.led1On();
					}
					else{
						serialBusy=TRUE;
					}
				}
				else{
					printf("The queue is empty! No message left to send\n");
					printfflush();
				}
			}
		}
	}
	
	/*
	If the sending phase was successfull, it's time to dequeue the packet and to send the next packets in the queue, whether existing;
	also a confirmation message is printed, led 2 is toogled and led 1 is turned off, in case it was on because of a preceeding error.
	If some problem occurs during the sending phase, leds 1 is turned on and a debugging message is printed
	*/
	
	event void AMSend.sendDone(message_t* msg, error_t error){
		
		/*
		The uart is no longer busy
		*/
		
		serialBusy=FALSE;
		if(error==SUCCESS){
			printf("Message correctly sent\n");
			
			/*
			Since we are at this point, there must be at least one element in the queue
			*/
			
			call Queue.dequeue();
			call Leds.led2Toggle();
			call Leds.led1Off();
			if(call Queue.empty()==FALSE){
				
				/*
				The queue is not empty, meaning that there are some more packets to send, then schedule a new sending
				phase
				*/
				
				post sendSerialMessage();
			}
		}
		else{
			printf("An error occurred while sending message\n");
			call Leds.led1On();
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
