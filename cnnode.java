import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.math.BigDecimal;						
import java.math.RoundingMode;

public class cnnode extends Thread{

	static DatagramSocket sock = null;
	static int self, already_picked, already_sent, cur_port;
	static Map<Integer,Double> m , parent;
	static Map<Integer,Integer> hop;
	static List<Integer> neibor, child, window;
	static InetAddress Ip;
	static ReentrantLock lock ;
	static cnnode obj;
	static boolean status ,flag1;

	public static void main(String args[])
    	{
		boolean start = false;
		flag1 = true;
		byte[] sendData = new byte[1024];
		byte[] recData = new byte[1024];
		m = new HashMap<Integer,Double>();
		hop = new HashMap<Integer,Integer>();
		neibor = new ArrayList<Integer>();
		parent = new HashMap<Integer,Double>();
		child = new ArrayList<Integer>();
		window = new ArrayList<Integer>();
		obj = new cnnode();
		lock = new ReentrantLock();
		if(args.length<4||!args[1].equals("receive"))
		{
			System.out.println("invalid input 1");
			System.exit(0);
		}
		if(args[args.length-1].equals("last")) start = true;
		int j = 0;
		for( j=2 ; j<args.length ; j+=2){
			if(!Character.isDigit(args[j].charAt(0))) break;
			m.put(Integer.parseInt(args[j]), 1.0);
			neibor.add(Integer.parseInt(args[j]));
			hop.put(Integer.parseInt(args[j]),Integer.parseInt(args[j]));
			parent.put(Integer.parseInt(args[j]),Double.parseDouble(args[j+1]));
		}
		if(!args[j].equals("send")){
                        System.out.println("invalid input");
                        System.exit(0);
                }
		j++;
		for( j=j ; j<args.length ; j++){
			if(args[j].equals("last")) continue;
			m.put(Integer.parseInt(args[j]), 1.0);
                        neibor.add(Integer.parseInt(args[j]));
                        hop.put(Integer.parseInt(args[j]),Integer.parseInt(args[j]));	
			child.add(Integer.parseInt(args[j]));
		}
		self = Integer.parseInt(args[0]);
		try{	
			DatagramPacket receivePacket = new DatagramPacket(recData,recData.length);
			Ip = InetAddress.getByName("localhost");
			sock = new DatagramSocket(self);
			if(start){
				String to_send = obj.convert(self);	
				sendData = to_send.getBytes();
				for(int pt : neibor){
					DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,Ip,pt);
					sock.send(sendPacket);
				}
			}
			else{
				boolean trigger = true, update = false;
				receivePacket = new DatagramPacket(recData, recData.length);
				sock.receive(receivePacket);
				String s = new String( receivePacket.getData());
                                List<String> new_data = (obj.decode(s));
                                int source = Integer.parseInt(new_data.get(0));
				new_data.remove(0);
                                update = obj.check(new_data,source);
                                if(update||trigger){
                                        trigger = false;
                                        String to_send = obj.convert(self);
                                        sendData = to_send.getBytes();
                                        for(int pt : neibor){
                                                DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,Ip,pt);
                                                sock.send(sendPacket);
                                        }
				}
				
			}
			status = true;
			timer t = obj.new timer();
			send_update se = obj.new send_update();
			if(child.size()!=0){
				t.start();
				se.start();
			}
			obj.start(); /*probe packet sending*/

			for(int pt : child){
				cur_port = pt;
				int i = 0;
				double time1 , time2 ;
				DatagramPacket sendPacket = null;
				while(i<10){
					lock.lock();
					if(window.size()<5){
						sendData = new byte[1024];
						String to_send = "p"+i;
						sendData = to_send.getBytes();
						sendPacket = new DatagramPacket(sendData,sendData.length,Ip,pt);
                                       	 	sock.send(sendPacket);
						window.add(i);
						already_sent++;
						i++;
					}
					if(window.size()==5)
					{
						synchronized(obj)
						{
						time1 = (double)System.currentTimeMillis();
						obj.wait(500);
						time2 = (double)System.currentTimeMillis();
						}
						if(time2-time1>480){
							for(int n : window){					
								String to_send = "p"+n;
								sendData = new byte[1024];
								sendData = to_send.getBytes();
								sendPacket = new DatagramPacket(sendData,sendData.length,Ip,pt);	
								sock.send(sendPacket);
								already_sent++;
							}
						}
					}
					lock.unlock();
					Thread.sleep(100);
					while(i==10)
					{
						if(window.size()==0) break;
                                                synchronized(obj)
						{
                                                time1 = (double)System.currentTimeMillis();
                                                obj.wait(500);
                                                time2 = (double)System.currentTimeMillis();
                                                }
                                                if(time2-time1>500){
							lock.lock();
                                                        for(int n : window){
								sendData=  new byte[1024];
                                                                String to_send = "p"+n;
                                                                sendData = to_send.getBytes();
                                                                sendPacket = new DatagramPacket(sendData,sendData
.length,Ip,pt);
								already_sent++;
                                                                sock.send(sendPacket);
                                                        }
							lock.unlock();
                                                }
					}
					double rate = 1.0 - (double)already_picked/(double)already_sent;
					dvnode onj2 = new dvnode();
					rate = obj.round(rate,2);
					lock.lock();
					if(rate==0.0&&i!=10) m.put(pt,1.0);
					else m.put(pt,rate);
					lock.unlock();
				}
				Thread.sleep(100);
				lock.lock();
				already_picked = 0;
				already_sent = 0;
				lock.unlock();
			}
			status = false;
		}catch(Exception e){e.printStackTrace();}
	}
	

	public void run(){
		Map<Integer,Integer> Ack_window = new HashMap<Integer,Integer>();
		for(int n : parent.keySet()){
			Ack_window.put(n,0);
		}
		int parent_port = 0, cur = 0;;
		double pro = 0.0;
		byte[] sendData = new byte[1024];
                byte[] recData = new byte[1024];	
		DatagramPacket receivePacket = null, sendPacket = null;
		String s = null;
		try{
		while(true){
			receivePacket = new DatagramPacket(recData, recData.length);
			sock.receive(receivePacket);
			s = new String( receivePacket.getData());
			if(s.charAt(0)=='p'){
				parent_port = receivePacket.getPort();
				pro = parent.get(parent_port);
				cur = Ack_window.get(parent_port);
				if(!exp(pro)) continue;
				if(s.charAt(1)-'0'==cur){
					String ack = "A"+cur;
					sendData = ack.getBytes();
					sendPacket = new DatagramPacket(sendData, sendData.length, Ip, parent_port);
					lock.lock();
					sock.send(sendPacket);
					lock.unlock();
					Ack_window.put(parent_port, Ack_window.get(parent_port)+1);
				}
				else{
					int tmp = cur - 1;
					String ack = "A"+tmp;
					sendData = ack.getBytes();
					sendPacket = new DatagramPacket(sendData, sendData.length, Ip, parent_port);
                                        lock.lock();
				        sock.send(sendPacket);
					lock.unlock();
				}
				
			}
			else if(s.charAt(0)=='A'){	
				lock.lock();
				already_picked++;
				parent_port = receivePacket.getPort();
				if(s.charAt(1)!='-'&&window.size()!=0&&s.charAt(1)-'0'>=window.get(0)){
					for(int i = window.get(0) ; i<=s.charAt(1)-'0' ; i++){
						window.remove(0);
					}
					synchronized(obj){
						notify();
					}
				}
				lock.unlock();
			}
			else{
                                List<String> new_data = (obj.decode(s));
                                int source = Integer.parseInt(new_data.get(0));
                                new_data.remove(0);
				boolean update = false;
				lock.lock();
                                update = obj.check(new_data,source);
				if(update){
                                        String to_send = obj.convert(self);
                                        sendData = to_send.getBytes();
                                        for(int pt : neibor){
                                                sendPacket = new DatagramPacket(sendData,sendData.length,Ip,pt);
                                                sock.send(sendPacket);
                                        }	
					 double time = (double)System.currentTimeMillis()/1000.0;
                                        System.out.printf("[%.3f] Node %d Routing Table\n",time,self);
                                        for(int n : m.keySet()){
                                                if(hop.get(n)==n) System.out.println("- ("+m.get(n)+") -> Node "+n);
                                                else{
                                                        System.out.print("- ("+m.get(n)+") -> Node "+n+";");
                                                        System.out.println(" Next hop -> Node "+hop.get(n));
                                                    }
                                        }
				}lock.unlock();	
			}	
		}
	}
	catch(Exception e){e.printStackTrace();}
	}
	
	public class send_update extends Thread{

		public void run(){
			while(status){
				byte[] sendData = new byte[1024];
                                try{
                                Thread.sleep(5000);
				lock.lock();
				String to_send = obj.convert(self);
                                sendData = to_send.getBytes();
                                for(int pt : neibor){
                                        DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,Ip,pt);
                                        sock.send(sendPacket);
                               	   }
				}catch(Exception e){;}
				lock.unlock();
			}
		}
		
	}
		
	private class timer extends Thread{

		public void run(){
			while(status){
				try{
					Thread.sleep(1000);
					}catch(Exception e){;}
				if(already_sent==0) continue;
				double time = (double)System.currentTimeMillis()/1000.0;
				double rate = 1.0 - (double)already_picked/(double)already_sent;
				int already_lost = already_sent-already_picked ;
                                System.out.printf("[%.3f] Link to %d: %d packets sent, %d packets lost, loss rate %.2f\n",time, cur_port, already_sent, already_lost, rate);
			}
		}	
	}

	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	public boolean exp(double probabilityTrue)
        {
            return Math.random() >= probabilityTrue;
        }

	public String convert (int port){
		List<String> list = new ArrayList<String>();
		list.add(String.valueOf(port));
		for(int s : m.keySet()){
			list.add(String.valueOf(s));
			list.add(String.valueOf(m.get(s)));
		}
		StringBuilder output = new StringBuilder();
        	for(String str : list){
            		output.append(String.valueOf(str.length())+"#");
            		output.append(str);
      		}
        return output.toString();
	}

	public List<String> decode(String s) {
        List<String> res = new ArrayList<String>();
        int start = 0 ;
	try{
        while(s.charAt(start)-'0'!=-48){
            int idx = s.indexOf('#', start);
            int size = Integer.parseInt(s.substring(start, idx));
            res.add(s.substring(idx + 1, idx + size + 1));
            start = idx + size + 1;
            }
	}catch(Exception e){;}
        return res;
    	}

	public boolean check(List<String> new_data,int next){
		boolean ans = false;
		for(int i=0 ; i<new_data.size() ; i+=2){
			int key = Integer.parseInt(new_data.get(i));
			double dis_from_next = Double.parseDouble(new_data.get(i+1));
			if(key==self){
				double tmp = m.get(next);
				if(tmp>dis_from_next){
					m.put(next,dis_from_next);
					ans =  true;
				}
				continue;
			}
			if(!m.containsKey(key)){
				m.put(key,1.0+dis_from_next);
				hop.put(key,next);
				ans = true;
			}
			else{
				double dis_to_next = m.get(next);
				double tmp = m.get(key);
				if(tmp>dis_to_next + dis_from_next){
					m.put(key,(dis_to_next*100 + dis_from_next*100)/100);
					ans = true;
					hop.put(key,next);
				}
			}
		}
		return ans;
	}
}
