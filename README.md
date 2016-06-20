# TinysOSClassMonitoring
<p>
  This application is the result of a didactic project for the
  <a href="http://ichatz.me/index.php/Site/PervasiveSystems2015">
    Pervasive Systems</a> course of the Master of Science of
    Engineering in Computer Science at <a href="http://cclii.dis.uniroma1.it/?q=it/msecs">Sapienza University of Rome</a>
</p>
<h2>Motivations</h2>
<p>The goal of this project is to provide a tool for collecting information to be used as starting point within the context of
      "learning analytics": this consists in using data and analysis models to predict and advise people's learning.
	  In this case data are values of acceleration along three axes (x,y,z) collected by accelerometers installed under the chairs of a classroom:
	  these values give information about the movements of people attending lectures and, as a consequence, on their level of attention to the lecturer.
	  Many researches have in fact proved the existence of a connection between the way people move while they attend a lecture and their interest about
	  the covered topics. More on "learning analytics" and futher motivations for this project can be found <a href="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Project%20Presentation.pdf">here</a>
  </p>
<h2>Overview</h2>
  <p> Thanks to this tool it is possible to monitor a class and this is achieved in two steps:
    <ol type="1">
      <li> DATA COLLECTION: a wireless sensors network (WSN) continuously acquires data about movements of the people attending lectures</li>
      <li> DATA VISUALIZATION AND STORAGE: a Java applet collects data from the network, gives a graphical representation of the topology
        of the network (as it evolves over time) and upload data to an online repository.</li>
    </ol>
    <img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/architecture.jpeg" alt="Architecture">
  </p>
<h2>Data collection</h2>
<h3>Description</h3>
<p>
The core of the solution is represented by motes, namely the nodes of the WSN. Each of them features some bacic hardware
components (microcontroller, radio transceiver, memory, power source) and is controlled using TinyOs. This is an embedded
operating system designed for sensor networks, which ensures that the custom code written by developers will be executed
in a very efficient way on the motes. Sensor networks are in fact created out of nodes with limited hardware resources
(in terms of memory and power), but still they are supposed to collect data as long as possible without maintenance and failures:
that's why programming the motes in order to optimize the usage of resources turns out to be crucial, and TinyOs greatly simplifies
this task. That's why it has been chosen also for the sensors network deployed in this project (more on TinyOs can be found
<a href="http://tinyos.stanford.edu/tinyos-wiki/index.php/TinyOS_Documentation_Wiki">here</a>).
This solution comprises two TinyOs-based applications, on for each of the roles that a mote can play within the network:
<ol type="1">
  <li><i>producer</i></li>: if a mote is provided with an accelerometer, its task consists in acquiring values of acceleration from it, as
  people move on their seats, and then communicating them to the other nodes of the network
  <li><i>forwarder</i></li> if a mote is not provided with an accelerometer, it can only receive messages from producers and forward them to either
    other motes or to the <i>root</i> of the network, namely the node which is connected to the host running the Java applet (that is in charge of
    work on the data collected)
</ol>
Producers and forwarders form a <i>tree-shaped</i> network based on the <i>Collection Tree Protocol (CTP)</i>: motes cooperate in order for the data
to be delivered to the root mote with the shortest number of hops, i.e. minimizing the number of forwarders in the path from one producer to the root
node. The protocol uses a parameter called "ETX" (expected number fo transmissions) as a gradient, so it always chooses the path from one producer to
the root node that minimizes the value of ETX.
</p>
<h3>Software implementation</h3>
<p>The applications for both the types of motes use the same implementation of the Collection Tree Protocol (CTP) included in the release
of TinyOs; they also make use of the same method (4bitLinkEstimation) to estimate the quality of the links with the neighbor nodes, that is how each
mote chooses the recepient of its messages (containing values of acceleration) among its neighbor nodes. The default implementation under the hood stores
messages in a queue before actually sending them and this solution includes a further queue, so we end up with a two-levels queue. This comes in handy when
the network is temporary not working, because more data from the accelerometer can be stored in the mote until the network is up again and queues are flushed
out forwarding data. The payload of messages created by producers, and forwarded by forwarders, not only contains the measure of acceleration sampled, but also
the list of all the motes that have received the message itself. More precisely, as a mote receives a message, it appends its ID to this list before forwarding
the message; this has to be done to support "data visualization" (see below).
Besides common features, there are differences between the two versions of the code executed by the motes:
<ul>
  <li><i>producers</i></li> are given the driver code for the accelerometer, thanks to which they sample values of acceleration with a periodicity which is read
  from an external configuration file
  <li>among <i>forwarders</i></li>, one claims to be the <i>root</i> of the network if its ID matches the ID defined for the root mote in the configuration file;
  this is also provided with the default TinyOs implementation of the serial communication stack, in order for it to communicate the data received from the network
  to the host using its serial port
</ul>
</p>
<h3>Hardware implementation</h3>
<p>
  The accelerometer used in this project is the <a href="http://www.analog.com/en/products/mems/mems-accelerometers/adxl345.html#product-overview">ADXL-345 by AnalogDevices</a>, a 3-axis accelerometer with 13-bit resolution, up to +/-16g peaks of acceleration.
  Producers featuring this sensor are <a href="http://www.wsense.it/?p=158">MagoNode Platform</a>, while the root node and the forwarders used are <a href="https://www.google.it/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjEtLLIz4vIAhVI_nIKHcQuBMI&url=http%3A%2F%2Fwww.willow.co.uk%2FTelosB_Datasheet.pdf&usg=AFQjCNEdsZ8RCsxFTT5e4otj-0cxDVyjfA&sig2=aCFXqqXgc4FxPS4z-ZtR3w">Crossbow TelosB</a>
</p>
<h2>Data collection</h2>
          <h3>Java GUI application</h3>
          <p>
            TinyOS comes with a Java application named "mviz" which provides a graphical tool to show the links in a network made of nodes running the Collection Tree Protocol:
            the application communicates with the root node through a serial port,so when messages are received the representation of the topology of the network gets updated.
            <img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/graph.jpg" alt="graph">
            <br>
            Thus the Java application included in this project represents an extension of "mviz" which provides it with two additional features:
            <ol type="1">
              <li>Data upload on a database created at <a href="www.parse.com">Parse.com</a>
                Only data extracted from packets whose origin is set to "1" are uploaded (data from the accelerometer)
              </li>
              <br><br>
              <img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/parse.jpg" alt="parse upload">
              <br><br>
              <li>Data download of the last three uploaded value, from the database</li>
              <br><br>
              <img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/download.jpg" alt="parse download">
              <br><br>
            </ol>
            Both these features are implemented using the <a href="https://www.parse.com/docs/rest/guide">REST API</a> available at Parse.com. Since the request
            and the response of this API contain a JSONObject, in order to run the "extended Mviz" it's necessary to import either this <a href="http://www.json.org/">JSON library</a>
            and this <a href="http://hc.apache.org/httpclient-3.x/">HTTP Client</a>.
          </p>
          <h2>Files Description</h2>
          <p>
            The most important files available from this project are:
            <ol>
              <li>TinyOs applications for the motes, i.e. for the Magonode (Accelerometer) and for the TelosB (Node)</li>
              <li>Header file (Acceleration.h) including the fields of the packets to be visualized by the Java GUI application </li>
              <li>Extended version of Mviz</li>
              <li>Make files to compile to magonode (avr folder and magonode.target); to be put in the make directory of the TinyOS installation</li>
            </ol>
          </p>