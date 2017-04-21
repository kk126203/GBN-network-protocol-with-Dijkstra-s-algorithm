To compile the java scripts , run javac on each part 
$ javac gbnnode.java
$ javac dvnode.java
$ javac cnnode.java, so that we could start our program.


In part 2, we protect the integrity of the sending message by applying GBN protocol. No matter how hard the packets or acks were lost, the final message received on the other side will remain identical to the original sent one.

In part 3, we use bellmond - ford algorithm to compute the shortest distance to every other nodes for each node in the graph. Thanks to Bellmond-ford, we can also obtain the route from one node to the destination if following the next hop.

In part 4, this is the trickest part, we combine part2 and part3 to create a distance vector table for each node. At the first step, we send out packets to the neihgbor nodes, and by calculating the loss rate, we could know which edge has better tranmmition quality. Then we use Bellmod-ford again to compute the shortest route (smallest loo rate) for each node. It is not until we finished sending all the packets did the update of the distance vector tabel stopped.



Algorithms : Bellmond-ford, incode and decode Hashmap.

Data structre : Arraylist for buffer and window in part2. Hashmap for distance table in part3.  In part4 , Arraylist for the GBN's window, Hashmap for the storage of distance table and the current status of each node in the graph.



Test Case : 

Part2 : run the following command 

$ java gbnnode 1111 2222 5 -p 0.1
$ java gbnnode 2222 1111 5 -p 0.1

in 1111's shell, run 

node> send abcdefgh
[1492754564.093] packet 0 a sent
[1492754564.108] ACK0 discarded
[1492754564.115] packet 1 b sent
[1492754564.117] ACK1 discarded
	.
	.
	.
	.
[Summary] 1/9 packets dropped, loss rate = 0.1111111111111111
node> send Hello world 
[1492754614.078] packet 7 H sent
[1492754614.080] ACK7 recieved, window moves to 8
[1492754614.091] packet 8 e sent
	.
	.
	.
	.
node> [Summary] 6/24 packets discarded, loss rate = 0.25
node>           

-------------------------------------------Part2 test pass !

Part3 : run the following command 
$ java dvnode 1111 2222 0.1 3333 0.5
$ java dvnode 3333 1111 0.5 2222 0.2 4444 0.5
$ java dvnode 2222 1111 0.1 3333 0.2 4444 0.8
$ java dvnode 4444 2222 0.8 3333 0.5 last


and the final output of each node's distance vector

[1492754964.499] Node 1111 Routing Table
- (0.1) -> Node 2222
- (0.8) -> Node 4444; Next hop -> Node 2222
- (0.3) -> Node 3333; Next hop -> Node 2222

[1492754964.495] Node 2222 Routing Table
- (0.7) -> Node 4444; Next hop -> Node 3333
- (0.1) -> Node 1111
- (0.2) -> Node 3333

[1492754964.494] Node 3333 Routing Table
- (0.2) -> Node 2222
- (0.5) -> Node 4444
- (0.3) -> Node 1111; Next hop -> Node 2222

[1492754964.513] Node 4444 Routing Table
- (0.7) -> Node 2222; Next hop -> Node 3333
- (0.8) -> Node 1111; Next hop -> Node 2222
- (0.5) -> Node 3333

The outputs distance are very close to the input loss rate and next hops are corresponding to what it should be

---------------------------------------------Part3 test pass !

Part 3 : run the following command 
$ java cnnode 1111 receive send 2222 3333
$ java cnnode 2222 receive 1111 .1 send 3333 4444
$ java cnnode 3333 receive 1111 .5 2222 .2 send 4444
$ java cnnode 4444 receive 2222 .8 3333 .5 send last


Then the final output distance vector for each node appeared on shells

[1492755424.401] Node 1111 Routing Table
- (0.05) -> Node 2222
- (0.59) -> Node 4444; Next hop -> Node 3333
- (0.18) -> Node 3333; Next hop -> Node 2222

[1492755409.405] Node 2222 Routing Table
- (0.54) -> Node 4444; Next hop -> Node 3333
- (0.05) -> Node 1111
- (0.13) -> Node 3333

[1492755409.405] Node 3333 Routing Table
- (0.13) -> Node 2222
- (0.41) -> Node 4444
- (0.18) -> Node 1111; Next hop -> Node 2222

[1492755409.407] Node 4444 Routing Table
- (0.54) -> Node 2222; Next hop -> Node 3333
- (0.59) -> Node 1111; Next hop -> Node 2222
- (0.41) -> Node 3333; Next hop -> Node 2222

The shortest distance is very close to what it should be according to the input loss rate, and the next hops were also shown expectedly, based on my own calculation of the graph.

-----------------------------------------------Part4 test pass!



