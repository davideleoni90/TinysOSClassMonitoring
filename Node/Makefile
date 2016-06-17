COMPONENT=NodeAppC
CFLAGS += -I$(TOSDIR)/support/sdk/java/net/tinyos/tools
CFLAGS += -I$(TOSDIR)/lib/net \
          -I$(TOSDIR)/lib/net/4bitle \
          -I$(TOSDIR)/lib/net/ctp
CFLAGS+=-I$(TOSDIR)/lib/printf
CFLAGS+=-I./..
BUILD_EXTRA_DEPS = SensorsDataMsg.class
CLEAN_EXTRA = *.class *.java

SensorsDataMsg.java: ../Acceleration.h
	mkdir ../node
	mig -target=null -java-classname=node.SensorsDataMsg java ../Acceleration.h acceleration_msg -o ../node/$@

SensorsDataMsg.class: SensorsDataMsg.java
	javac ../node/SensorsDataMsg.java

include $(MAKERULES)
