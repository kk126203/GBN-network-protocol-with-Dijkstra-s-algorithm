import java.io.*;
import java.net.*;
import java.util.*;

public class dvnode{

	static DatagramSocket sock = null;
	static int self;
	static Map<Integer,Double> m;
	static Map<Integer,Integer> hop;
	static List<Integer> neibor;
	static InetAddress Ip;
	public static void main(String args[])
    	{
		boolean start = false;
		byte[] sendData = new byte[1024];
		byte[] recData = new byte[1024];
		m = new HashMap<Integer,Double>();
		hop = new HashMap<Integer,Integer>();
		neibor = new ArrayList<Integer>();
		dvnode obj = new dvnode();
		if(args.length<3||args.length%2==0&&!args[args.length-1].equals("last"))
		{
			System.out.println("invalid input");
			System.exit(0);
		}
		if(args[args.length-1].equals("last")) start = true;
		for(int i=1 ; i<args.length ; i+=2){
			if(args[i].equals("last")) continue;
			m.put(Integer.parseInt(args[i]), Double.parseDouble(args[i+1]));
			neibor.add(Integer.parseInt(args[i]));
			hop.put(Integer.parseInt(args[i]),Integer.parseInt(args[i]));
		}
		self = Integer.parseInt(args[0]);
		try{	
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
			boolean trigger = true;
			while(true){
				boolean update = false;
				DatagramPacket receivePacket = new DatagramPacket(recData, recData.length);
				sock.receive(receivePacket);
				String s = new String( receivePacket.getData());
				List<String> new_data = (obj.decode(s));
				int source = Integer.parseInt(new_data.get(0));
				if(!neibor.contains(source)){
					System.out.println("nonexist neibor");
					System.exit(0);
				}
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
					double time = (double)System.currentTimeMillis()/1000.0;
                                        System.out.printf("[%.3f] Node %d Routing Table\n",time,self);
					for(int n : m.keySet()){
						if(hop.get(n)==n) System.out.println("- ("+m.get(n)+") -> Node "+n);
						else{
                                		    	System.out.print("- ("+m.get(n)+") -> Node "+n+";");
							System.out.println(" Next hop -> Node "+hop.get(n)); 
						    }
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
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
        while(s.charAt(start)-'0'!=-48){
            int idx = s.indexOf('#', start);
            int size = Integer.parseInt(s.substring(start, idx));
            res.add(s.substring(idx + 1, idx + size + 1));
            start = idx + size + 1;
        }
        return res;
    }

	public boolean check(List<String> new_data,int next){
		boolean ans = false;
		double dis_to_next = m.get(next);
		for(int i=0 ; i<new_data.size() ; i+=2){
			int key = Integer.parseInt(new_data.get(i));
			double dis_from_next = Double.parseDouble(new_data.get(i+1));
			if(key==self) continue;
			if(!m.containsKey(key)){
				m.put(key,(dis_to_next*100 + dis_from_next*100)/100);
				hop.put(key,next);
				ans = true;
			}
			else{
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
