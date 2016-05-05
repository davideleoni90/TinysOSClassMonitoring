#include "Acceleration.h"

/*Configuration for a mote which forwards messages received my Magonodes to the root of the collection tree (TelosB)*/

configuration NodeAppC {

}
implementation {

	/*The application (NodeC) is wired to MainC in order to boostrap the mote (see "TinyOS - TEP 107")*/
	components MainC, NodeC as App;
	App->MainC.Boot;
	
	/*Leds are wired for debugging purposes (see NodeC.nc)*/
	components LedsC;
	App.Leds->LedsC;

	/*REMOVE components new TimerMilliC() as Timer;
	App.Timer->Timer;*/
	
	/*In order to trace the execution of the application, logging messages are printed on the screen (in case the mote is connected
	to a pc via serial port) thanks to the command "printf". Hence PrintfC has to be wired, as well as SerialStartC, and the latter starts the
	serial port; since the serial port is automatically started at boot time, there's no need to explicitely start it by including the component
	"Serial Active MessageC" */
	components PrintfC, SerialStartC;
	
	components CollectionC as Collector;
	
	/*CtpP provides the interface LinkEstimator, necessary to estimate the bi-directional link quality between two nodes*/
	components CtpP;
	
	/*The mote keeps an internal FIFO queue where it stores all the packets received (the length of this queue is determined by the variable
	"UART_QUEUE_DEPTH" in the header file Acceleration.h): in this way, in case a packet cannot be sent to the serial port as soon as it is
	received becasuse the port is busy, the packet is not discarded; on the contrary, it is sent as soon as the serial port is available
	again. The type of the elements stored in the queue is pointer to a message buffer*/
	components new QueueC(message_t*, UART_QUEUE_DEPTH) as Queue;
	
	/*This configuration provides the interface SplitControl to turn on the radio subsystem (see "TinyOS - TEP 116")*/
	components ActiveMessageC;
	
	/*REMOVE This is platform-independet top component of the Serial communication stack (see "TinyOS - TEP 113"), and hence it has to 
	included in the configuration in order to turn on the serial communication subsystem
	components SerialActiveMessageC;*/
	
	/*The Collection Tree Protocol provided by TinyOS comprises a set of generic components: CollectionSenderC is the virtualized
	abstraction for sending messages to root nodes; "CTP_COLLECTION_ID" marks the messages involved in this communication protocol
	(see TinyOS - TEP 110 and 119)*/
	components new CollectionSenderC(CTP_COLLECTION_ID) as Sender;
	
	/*SerialAMSenderC is the virtualized abstraction for sending messages to the serial port of a mote; "CTP_COLLECTION_ID" marks 
	the messages involved in this serial communication protocol
	(see TinyOS - TEP 116)*/
	components new SerialAMSenderC(CTP_COLLECTION_ID) as SerialSender;
	
	/*The root node has to use the parameterised "Receive" interface to receive messages sent by the Magonode motes; the parameter is
	represented by the id of the collection "CTP_COLLECTION_ID"*/
	App.RootReceive->Collector.Receive[CTP_COLLECTION_ID];
	
	/*In order to configure a node as the root of a collection tree, the interface "RootControl" has to be used*/
	App.RootControl->Collector;
	
	/*The "processor" nodes have to use the parameterised "Receive" interface to intercept messages sent by the Magonode motes; the parameter
	is represented by the id of the collection "CTP_COLLECTION_ID"*/
	App.Intercept->Collector.Intercept[CTP_COLLECTION_ID];
	
	/*Start the radio subsystem*/
	App.RadioControl->ActiveMessageC;
	
	/*Start the Collection Tree Protocol subsystem*/
	App.StdControl->Collector;
	
	/*The mote is capable of getting the mote-ID of its parent in the collection tree*/
	App.CtpInfo->Collector;
	
	/*The mote is capable of getting the mote-ID of its parent in the collection tree*/
	App.Packet->Sender;
	
	/*AMSend is the interface for sending active messages when using protocols that are not address-free, like the serial communication
	(see TinyOS - TEP 119) stack*/
	App.AMSend->SerialSender;
	
	/*This is for the mote to estimate the quality of the link with its neighbors*/
	App.LinkEstimator->CtpP.LinkEstimator;
	
	/*The component QueueC provides the Queue interface to the application*/
	App.Queue->Queue;
}
