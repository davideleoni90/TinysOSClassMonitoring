#include "Acceleration.h"
configuration AdxlAppC {

}
implementation {
	components MainC, AdxlC as App;
	App->MainC.Boot;

	components LedsC;
	App.Leds->LedsC;

	components new TimerMilliC() as TimerBootC;
	App.TimerBoot->TimerBootC;

	components new TimerMilliC() as TimerAccel;
	App.TimerAccel->TimerAccel;
	
	//components new TimerMilliC() as TimerInact;
	//App.TimerInact->TimerInact;
	
	components PrintfC, SerialStartC;
	
	components ActiveMessageC;
	
	components CtpP;
	
	components new CollectionSenderC(CTP_COLLECTION_ID) as Sender;
	
	components CollectionC as Collector;
	
	components RFA1RadioC;
	
	components RFA1RadioP;
	
	components new ADXL345C();
	App.Zaxis->ADXL345C.Z;
	App.Yaxis->ADXL345C.Y;
	App.Xaxis->ADXL345C.X;
	/*App.Register->ADXL345C.Register;
	App.Register2->ADXL345C.Register;*/
	App.AccelControl->ADXL345C.SplitControl;
	App.RadioControl->ActiveMessageC.SplitControl;
	//App.ADXL345Control->ADXL345C.ADXL345Control;
	App.Send->Sender;
	App.Packet->Sender;
	App.Snoop->Collector.Snoop[CTP_COLLECTION_ID];
	App.Receive->Collector.Receive[CTP_COLLECTION_ID];
	App.Intercept->Collector.Intercept[CTP_COLLECTION_ID];
	App.CtpInfo->Collector;
	App.LinkEstimator->CtpP;
	//Collector.CollectionDebug->App.CollectionDebug;
	//CtpP.CollectionDebug->App.CollectionDebug;
	App.StdControl->Collector.StdControl;
	App.RadioChannel->RFA1RadioC;
	//App.SoftwareAckConfig->RFA1RadioP;
}
