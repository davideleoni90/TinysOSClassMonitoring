# TinysOSClassMonitoring
<p>
  This application is the result of a didactic project for the
  <a href="http://ichatz.me/index.php/Site/PervasiveSystems2015">
    Pervasive Systems</a> course of the Master of Science of
    Engineering in Computer Science at <a href="http://cclii.dis.uniroma1.it/?q=it/msecs">Sapienza University of Rome</a></p>

<h2>Motivations</h2>
  <p>The goal of this project is to provide a tool for supporting data-capture within the context of the
      "learning analytics", which means using different techniques and models in order to
      predict and advise on learning. In this case data are values of acceleration along three axes (x,y,z)
      provided by accelerometers installed under the chairs of a classroom: these values can be used to monitor
      the movements of people attending lectures and, ultimately, to get a feedback about the quality of lectures
      themselves. In fact, as many researches have proved, it's possible to find a correlation between the behavior
      of people attending classes and their level of attention. More on "learning analytics" and futher motivations
      for this project can be found <a href="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Project%20Presentation.pdf">here</a>
  </p>
<h2>Architecture</h2>
  <p> This solution achieves class monitoring through two steps:
    <ol type="1">
      <li> DATA CAPTURE: a wireless sensors network (WSN) of motes, some of them featuring an accelerometer,gathers data about movements of the people</li>
      <li> DATA VISUALIZATION AND STORAGE: a Java applet receives data from the network, draws a graph to represent the topology of the network (as it evolves over time) and upload data to an online repository.</li>
    </ol>
    <img src="https://github.com/davideleoni90/TinysOSClassMonitoring/blob/master/Images/architecture.jpeg" alt="Architecture">
  </p>

       <h2>Architecture</h2>
       <p> The network is based on the <i>Collection Tree Protocol (CTP)</i>:nodes organize themselves to form a <i>tree-shaped</i> network, where packets are transmitted from the leaf node to the root node. In particular, every node maintains its own routing table
    with an estimation of the quality of the link to all its neighbors, so it's always capable of determining the path which involves less hops before a packet is delivered to the root;
      periodically every node sends broadcast packets (beacons), waiting for the other nodes to acknowledge them in order to discover the quality of the links.
     For this project the architecture of the network is as follows:
      <ul>
         <li> <u>1 leaf node</u>: interacts with the accelerometer in order to sample data from it and then sends them towards the root of the network. Its mote-id has to be set to "1" in order to correctly update data on Parse(see later)</li>
          <li> <u>1 root node </u>: the recipient of the data sent by the leaf; it's connected to the serial port of a pc</li>
          <li> <u><i>n</i> intermediate nodes</u>: they are in charge of forwarding data received from one neighbor node to another neighbor node on the path to the root</li>
       </ul>
          The root node interacts with a pc which is connected to the Internet: the Java application running on it uploads data to a server as it receives them from the serial port.
        </p>
        <h2>Hardware implementation</h2>
        <p>
          The accelerometer used in this project is the <a href="http://www.analog.com/en/products/mems/mems-accelerometers/adxl345.html#product-overview">ADXL-345 by AnalogDevices</a>, a 3-axis accelerometer with 13-bit resolution, up to +/-16g peaks of acceleration.
          The leaf node to which it's connected is a <a href="http://www.wsense.it/?p=158">MagoNode Platform</a>, while the root node and the various messengers are represented by <a href="https://www.google.it/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjEtLLIz4vIAhVI_nIKHcQuBMI&url=http%3A%2F%2Fwww.willow.co.uk%2FTelosB_Datasheet.pdf&usg=AFQjCNEdsZ8RCsxFTT5e4otj-0cxDVyjfA&sig2=aCFXqqXgc4FxPS4z-ZtR3w">Crossbow TelosB</a>
        </p>
        <h2>Software implementation</h2>
        <h3>TinyOS-based application</h3>
        <p>Since the hardware platform of the leaf node is different from the one of the messenger nodes and of the root node, the TinyOs-based application comes in two distinct flavours:
          <ol type="1">
            <li>Accelerometer: it's the application installed on the Magonode platform (leaf node); it includes the driver to configure the accelerometer, with which it interacts</li>
            <li>Node: this is installed both in all the intermediate nodes and in the root node, meaning that they execute the same code, but actually their behaviour is not the same.
              In fact the root node is assigned a reserved mote id (50,arbitrarly chosen),so that it's possible to make it perform specific tasks by mean of the variable "TOS_NODE_ID"</li>
            </ol>
            Anyway, since they have to communicate one another, all the nodes in the network make use of the same implementation of the CTP provided with the distro of TinyOS.
          </p>
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
