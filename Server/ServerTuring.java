import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ServerTuring implements Runnable {

	public static int port;
	boolean stop;
	Selector selector;
	registerUserInterface s = null;
	ExecutorService executor;
	public static ConcurrentHashMap<String, User> users;
	public static ConcurrentHashMap<String, Document> documents;
	public static String UDPAddressChat;
	
	
	public ServerTuring(int p) {
		users = new ConcurrentHashMap<String, User>();
		documents = new ConcurrentHashMap<String, Document>();
		UDPAddressChat = "225.0.0.10";
		port = p;
		stop = false;
		executor = Executors.newCachedThreadPool();
	}
	
	@Override
	public void run() {
		//appena inizializzo il server, creo la cartella dove salvo tutti i documenti
		new File("DB_Document").mkdirs();
		
		System.out.println("Ascolto connessioni sulla " + port);
		ServerSocketChannel serverChannel;
		JSONParser parser = new JSONParser();
		ByteBuffer buff = ByteBuffer.allocateDirect(3000);
		//apro il socket
		try {
			System.out.println("Apro connessione sulla porta "+port);
			serverChannel = ServerSocketChannel.open();
			ServerSocket ss = serverChannel.socket();
			InetSocketAddress address = new InetSocketAddress(port);
			System.out.println("Indirizzo corrente "+address.getAddress());
			ss.bind(address);
			serverChannel.configureBlocking(false);
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
		//registrazione utente
		try {
			registerUserInterface s = new registerUser();
			LocateRegistry.createRegistry(5000);
			Registry r = LocateRegistry.getRegistry(5000);
			r.rebind("TURING_SERVER", (Remote) s);
		}catch(RemoteException e) {
			System.out.println("ERRORE NELLA REGISTRAZIONE");
			e.printStackTrace();
		}
		
		//ciclo per la ricezione e gestione dei messaggi
		System.out.println("Attendendo connessioni ... ");
		while(!stop) {
			try {
				selector.select();
				
			}catch (IOException ex) {
				ex.printStackTrace();
				break;
			}
			
			Set <SelectionKey> readyKeys;
			Iterator <SelectionKey> iterator;
			
			synchronized(this){
				if(!selector.isOpen()) break;
				readyKeys = selector.selectedKeys();
			}
			iterator = readyKeys.iterator();
			
			
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				
				try {
					//accettazione delle nuove connessioni
					if (key.isAcceptable()) {
						System.out.println("Ricevuto una connessione socket ");
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						SocketChannel client = server.accept();
						System.out.println("Connessione accettata da " + client);
						client.configureBlocking(false);	
						client.register(selector, SelectionKey.OP_READ);
					}
					
					//lettura del messaggio in arrivo
					else if(key.isReadable()){
						System.out.println("Ricevuto messaggio dal client");
						SocketChannel client = (SocketChannel) key.channel();
												
						int loopevent=0;
						int res=0;
						buff.clear();
						while((res=client.read(buff)) != -1 && buff.hasRemaining() && loopevent == 100) 
							loopevent++;
						
						//messaggio troppo grande
						if(buff.hasRemaining() == false || res == -1)
							throw new IOException();
						if (buff.position()<3) {
							System.out.println("Byte residui sul canale puliti");
							break;
						}
						
						buff.flip();
						byte[] arr = new byte[buff.remaining()];
						buff.get(arr);
						String name = new String(arr);
						System.out.println(name);
						buff.clear();

						//fetch JSON e metto la richiesta in pila nel threadpool
						JSONObject parseObj = null;
						try {
							parseObj = (JSONObject) parser.parse(name);
							System.out.println("Parsing JSON");
						}catch (Exception e) {
							System.out.println("Messaggio invalido ricevuto");
							throw new IOException();
						}
						
						//consegno il messaggio al threadPool
						//Task t = new Task(parseObj, client.socket());
						Task t = new Task(parseObj, client);
						executor.execute(t);
					}
				//in caso di errore
				}catch (IOException ex) {
					//logout utente associato al socket
					SocketChannel cl = (SocketChannel) key.channel();
					Enumeration<String> keysSearch = ServerTuring.users.keys();
					while(keysSearch.hasMoreElements()) {
						Object keySearch = keysSearch.nextElement();
						if(users.get(keySearch).getsocket().equals(cl.socket()))
							users.get(keySearch).setState(0);
					}
					System.out.println("ERRORE: IO, cancellazione chiave e logout utente");
					//cancellazione chiave dal set e chiusura del socket
					key.cancel();
					try{
						cl.socket().close();
						key.channel().close(); 
					}
					catch (IOException cex) {} 
				}
			}
		}
		//quando il loop termina chiudo registry
		try {
			System.out.println("Unbinding registry");
			Registry r = LocateRegistry.getRegistry(5000);
			r.unbind("TURING_SERVER");
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}  
		return;
	}
	
	public synchronized void serverStop() {
		stop=true;
		try {
			selector.close();
		} catch (IOException e1) {}
		
		//ci sono ancora task da eseguire il server verrà chiuso lo stesso
		executor.shutdown();
		System.out.println("Chiusura del server");
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if(executor.isShutdown()) return;
		else this.serverStop();
	}
}
