#include "printf.h"
#include "Acceleration.h"
#include "AM.h"
#include "TinyError.h"
module NodeC{
	uses interface Boot;
	uses interface StdControl;
	uses interface SplitControl as RadioControl;
	uses interface SplitControl as SerialControl;
	uses interface Receive as RootReceive;
	uses interface Receive as Snoop;
	uses interface Send;
	uses interface AMSend;
	uses interface Packet;
	uses interface RootControl;
	uses interface CollectionPacket;
	uses interface Leds;
	uses interface Intercept;
	provides interface CollectionDebug;
	uses interface CtpInfo;
	uses interface LinkEstimator;
	uses interface Timer<TMilli>;
}
implementation{
	
	bool busy=FALSE;
	bool serialBusy=FALSE;
	bool isRoot=FALSE;
	message_t message;
	message_t serialMessage;
	error_t ctpOn;
	error_t sendingToSerial;
	error_t sendingToRadio;
	acceleration_msg_t* measure;
	am_addr_t src;
	uint16_t quality;
	am_addr_t parent;
	uint16_t value;
	
	event void Boot.booted() {
		if(call RadioControl.start()!=SUCCESS){
			printf("Error while starting radio");
			printfflush();
		}
		ctpOn=call StdControl.start();
		if(ctpOn!=SUCCESS){
			printf("Error while starting collection infrastructure");
			printfflush();
		}
	}
	
	event void RadioControl.startDone(error_t error){
		if(error!=SUCCESS){
			printf("Radio initialization failed");
			printfflush();
			return;
		}
		call Leds.led1On();
		while(ctpOn!=SUCCESS){
			call StdControl.start();
		}
		if(TOS_NODE_ID==ROOT_ID && !(call RootControl.isRoot())){
			call Leds.led2On();
			call Leds.led1Off();
			call RootControl.setRoot();
			printf("Is root?%d",call RootControl.isRoot());
		}
		else{
			/*if(TOS_NODE_ID!=4){
				acceleration_msg_t* payload=(acceleration_msg_t*)call Send.getPayload(&message,sizeof(acceleration_msg_t));
				printf("Payload length:%d1n",call Send.maxPayloadLength());
				printf("LENGTH:%d",call Packet.payloadLength(&message));
				payload->x_acceleration=1;
				payload->y_acceleration=2;
				payload->z_acceleration=3;
				call CtpInfo.getParent(&src);
				call Send.send(&message, sizeof(acceleration_msg_t));
				printf("PARENT:%d",src);
				printfflush();
			}*/
		}
		printfflush();
		call Timer.startPeriodic(2000);
	}
	
	event void RadioControl.stopDone(error_t error){
	
	}
	
	event void SerialControl.startDone(error_t err){
	
	}
	
	event void SerialControl.stopDone(error_t err) {

	}
	
	event void Timer.fired(){
		printf("HERE %d",parent);
		if(parent!=NULL){
			call LinkEstimator.getLinkQuality(1);
		}
		if(!busy){
			acceleration_msg_t* Payload=(acceleration_msg_t*)call Send.getPayload(&message,sizeof(acceleration_msg_t));
				if(Payload!=NULL){
					Payload->origin=TOS_NODE_ID;
					call CtpInfo.getEtx(&quality);
					call CtpInfo.getParent(&parent);
					Payload->quality=quality;
					Payload->link_path_addr=parent;
					Payload->link_path_value= call LinkEstimator.getLinkQuality(parent);
					Payload->x_acceleration=1;
					Payload->y_acceleration=2;
					Payload->z_acceleration=3;
					printf("ETX:%d, PARENT:%d, QUALITY:%d",quality,parent,call LinkEstimator.getLinkQuality(parent));
					
					busy=TRUE;
					sendingToRadio=call Send.send(&message,sizeof(acceleration_msg_t));
					printf("SENT TO RADIO?%d",sendingToRadio);
					if(sendingToRadio!=SUCCESS){
						call Leds.led1On();
					}
				}
				else{
					call Leds.led1On();
				}
		}
		printfflush();
	}
	
	event message_t* RootReceive.receive(message_t* msg, void* payload, uint8_t len){
		//printf("Size:%d,%d",len,sizeof(&payload));
		if(len!=sizeof(acceleration_msg_t)){
			printf("Packet not well formed");
			printfflush();
		}
		call Leds.led0Toggle();
		src=call CollectionPacket.getOrigin(msg);
		measure=(acceleration_msg_t*)(payload);
		printf("Origin %d\n",src);
		printf("X received:%d\n",measure->x_acceleration);
		printf("Y received:%d\n",measure->y_acceleration);
		printf("Z received:%d\n",measure->z_acceleration);
		printf("Parent:%d\n",measure->link_path_addr);
		parent=measure->link_path_addr;
		printf("Quality:%d\n",measure->quality);
		printf("Value:%d\n",measure->link_path_value);
		printfflush();
		if(!serialBusy){
			acceleration_msg_t* serialPayload=(acceleration_msg_t*)call AMSend.getPayload(&serialMessage,sizeof(acceleration_msg_t));
				if(payload!=NULL){
					memcpy(serialPayload,(acceleration_msg_t*)payload,sizeof(acceleration_msg_t));
					serialBusy=TRUE;
					sendingToSerial=call AMSend.send(0xffff,&serialMessage,sizeof(acceleration_msg_t));
					if(sendingToSerial!=SUCCESS){
						call Leds.led1On();
					}
				}
				else{
					call Leds.led1On();
				}
		}
		/*if(!busy){
			acceleration_msg_t* Payload=(acceleration_msg_t*)call Send.getPayload(&message,sizeof(acceleration_msg_t));
				if(payload!=NULL){
					((acceleration_msg_t*)payload)->origin=TOS_NODE_ID;
					call CtpInfo.getEtx(&quality);
					call CtpInfo.getParent(&parent);
					((acceleration_msg_t*)payload)->quality=quality;
					((acceleration_msg_t*)payload)->link_path_addr=parent;
					((acceleration_msg_t*)payload)->link_path_value= call LinkEstimator.getLinkQuality(parent);
					printf("ETX:%d, PARENT:%d, QUALITY:%d",quality,parent,call LinkEstimator.getLinkQuality(parent));
					memcpy(Payload,(acceleration_msg_t*)payload,sizeof(acceleration_msg_t));
					busy=TRUE;
					sendingToRadio=call Send.send(&message,sizeof(acceleration_msg_t));
					printf("SENT TO RADIO?%d",sendingToRadio);
					if(sendingToRadio!=SUCCESS){
						call Leds.led1On();
					}
				}
				else{
					call Leds.led1On();
				}
		}*/
		return msg;
	}
	
	event void AMSend.sendDone(message_t* msg, error_t error){
		serialBusy=FALSE;
		call Leds.led1Off();
	}
	
	event void Send.sendDone(message_t* msg, error_t error){
		printf("SEND DONE");
		busy=FALSE;
		printfflush();
	}
	
	event bool Intercept.forward(message_t* msg, void* payload, uint8_t len){
		printf("INTERCEPT\n");
		printfflush();
		return TRUE;
	}
	
	event message_t* Snoop.receive(message_t* msg, void* payload, uint8_t len){
		printf("SNOOP\n");
		printfflush();
		return msg;
	}
	
	event void LinkEstimator.evicted(am_addr_t addr){
		printf("%d EVICTED",addr);
		printfflush();
	}
	
	command error_t CollectionDebug.logEvent(uint8_t type){
		printf("EVENT NODE:%d\n",type);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventSimple(uint8_t type, uint16_t arg){
		printf("SIMPLE EVENT NODE:%u ARG:%u\n",type,arg);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventDbg(uint8_t type, uint16_t arg1, uint16_t arg2, uint16_t arg3){
		printf("DBG EVENT NODE:%u\n",type);
		printf("ARG1:%u\n",arg1);
		printf("ARG2:%u\n",arg2);
		printf("ARG3:%u\n",arg3);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventMsg(uint8_t type, uint16_t msg, am_addr_t origin, am_addr_t node){
		printf("LOG EVENT NODE:%u\n",type);
		printf("MSG:%u\n",msg);
		printf("ORIGIN:%u\n",origin);
		printf("NODE:%u\n",node);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventRoute(uint8_t type, am_addr_t parent, uint8_t hopcount, uint16_t metric){
		printf("ROUTE EVENT NODE:%u\n",type);
		printf("PARENT:%u\n",parent);
		printf("HOP COUNT:%u\n",hopcount);
		printf("METRIC:%u\n",metric);
		printfflush();
		return SUCCESS;
	}
	
}
