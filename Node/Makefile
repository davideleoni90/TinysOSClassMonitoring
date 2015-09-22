COMPONENT=NodeAppC
CFLAGS += -I$(TOSDIR)/support/sdk/java/net/tinyos/tools
CFLAGS += -I$(TOSDIR)/lib/net \
          -I$(TOSDIR)/lib/net/4bitle \
          -I$(TOSDIR)/lib/net/ctp
CFLAGS+=-I$(TOSDIR)/lib/printf
CFLAGS+=-I./..
BUILD_EXTRA_DEPS = MVizMsg.class
CLEAN_EXTRA = *.class *.java

MVizMsg.java: ../Acceleration.h
	mig -target=null -java-classname=MVizMsg java ../Acceleration.h acceleration_msg -o $@

MVizMsg.class: MVizMsg.java
	javac MVizMsg.java

include $(MAKERULES)
