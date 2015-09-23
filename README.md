# TinysOsAccelerometer
 <p>
 This application is the result of a didactic project for the 
 <a href="http://ru1.cti.gr/~ichatz/index.php/Site/PervasiveSystems">
 Pervasive Systems</a> course of the Master of Science of
 Engineering in Computer Science at <a href="http://cclii.dis.uniroma1.it/?q=it/msecs">Sapienza University of Rome</a></p>
 
 <h2>Overview</h2>
 <p>The goal of the project is to create a wireless sensors network (WSN) for collecting data from an accelerometer and storing them to a server<br><br>
 The network is made of a number of <i>motes</i>, small electronic devices capable of doing some processing and communicating one another by mean of a radio transceiver.
 The solution adopted to reach the goal comprises two main parts:
 <ol type="1">
 <li> a TinyOS-based application, running on every single node of the network
 <li> a Java GUI application in charge of uploading data to a server
 </ol>  
 </p>
 <h2>Architecture</h2>
 <p> The network is based on the <i>Collection Tree Protocol (CTP)</i>:nodes organize themselves to form a <i>tree-shaped</i> network, where packets are transmitted from the leaf node to the root node. In particular, every node maintains its own routing table
 through which its capable of estimate the quality of the link with all its neighbors, so it's always capable of estimating the path which involves less hops before a packet is delivered to the root;
 periodically every node sends broadcast packets (beacons), waiting for the other nodes to acknowledge them in order to discover the quality of the link with them.
 For this project the architecture of the network is as follows: 
 <ul>
 <li> <u>1 leaf node</u>: interacts with the accelerometer in order to sample data from it and then sends them towards the root of the network</li>
 <li> <u>1 root node </u>: the recipient of the data sent by the leaf; it's connected to the serial port of a pc</li>
 <li> <u><i>n</i> intermediate nodes</u>: they are in charge of forwarding data received from one neighbor node to another neighbor node on the path to the root</li>
 </ul>
 The root node interacts with a pc which is connected to the Internet: the Java application running on it uploads data to a server as it receives them from the serial port.
 </p>
 <h2>Hardware implementation</h2>
 <p>
 The accelerometer used in this project is the <a href="http://www.analog.com/en/products/mems/mems-accelerometers/adxl345.html#product-overview">ADXL-345 by AnalogDevices</a>, a 3-axis accelerometer with 13-bit resolution, up to +/-16g peaks of acceleration. 
 The leaf node to which it's connected is a <a href="http://www.wsense.it/?p=158">MagoNode Platform</a>, while the
 root node and the various messengers are represented by <a href="https://www.google.it/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjEtLLIz4vIAhVI_nIKHcQuBMI&url=http%3A%2F%2Fwww.willow.co.uk%2FTelosB_Datasheet.pdf&usg=AFQjCNEdsZ8RCsxFTT5e4otj-0cxDVyjfA&sig2=aCFXqqXgc4FxPS4z-ZtR3w">Crossbow TelosB</a>   
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
 the application interacts with the root node by mean of a serial port,so as messages are received the representation of the topology of the network gets updated.
 <img src="https://github.com/kimi1490/TinysOsAccelerometer/blob/master/Images/graph.jpg" alt="graph">
 <br>
 Thus the Java application included in this project represents an extension of "mviz" which provides it with two additional features:
 <ol type="1">
 <li>Data upload on a database created at <a href="www.parse.com">Parse.com</a></li>
 <br><br>
 <img src="https://github.com/kimi1490/TinysOsAccelerometer/blob/master/Images/parse.jpg" alt="parse upload">
 <br><br>
 <li>Data download of the last three uploaded value, from the database</li>
 <br><br>
 <img src="https://github.com/kimi1490/TinysOsAccelerometer/blob/master/Images/download.jpg" alt="parse download">
 <br><br>
 </ol>
 </p>
  
 
 

