import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

class gbnnode extends Thread
{
	static DatagramSocket node ;
	static int self, peer, size, drop;
	static double time;
	static InetAddress Ip;
	static boolean flag;
	static List<Character> window;
	static gbnnode t;
	static boolean d, p, re;
	static ReentrantLock lock ;
	static double pro;
	public static void main(String args[])
	{
		if(args.length!=5){
			System.out.println("Wrong argc size");
			System.exit(0);
		} 
		size = Integer.parseInt(args[2]);
		if(args[3].equals("-d")) {
			if (args[4].equals("0")){
				drop = Integer.MAX_VALUE;
			}
			else drop = Integer.parseInt(args[4]);
			if(drop<0){
				System.out.println("invalid number");
				System.exit(0);
			}
			d = true;
			re = false;
		}
		else if(args[3].equals("-p")){
			if(args[4].charAt(0)-'0'<0||args[4].charAt(0)-'0'>1)
			{
				System.out.println("invalid probability");
				System.exit(0);
			}
			p = true;
			pro = Double.parseDouble(args[4]);
			re = false;
		}
		lock = new ReentrantLock();
		window = new ArrayList<Character>();
		flag = true;
		t = new gbnnode();
		t.start();
		List<Character> buf = new ArrayList<Character>();
		int num  = 0, cur_num = 0, index = 0, wait_sum=0;
		try{	
			self = Integer.parseInt(args[0]);
			peer = Integer.parseInt(args[1]);
			node = new DatagramSocket(self);
			Ip = InetAddress.getByName("localhost");
			DatagramPacket packet;
			byte[] sendData;
			while(true)
			{
			   sendData = new byte[1024];
			   System.out.print("node> ");	
			   BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			   String sentence  = in.readLine();
			   if(sentence.length()<6||!sentence.substring(0,5).equals("send ")){
				System.out.println("invalid format");
				continue;
			   }
			   String data = sentence.substring(5,sentence.length());
			   char[] arr = data.toCharArray();
			   for(char c : arr){
				buf.add(c);
				num++;
			   }
			   int len = 0;
			   while(len<buf.size()){
				char c = buf.get(len);
				int ten = 1, i = 1;
				String to_send;
				lock.lock();
				if(window.size()<size){
					while(ten*10<=index){
				    	ten = ten*10;
				    	i++;
					}
					to_send = i+String.valueOf(index)+c;
					sendData = to_send.getBytes();
					packet = new DatagramPacket(sendData,sendData.length,Ip,peer);
					window.add(c);
					cur_num++;
					time = (double)System.currentTimeMillis()/1000.0;
					System.out.printf("[%.3f] packet %d %c sent\n",time,index,c);
					node.send(packet);
					index++;
					len++;
				}
				if(window.size()==size)
				{	
					try{
						long time_1, time_2 ;
						synchronized(t){
						time_1 = System.currentTimeMillis();
						t.wait(500);
						time_2 = System.currentTimeMillis();
						}
						if(time_2-time_1>=500){
							int index_to_sent = index-window.size();
							for(char cc : window)
							{
								ten = 1; i = 1;
								while(ten*10<=index_to_sent){
									ten = ten*10;
									i++;
								}
								to_send = i+String.valueOf(index_to_sent)+cc;
								sendData = to_send.getBytes();
								packet = new DatagramPacket(sendData,sendData.length, Ip,peer);
								node.send(packet);
								time = (double)System.currentTimeMillis()/1000.0;
								System.out.printf("[%.3f] resend packet %d %c\n",time,index_to_sent,cc);
								index_to_sent++;	
							}
						}
					}catch(Exception e){e.printStackTrace();}
				}
				lock.unlock();
				Thread.sleep(10);
				while(cur_num==num)
				{
					if(window.size()==0) break;
					try{
						long time_1, time_2;
						synchronized(t){
						time_1 = System.currentTimeMillis();
						t.wait(500);
						time_2 = System.currentTimeMillis();
						}
						if(time_2-time_1>=500){
							int index_to_sent = index-window.size();
							lock.lock();
							for(char cc : window)
							{
								ten = 1; i = 1;
								while(ten*10<=index_to_sent){
									ten = ten*10;
									i++;
								}
								to_send = i+String.valueOf(index_to_sent)+cc;
								sendData = to_send.getBytes();
								packet = new DatagramPacket(sendData,sendData.length, Ip,peer);
								time = (double)System.currentTimeMillis()/1000.0;
								System.out.printf("[%.3f] resend packet %d %c\n",time,index_to_sent,cc);
								node.send(packet);
								index_to_sent++;	
							}lock.unlock();
						}
					}catch(Exception e){e.printStackTrace();}
			   	}
			   }			
			   buf.clear();
			   cur_num = 0;
			   num = 0;
			   Thread.sleep(400);
			   String to_send2 = "**";
			   byte[] sendDa = new byte[1024];
			   sendDa = to_send2.getBytes();
			   packet = new DatagramPacket(sendDa,sendDa.length, Ip, peer); 
			   node.send(packet);
			}
	    	}
		catch(IOException e)
		{
			System.out.println("Sending error");
		}
		catch(InterruptedException e)
		{
			System.out.println("Thread exception");
		}

	}

	public void run()
	{
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		DatagramPacket sendpacket = null;
		boolean end = false;
		StringBuilder ans = new StringBuilder();
		int win_index = 0, index = 0, pos = 0, pos1 = 0,expect = 0, num, ten, j=0,ack_count = 0,ack_drop_count=0, count = 0, drop_count = 0;
		gbnnode obj = new gbnnode();
		try{
			while(true){
			if(flag){
			Thread.sleep(500);
			flag = false;
			}
			DatagramPacket receive = new DatagramPacket(receiveData, receiveData.length);		
			node.receive(receive);
			String s = new String(receive.getData());
			if(s.charAt(0)=='A'){
				if(s.charAt(1)=='*'){
					double summ = (double)ack_drop_count/(double)ack_count;
					System.out.println("[Summary] "+ack_drop_count+"/"+ack_count+" packets discarded, loss rate = "+summ);
					System.out.print("node> ");
					continue;
				}
				
				ack_count++;
			    	if(d&&ack_count%drop==0){
					ack_drop_count++;
					time = (double)System.currentTimeMillis()/1000.0;
					ten = s.charAt(1)-'0';
					if(s.charAt(2)=='-') num = -1;
					else num = Integer.parseInt(s.substring(2,2+ten));
					System.out.printf("[%.3f] ACK%d discarded\n",time,num);
					continue;
					}


				if(p){
					boolean qq = exp(pro);
					if(!qq) { 
						ack_drop_count++;
						time = (double)System.currentTimeMillis()/1000.0;
						ten = s.charAt(1)-'0';
						if(s.charAt(2)=='-') num = -1;
						else num = Integer.parseInt(s.substring(2,2+ten));
						System.out.printf("[%.3f] ACK%d discarded\n",time,num);
						continue;
					}
			    	}
				ten = s.charAt(1)-'0';
				if(s.charAt(2)=='-') num = -1;
				else num = Integer.parseInt(s.substring(2,2+ten));
			        time = (double)System.currentTimeMillis()/1000.0;
				String cur = "ACK"+num;
				if(num>=win_index){
				     int small = win_index;
				     win_index = num+1;
				     System.out.printf("[%.3f] %s recieved, window moves to %d\n",time,cur,win_index);
				     lock.lock();
				     for(int i=0 ; i<num-small+1; i++)
				     window.remove(0);
				     lock.unlock();
				     synchronized(t){
					   notify();
				     }
				}
				else{
				     System.out.printf("[%.3f] %s recieved, window moves to %d\n",time,cur,win_index);
				}
			}
			
			else{
			    if(s.substring(0,2).equals("**")){
				double summ = (double)drop_count/(double)count;
				System.out.println("[Summary] "+drop_count+"/"+count+" packets dropped, loss rate = "+summ);
				Thread.sleep(800);
				String ack = "A*";
				sendData = ack.getBytes(); 
			    	sendpacket = new DatagramPacket(sendData, sendData.length, Ip, peer);
			    	node.send(sendpacket);
				String finals = ans.toString();
				System.out.println("node> ");
				ans.setLength(0);
				continue;
			    }
			    
			    count++;
			    if(d&&count%drop==0){ 
				pos = s.charAt(0)-'0'+1;
			   	index = Integer.parseInt(s.substring(1,pos));
				time = (double)System.currentTimeMillis()/1000.0; 
				String data1 = s.substring(pos,pos+1);
			    	System.out.printf("[%.3f] packet %d %s discarded\n",time,index,data1);
				drop_count++;
				continue;
				}
			    if(p){
				boolean qq = exp(pro);
				if(!qq){  
				pos = s.charAt(0)-'0'+1;
			   	index = Integer.parseInt(s.substring(1,pos));
				time = (double)System.currentTimeMillis()/1000.0; 
				String data1 = s.substring(pos,pos+1);
			    	System.out.printf("[%.3f] packet %d %s discarded\n",time,index,data1);
				drop_count++;
				continue;
				}
			    }
			    int show = expect+1;
			    pos = s.charAt(0)-'0'+1;
			    index = Integer.parseInt(s.substring(1,pos));
			    String data = s.substring(pos,pos+1);
			    if(expect!=index){
				int tmp = expect-1;
			    	time = (double)System.currentTimeMillis()/1000.0;
			    	System.out.printf("[%.3f] packet %d %s received\n",time,index,data);
			    	System.out.printf("[%.3f] ACK %d sent, expecting packet %d\n",time,tmp,expect);
		 	    	int i=1 ;ten = 1;
			    	while(tmp>=ten*10){
					ten = ten*10;
					i++;
		            	}
			    	String ack = "A"+i+""+tmp;
			    	sendData = ack.getBytes(); 
			    	sendpacket = new DatagramPacket(sendData, sendData.length, Ip, peer);
			    	node.send(sendpacket);
				}
			    else{
				time = (double)System.currentTimeMillis()/1000.0;
			    	System.out.printf("[%.3f] packet %d %s received\n",time,index,data);
			    	System.out.printf("[%.3f] ACK %d sent, expecting packet %d\n",time,expect,show);
		 	    	int i=1 ;ten = 1;
			    	while(expect>=ten*10){
					ten = ten*10;
					i++;
		            	}
			    	String ack = "A"+i+""+expect;
			    	sendData = ack.getBytes(); 
			    	sendpacket = new DatagramPacket(sendData, sendData.length, Ip, peer);
			    	node.send(sendpacket);
				ans.append(data);
				expect++;
				}
			    }
			}
		}
		catch(IOException e)
		{
			System.out.println("Listening error");
		}
		catch(InterruptedException e)
		{
			System.out.println("Thread exception");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean exp(double probabilityTrue)
	{
	    return Math.random() >= probabilityTrue;
	}
	
}
