#include "printf.h"
#include "ADXL345.h"
#include "Acceleration.h"
module AdxlC {
	uses interface Boot;
	uses interface Leds;
	uses interface Timer<TMilli> as TimerAccel;
	uses interface Timer<TMilli> as TimerBoot;
	uses interface Read<uint16_t> as Zaxis;
	uses interface Read<uint16_t> as Yaxis;
	uses interface Read<uint16_t> as Xaxis;
	//uses interface Read<uint8_t> as Register;
	//uses interface Read<uint8_t> as Register2;
	uses interface Send;
	uses interface Packet;
	uses interface Receive as Snoop;
	uses interface Receive;
	uses interface SplitControl as AccelControl;
	uses interface StdControl;
	uses interface SplitControl as RadioControl;
	uses interface CtpInfo;
	uses interface LinkEstimator;
	uses interface Intercept;
	//uses interface ADXL345Control;
	uses interface RadioChannel;
	//uses interface SoftwareAckConfig;
	//provides interface CollectionDebug;
}
implementation {

	/*uint8_t registerToRead=0x00;
	uint32_t elapsed=0;
	uint8_t current=0;
	bool fifo=FALSE;*/
	message_t buffer;
	uint16_t xAcceleration=0;
	uint16_t yAcceleration=0;
	uint16_t zAcceleration=0;
	bool busySending=FALSE;
	error_t radioOn=EOFF;
	am_addr_t parent;
	uint16_t quality;
	uint16_t value;
	
	task void startAcc();
	
	task void startRadio();

	event void Boot.booted() {
		call TimerBoot.startOneShot(1000);
	}

	event void TimerBoot.fired() {
		post startAcc();
		post startRadio();
	}

	event void TimerAccel.fired() {
	/*if(!fifo){
		printf("try:%x",registerToRead);
		printfflush();
		//call ADXL345Control.setReadAddress(registerToRead);
		}
		else{
		//call ADXL345Control.setReadAddress(0x39);
		}*/
		call Xaxis.read();
	}
	
	

	event void AccelControl.startDone(error_t err) {
		if(err == SUCCESS) {
			printf("Accelerometer Started\n");
			call TimerAccel.startPeriodic(2000);
		}
		printfflush();

	}

	task void startAcc() {
		call AccelControl.start();
	}
	
	task void startRadio() {
		call RadioControl.start();
		call StdControl.start();
	}

	event void AccelControl.stopDone(error_t err) {

	}
	
	event void RadioControl.stopDone(error_t err) {

	}
	
	event void RadioControl.startDone(error_t err){
		radioOn=err;
		if(err!=SUCCESS){
			printf("Could not initialize the radio");
			printfflush();
		}
		else{
			/*set the same channel as the Telosb*/
			call RadioChannel.setChannel(26);
		}
	}
	
	event void RadioChannel.setChannelDone(){
		printf("SETTING DONE");
		printfflush();
		call Leds.led1On();
	}

	/*event void Register.readDone(error_t result, uint8_t data) {
		if(result==SUCCESS){
		if(!fifo){
		elapsed+=call TimerAccel.getdt();
		printf("time: %li\n",elapsed); 
		if(elapsed<=15000){
			switch(registerToRead){
				case ADXL345_ADDRESS:{
					printf("Register %04x:%d\n",registerToRead,data);
					printfflush();
					registerToRead=ADXL345_DEVID;
					break;
				}
				case ADXL345_DEVID:{
					printf("Register %04x:%d\n",registerToRead,data);
					printfflush();
					registerToRead=ADXL345_THRESH_TAP;
					break;
				}
				default:{
					printf("Register %04x:%d\n",registerToRead,data);
					printfflush();
					if(elapsed!=15000){
					registerToRead++;
					}
					break;
				}
			}
			}
			else{
			printf("stop");
			printfflush();
			if(current==0){
			call TimerAccel.stop();
			call Xaxis.read();
			}
			}
		}
		else{
		printf("FIFO:%d",data);
		}
		}
	}
	
	event void Register2.readDone(error_t result, uint8_t data) {
		if(current!=0){
		printf("Register2:%d\n",data);
		}
	}	

	event void ADXL345Control.setRangeDone(error_t error) {
	}

	event void ADXL345Control.setInterruptsDone(error_t error) {
	}

	event void ADXL345Control.setIntMapDone(error_t error) {
	}

	event void ADXL345Control.setRegisterDone(error_t error) {
		if(error == SUCCESS) {
			printf("Register succesfully set");
			fifo=TRUE;
			call TimerAccel.startOneShot(30000);
		}
	}

	event void ADXL345Control.setDurationDone(error_t error) {
	}

	event void ADXL345Control.setLatentDone(error_t error) {
	}

	event void ADXL345Control.setWindowDone(error_t error) {
	}

	event void ADXL345Control.setReadAddressDone(error_t error) {
		if(error==SUCCESS){
			if(current==0){
			call Register.read();
			}
			else{
			printf("corrente:%d\n",current);
			printfflush();
			call Register2.read();
			}
		}
		else{
			printf("stop");
			printfflush();
			call TimerAccel.stop();
			call Xaxis.read();
		}
	}*/

	event void Xaxis.readDone(error_t result, uint16_t data) {
		printf("X (%d) ", data);
		xAcceleration=data;
		call Yaxis.read();
	}

	event void Yaxis.readDone(error_t result, uint16_t data) {
		printf("Y (%d) ", data);
		yAcceleration=data;
		call Zaxis.read();
	}

	event void Zaxis.readDone(error_t result, uint16_t data) {
		if(radioOn==SUCCESS && call RadioChannel.getChannel()==26){
			if(!busySending){
				acceleration_msg_t* payload=(acceleration_msg_t*)call Send.getPayload(&buffer,sizeof(acceleration_msg_t));
				printf("Z (%d) \n", data);
				//printf("ACK TIME:%d",call SoftwareAckConfig.getAckTimeout());
				if(payload!=NULL){
					payload->x_acceleration=xAcceleration;
					payload->y_acceleration=yAcceleration;
					payload->z_acceleration=data;
					payload->origin=TOS_NODE_ID;
					call CtpInfo.getParent(&parent);
					printf("PARENT:%d",parent);
					payload->link_path_addr=parent;
					call CtpInfo.getEtx(&quality);
					payload->quality=quality;
					payload->link_path_value=call LinkEstimator.getLinkQuality(payload->link_path_addr);
					if((call Send.send(&buffer, sizeof(acceleration_msg_t)))==SUCCESS){
						busySending=TRUE;
					}
					else{
						printf("Error!");
						call Leds.led0On();
					}
				}	
			}
		}
		//call ADXL345Control.setRegister(current,64);
		printfflush();
	}
	
	event void Send.sendDone(message_t* msg, error_t error){
		printf("RESPONSE:%d",error);
		/*if(error==SUCCESS){
			printf("Message correctly sent");
			printf("X:%d",((acceleration_msg_t*)msg)->x_acceleration);
			printf("Y:%d",((acceleration_msg_t*)msg)->y_acceleration);
			printf("Z:%d",((acceleration_msg_t*)msg)->z_acceleration);
			call Leds.led2On();
		}
		else{
			printf("An error occurred while sending message");
			call Leds.led1Off();
			call Leds.led0On();
			
		}*/
		busySending=FALSE;
		printfflush();
	}
	
	/*command error_t CollectionDebug.logEvent(uint8_t type){
		printf("EVENT:%d\n",type);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventSimple(uint8_t type, uint16_t arg){
		printf("SIMPLE EVENT:%u ARG:%u\n",type,arg);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventDbg(uint8_t type, uint16_t arg1, uint16_t arg2, uint16_t arg3){
		printf("DBG EVENT:%u\n",type);
		printf("ARG1:%u\n",arg1);
		printf("ARG2:%u\n",arg2);
		printf("ARG3:%u\n",arg3);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventMsg(uint8_t type, uint16_t msg, am_addr_t origin, am_addr_t node){
		printf("LOG EVENT:%u\n",type);
		printf("MSG:%u\n",msg);
		printf("ORIGIN:%u\n",origin);
		printf("NODE:%u\n",node);
		printfflush();
		return SUCCESS;
	}
	
	command error_t CollectionDebug.logEventRoute(uint8_t type, am_addr_t parent, uint8_t hopcount, uint16_t metric){
		printf("LOG EVENT:%u\n",type);
		printf("PARENT:%u\n",parent);
		printf("HOP COUNT:%u\n",hopcount);
		printf("METRIC:%u\n",metric);
		printfflush();
		return SUCCESS;
	}*/
	
	event message_t* Receive.receive(message_t* msg, void* payload, uint8_t len){
		acceleration_msg_t* res=(acceleration_msg_t*)(payload);
		printf("X RESULT %d",res->x_acceleration);
		printfflush();
		return msg;
	}
	
	event message_t* Snoop.receive(message_t* msg, void* payload, uint8_t len){
		printf("SNOOP\n");
		printfflush();
		return msg;
	}
	
	event bool Intercept.forward(message_t* msg, void* payload, uint8_t len){
		printf("ACCELEROMETER INTERCEPT\n");
		printfflush();
		return TRUE;
	}
	
	event void LinkEstimator.evicted(am_addr_t neighbor){
		printf("Neighbor evicted");
	}
}
