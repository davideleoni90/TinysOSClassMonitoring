COMPONENT=AdxlAppC
CFLAGS += -I$(TOSDIR)/support/sdk/java/net/tinyos/tools
CFLAGS += -I$(TOSDIR)/lib/net \
          -I$(TOSDIR)/lib/net/4bitle \
          -I$(TOSDIR)/lib/net/ctp
CFLAGS+=-I$(TOSDIR)/lib/printf
CFLAGS+=-I./adxl
CFLAGS+=-I./adxl345
CFLAGS+=-I./..
BUILD_EXTRA_DEPS = JavaMsg.class
CLEAN_EXTRA = *.class *.java

JavaMsg.java: ../Acceleration.h
	mig -target=null -java-classname=JavaMsg java ../Acceleration.h acceleration_msg -o $@

JavaMsg.class: JavaMsg.java
	javac JavaMsg.java

include $(MAKERULES)
