#include "Acceleration.h"
configuration NodeAppC {

}
implementation {
	components MainC, NodeC as App;
	App->MainC.Boot;

	components LedsC;
	App.Leds->LedsC;

	components new TimerMilliC() as Timer;
	App.Timer->Timer;
	
	components PrintfC, SerialStartC;
	
	components CollectionC as Collector;
	
	components CtpP;
	
	components ActiveMessageC,SerialActiveMessageC;
	
	components new CollectionSenderC(CTP_COLLECTION_ID) as Sender;
	
	components new SerialAMSenderC(CTP_COLLECTION_ID) as SerialSender;
	
	App.RootReceive->Collector.Receive[CTP_COLLECTION_ID];
	App.Snoop->Collector.Snoop[CTP_COLLECTION_ID];
	App.Intercept->Collector.Intercept[CTP_COLLECTION_ID];
	App.RootControl->Collector;
	App.CollectionPacket->Collector;
	App.RadioControl->ActiveMessageC;
	App.SerialControl->SerialActiveMessageC;
	App.StdControl->Collector;
	App.CtpInfo->Collector;
	App.Send->Sender;
	App.AMSend->SerialSender;
	App.Packet->Sender;
	App.LinkEstimator->CtpP.LinkEstimator;
	//Collector.CollectionDebug->App.CollectionDebug;
}
