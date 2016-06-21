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
  <p align="center">> Thanks to this tool it is possible to monitor a class and this is achieved in two steps:
    <ol type="1">
      <li> DATA COLLECTION: a wireless sensors network (WSN) continuously acquires data about movements of the people attending lectures</li>
      <li> DATA VISUALIZATION AND STORAGE: a Java application collects data from the network, gives a graphical representation of the topology
        of the network (as it evolves over time) and upload data to an online repository.</li>
    </ol>
    <br><br>
    <div style="text-align:center"><img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/architecture.jpeg" alt="Architecture"></div>
    <br><br>
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
  <li><i>producer</i>: if a mote is provided with an accelerometer, its task consists in acquiring values of acceleration from it, as
    people move on their seats, and then communicating them to the other nodes of the network</li>
  <li><i>forwarder</i> if a mote is not provided with an accelerometer, it can only receive messages from producers and forward them to either
    other motes or to the <i>root</i> of the network, namely the node which is connected to the host running the Java applet (that is in charge of
    work on the data collected)</li>
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
  <li><i>producers</i> are given the driver code for the accelerometer, thanks to which they sample values of acceleration with a periodicity which is read
  from an external configuration file</li>
  <li>among <i>forwarders</i>, one claims to be the <i>root</i> of the network if its ID matches the ID defined for the root mote in the configuration file;
  this is also provided with the default TinyOs implementation of the serial communication stack, in order for it to communicate the data received from the network
  to the host using its serial port</li>
</ul>
</p>
<h3>Hardware implementation</h3>
<p>
  The accelerometer used in this project is the <a href="http://www.analog.com/en/products/mems/mems-accelerometers/adxl345.html#product-overview">ADXL-345 by AnalogDevices</a>, a 3-axis accelerometer with 13-bit resolution, up to +/-16g peaks of acceleration.
  Producers featuring this sensor are <a href="http://www.wsense.it/?p=158">MagoNode Platform</a>, while the root node and the forwarders used are <a href="https://www.google.it/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjEtLLIz4vIAhVI_nIKHcQuBMI&url=http%3A%2F%2Fwww.willow.co.uk%2FTelosB_Datasheet.pdf&usg=AFQjCNEdsZ8RCsxFTT5e4otj-0cxDVyjfA&sig2=aCFXqqXgc4FxPS4z-ZtR3w">Crossbow TelosB</a>
</p>
<h2>Data visualization and storage</h2>
<h3>Description</h3>
<p>
  Once the measures from the accelerometers have been successfully collected by the root of the sensors network, a Java application is in charge of presenting them to the user in such
  a way that it's possible for him to carry on analytics and draw conclusion regarding learning. Also this application has to support the monitoring of the network itself, providing
  information useful to detect, for example, if a certain accelerometer is no longer working.
</p>
<h3>Software implementation</h3>
<p>
  The release of TinyOs contains an application named "Mviz" which shows how it's possibile to implement the Collection Tree Protocol (CTP): when
  it is installed on a number of motes, they organize themselves into a tree-shaped network and send values sampled from their light sensor  to the
  root node. The release also features a Java SDK which can be used to build java applications that interact with TinyOs-based applications: one of
  them, "mviz", is the java counterpart of "Mviz" and is specifically designed to give a graphical representation of the nodes and links composing
  a sensors network that implements CTP.
  In this project, data visualization and storage is delegated to a Java application, built on top of the Java SDK of TinyOs and inspired by the sample application "mviz".
  They share the same main task: to parse raw sequences of bytes (messages) sent by the motes and to draw the topology of the sensors network in its current state,
  deriving it from the set of messages received.
  There still differences among "mviz" and the customized version included in this solution though:
  <ol type="1">
    <li> while mviz is "general-purpose", in the sense that it supports any structure for the the payload of messages sent by the motes, its customized
       version is meant to work only with a specific TinyOs application (see above): this makes it way more easier for the customized version to parse
       messages received from the network, since it's aware of their structure
    </li>
    <li>mviz is meant to interact with a network where all the motes feature a sensor, so that all of them is supposed to be the source of at least one
    message sent through the network, but this is not the case in the scenario presented above for this project (see forwarders). This makes it harder
    to track forwarders (not only producers), but the proble is solved in the customized version thanks to the fact that all motes append their ID to the payload of every
    message when they process it
  </li>
    <li>
      Since one of the goals of this project is keeping tracks of data collected, not only showing them, this customized version of mviz uploads all <thead>
        measures of acceleration to a online repository, from which they can also be retrieved after
    </li>
    <li>
      Finally, this customized version of mviz is "path-aware", namely it is able to keep track, for each producer, the list of forwarders thanks to which,
      following the Collection Tree Protocol, a message is delivered to the root mote (and then to the host)
    </li>
    </ol>
    This customized version of mviz relies on three pieces of information contained in the payload of every message received by the root mote:
    <ol type="1">
      <li><i>message_path</i>: an array of integer, where each element is the ID of a mote that handled the message, either as producer or as forwarder,
        on its path towards the root node
        <li><i>path_quality</i>: another array of integer, where each element is the quality of the link between two following motes along the path of
        the message towards the root node</li>
        <li><i>hopcount</i>: since the maximum size for the two above arrays has to be defined in advance (see header "Acceleration.h"), every time a mote
          handles a message it increments this field, which act as a counter, so that then it's possible to understand the exact number of nodes alogn the
        path</li>
    </ol>
    Combining these three fields, for each message it is possible to get the list of motes and links it passed through, together with the quality of all
      links themselves: all this makes it possible to provide the following services.
    <h3>Links Viewer</h3>
    <p>
    A table containing a description of all the links in the network: the ID of the two motes connected by the link and the quality of the link. The last
    value corresponds to the probability that a message sent by one vertex of the link is received at the other vertex and it's computed as a function of
    link quality estimated by both the involved motes. That's why, if a message is sent from nodes A to B and then from B to A, the table keeps a row for
    the two links, but with the same value of "Quality" updated to the last message received that crossed that link.
    <br>
    <div style="text-align:center"><img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/LinksViewer.jpeg" alt="Links Viewer"></div>
  </p>
    <h3>Measures Table</h3>
    <p>
      Every time a message is received, values of acceleration, ID of the producer and timestemp are uploaded to a online repository: the chosen one is <a href="http://www.parse.com">Parse</a>,
      mostly because it's free and APIs are really easy to use (they are simple REST calls). It's possible to retrieve at any moment any number of the last measures uploaded on Parse.
      </p>
    <div style="text-align:center"><img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/MeasuresTable.jpeg" alt="Measures Table"><div>
  <h3>Paths Table and Graph</h3>
  <p>
  The main part of the whole application is the graphical representation of the sensors network, connected to the Paths Table. All the motes are drawn using an
  icon (as regards with producers, this has also a red circle around it) and the links among them are drawn with different colors depending on the currenly
  selected rows in the adjacent Paths Table. Here there's one row for each producer mote, with the indication of the number of forwarders that processed the
  last message received from the producer itself (this value gets updated every time a new message from the same mote is received).</p>
  <div style="text-align:center"><img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/PathsTable.jpeg" alt="Paths Table"></div>
  <div style="text-align:center"><img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/Canvas.jpeg" alt="Canvas"></div>
