import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class UDPChat extends Thread{
	int port;
	List<String> messagges;
	MulticastSocket ms = null;
	InetAddress group = null;
	boolean stopServer;
	
	//metodo costruttore che prende l'indirizzo multicast
	public UDPChat(String address) throws IOException, SocketException, UnknownHostException{
		port = 3500;
		messagges = new ArrayList<String>();
		stopServer = false;
		
		group = InetAddress.getByName(address);
		ms = new MulticastSocket(port);
		ms.setReuseAddress(true);		
		ms.joinGroup (group);	
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			byte[] buffer = new byte[8192];
			//ciclo continuo che aspetta i messaggi di tutti gli utenti 
			//connessi alla chat e li salva in una lista 
			while(!stopServer) {
				DatagramPacket dp=new DatagramPacket(buffer,buffer.length);
				ms.receive(dp);
				String s = new String(dp.getData(), dp.getOffset(), dp.getLength());
				messagges.add(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if (ms!=null){
				try {
					ms.leaveGroup(group);
				} catch (IOException e) {}
				ms.close();
			}
		}
	}
	
	/*metodo che consegna i messaggi ricevuti finora 
	 * escludendo quelli dell'utente che ne fa richiesta*/
	public void receive(String username){
		List<String> receives = new ArrayList<String>();
		if(messagges.isEmpty()) {
			System.out.println("Nessun messaggio in chat");
			return;
		}
		
		//inserisco i messaggi tranne dell'utente che ne fa richiesta in una lista temporanea
		for(int i = 0; i < messagges.size(); i++) {
			String[] splits = messagges.get(i).split(">>>>");
			if(splits[0].equals(username))
				continue;
			receives.add(splits[1]);
			//System.out.println(splits[1]);
		}
		messagges.clear();
		
		//controllo se non ci sono messaggi di altri utenti da visualizzare
		if(receives.isEmpty()) {
			System.out.println("Nessun messaggio in chat");
			return;
		}
		
		for(int i = 0; i < receives.size(); i++) 
			System.out.println(receives.get(i));
	}
	 	
	/*metodo che chiude la chat multicast*/
	public void chatStop() {
		stopServer = true;
	}
}
