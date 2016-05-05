#include "Acceleration.h"

/*Configuration for a mote connected to the accelerometer (Magonode)*/

configuration AdxlAppC {

}
implementation {
	
	/*The application (AdxlC) is wired to MainC in order to boostrap the mote (see "TinyOS - TEP 107")*/
	components MainC, AdxlC as App;
	App->MainC.Boot;
	
	/*Leds are wired for debugging purposes (see Adxl.nc)*/
	components LedsC;
	App.Leds->LedsC;
	
	/*REMOVE components new TimerMilliC() as TimerBootC;
	App.TimerBoot->TimerBootC;*/
	
	/*Data from the accelerometer are sampled every time this timer is fired*/
	components new TimerMilliC() as TimerAccel;
	App.TimerAccel->TimerAccel;
	
	/*In order to trace the execution of the application, logging messages are printed on the screen (in case the mote is connected
	to a pc via serial port) thanks to the command "printf". Hence PrintfC has to be wired, as well as SerialStartC, and the latter starts 
	the serial port */
	components PrintfC, SerialStartC;
	
	/*This HIL component provides the interface SplitControl to turn on the radio subsystem (see "TinyOS - TEP 116")*/
	components ActiveMessageC;
	
	/*CtpP provides the interface LinkEstimator, necessary to estimate the bi-directional link quality between two nodes*/
	components CtpP;
	
	/*The mote keeps an internal FIFO queue where it stores all the packets to send (the length of this queue is determined by the variable
	"SEND_QUEUE_DEPTH" in the header file Acceleration.h): in this way, in case it is temporarely not possible to send a packet through
	the radio, still it's not lost (unless the buffer queue is full) and will be sent later as soon as radio is available again*/
	components new QueueC(acceleration_msg_t*, SEND_QUEUE_DEPTH) as Queue;
	
	/*The Collection Tree Protocol provided by TinyOS comprises a set of generic components: CollectionSenderC is the virtualized
	abstraction for sending messages to root nodes; "CTP_COLLECTION_ID" marks the messages involved in this communication protocol
	(see TinyOS - TEP 110 and 119)*/
	components new CollectionSenderC(CTP_COLLECTION_ID) as Sender;
	
	/*CollectionC provides the interface StdControl to start the CTP subsystem*/
	components CollectionC as Collector;
	
	/*HPL component to manage the radio chip At-mega128RFA1 (RFA1), which is present in the Magonode Platform*/
	components RFA1RadioC;
	
	/*the driver of the accelerometer ADXL-345*/
	components new ADXL345C();
	
	/*Read acceleration along X,Y and Z axes of the accelerometer*/
	App.Zaxis->ADXL345C.Z;
	App.Yaxis->ADXL345C.Y;
	App.Xaxis->ADXL345C.X;
	
	/*Start the accelerometer*/
	App.AccelControl->ADXL345C.SplitControl;
	
	/*Start the radio subsystem*/
	App.RadioControl->ActiveMessageC.SplitControl;
	
	/*Start the Collection Tree Protocol subsystem*/
	App.StdControl->Collector.StdControl;
	
	/*Now the mote is capable of sending messages to the root of the network*/
	App.Send->Sender;
	App.Packet->Sender;
	
	/*The mote is capable of getting the mote-ID of its parent in the collection tree*/
	App.CtpInfo->Collector;
	
	/*This is for the mote to estimate the quality of the link with its neighbors*/
	App.LinkEstimator->CtpP;
	
	/*Magonode features the Atmel’s Atmega128RFA1, so RFA1RadioC is the configuration for the management of its transceiver*/
	App.RadioChannel->RFA1RadioC;
	
	/*The component QueueC provides the Queue interface to the application*/
	App.Queue->Queue;
}
