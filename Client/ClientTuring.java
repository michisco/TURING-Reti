import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClientTuring extends Thread{
	static Socket socket;
	String nickname;
	static List<JSONObject> ack;
	static Object lock = new Object();
	String dirDownload = "downloads_";
	UDPChat receiver = null;
	String addressChat = "";
	
	public ClientTuring() throws IOException {
		ack=new Vector<>();
		try {
			socket =new Socket("127.0.0.1", 3100);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	/*routine per la ricezione dei messaggi e dei file*/
	public void run() {
		
		JSONObject obj = null;
		try {
			while(!socket.isClosed()) {
				try {
					obj = msgReceive();
					String type = (String) obj.get("type");
					try {
						synchronized(lock) {
							//se è un messaggio di ack o un errore lo salvo nella lista ACK
							if(type.equals("ack") || type.equals("error")) {
								ack.add(obj);
							}
							//altrimenti se è un invito lo notifico nella stampa del client
							else {
								String notify = (String) obj.get("msg");
								System.out.print("\n"+notify+"\n");
							}
							lock.notifyAll();
						}
					}catch(Exception e) {}//messaggio illegibile lo scarto
				} catch (Exception e) {
					try {
						socket.close();
					} catch (IOException e1) {}
					break;
				}
			}			
		}catch(NullPointerException e) {
			System.out.println("Connessione assente. Riprovare più tardi");
			System.exit(0);
		}
	}
	
	
	/*metodo che riceve i messaggi JSON dal server*/
	private JSONObject msgReceive () throws IOException {
		InputStream in = socket.getInputStream();
		byte[] b = new byte[3000];

		JSONParser parser = new JSONParser();
		in.read(b);
		
		String risposta = new String(b);
		risposta = risposta.trim();

		JSONObject parseObj=null;
		try {
			parseObj = (JSONObject) parser.parse(risposta);
		} catch (ParseException e) {
			throw new IOException();
		}
		
		return parseObj;
	}
	
	@SuppressWarnings("static-access")
	public void closeAll()  {
		try {
			this.socket.close();
		} catch (Exception e) {}
	}
	
	/*metodo che invia il JSON object al server*/
	private static void msgSend (JSONObject obj) throws IOException {
		
		String message = obj.toJSONString();
		OutputStream out = socket.getOutputStream();
		out.write(message.getBytes());
	}
	
	/*metodo che ottiene l'ultimo ack inserito nella lista e lo rimuove*/
	private static JSONObject getAckMessage()  {
		JSONObject obj;
		synchronized(lock) {
			while(ack.isEmpty()) {
				try {
					lock.wait(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} 
			obj=ack.get(ack.size()-1);
			ack.remove(ack.size()-1);
		}
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	/*metodo che crea il messaggio JSON da inviare al server*/
	private JSONObject msgPrepare (String dest, String type, String mitt, String firstPar, String secondPar, String thirdPar) {
		JSONObject message = new JSONObject();
		
		message.put("mitt", mitt);
		message.put("dest", dest);
		message.put("type", type);
		message.put("parameter1", firstPar);
		message.put("parameter2", secondPar);
		message.put("parameter3", thirdPar);
		
		return message;
	}
	
	public void register(String username, String password) throws IOException, IllegalArgumentException {
		JSONObject obj = msgPrepare("server", "register", username, username, password, "");
	
		msgSend(obj);
		obj = getAckMessage();

		if(obj.get("type").equals("ack")) {
			//creo la cartella dove scaricare i file per lo specifico utente
			Path dirPath = Paths.get(dirDownload+username);
			if(!Files.exists(dirPath)) 
				Files.createDirectories(dirPath);
			System.out.println(obj.get("msg"));
		} else if (obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
		}
		else 
			throw new IllegalArgumentException();
		return;
	}
	
	public boolean login(String username, String password) throws IOException, IllegalArgumentException {
		nickname = username;
		JSONObject obj = msgPrepare("server", "login", nickname, username, password, "");
				
		msgSend(obj);
		obj = getAckMessage();
		
		if(obj.get("type").equals("ack")) {
			System.out.println(obj.get("msg"));
		}	
		else if(obj.get("type").equals("error")) {
			System.out.println(obj.get("msg"));
			nickname = null;
			return false;
		}
		else 
			throw new IllegalArgumentException();
		
		//in caso di login controllo se l'utente ha delle notifiche pendenti da segnalare 
		if(!obj.get("list").equals("")) {
			System.out.println("NOTIFICHE:\n");
			
			JSONArray slideContent = (JSONArray) obj.get("list");
	        @SuppressWarnings("rawtypes")
			Iterator i = slideContent.iterator();
	        while (i.hasNext()) {
	            System.out.println(i.next());        
	        }
		}
		
		return true;	
	}
	
	public boolean logout(boolean state)throws NullPointerException, IllegalArgumentException, IOException {
		if(nickname == null) throw new NullPointerException();
		JSONObject obj = msgPrepare("server", "logout", nickname, "", "", "");
		
		msgSend(obj);
		obj = getAckMessage();
		
		if(obj.get("type").equals("ack"))
			System.out.println(obj.get("msg"));
		else if(obj.get("type").equals("error")) {
			System.out.println(obj.get("msg"));
			return state;
		}else 
			throw new IllegalArgumentException();
		
		nickname = null;
		return false;
	}
	
	public void create(String name, String sections) throws IOException, IllegalArgumentException {
		JSONObject obj = msgPrepare("server", "create", nickname, name, sections, "");
		
		msgSend(obj);
		obj = getAckMessage();

		if(obj.get("type").equals("ack") || obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
			return;
		}
		else 
			throw new IllegalArgumentException();
	}
	
	public void share(String name, String username) throws IOException, IllegalArgumentException {
		JSONObject obj = msgPrepare("server", "share", nickname, name, username, "");
		
		msgSend(obj);
		obj = getAckMessage();

		if(obj.get("type").equals("ack") || obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
			return;
		}
		else 
			throw new IllegalArgumentException();
	}
	
	public void showAll(String name) throws IOException, IllegalArgumentException {
		String destPath = dirDownload+nickname+"/"+name;
		//creo directory per contenere l'intero documento
		Path dirPath = Paths.get(destPath);
		
		JSONObject obj = msgPrepare("server", "show", nickname, name, "", dirPath.toAbsolutePath().toString());
		msgSend(obj);
		
		obj = getAckMessage();
		if(obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
		}
		else if(obj.get("type").equals("ack")) { 
			//mostro il feedback del server all'utente
			System.out.println(obj.get("msg"));
		}
		else 
			throw new IllegalArgumentException();
		
	}
	
	public void showSection(String name, String section) throws IOException, IllegalArgumentException {
		Path p = Paths.get(dirDownload+nickname).toAbsolutePath();
		JSONObject obj = msgPrepare("server", "show", nickname, name, section, p.toString());
		msgSend(obj);
		
		
		obj = getAckMessage();
		if(obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
		}
		else if(obj.get("type").equals("ack")) { 
			String msg = (String) obj.get("msg");
			
			//mostro il feedback del server all'utente
			System.out.println(msg);
		}
		else 
			throw new IllegalArgumentException();
	}
	
	public boolean edit(String name, String section) throws IOException, IllegalArgumentException {
		String destPath = dirDownload+nickname+"/EDIT";
		//creo directory per contenere il file da modificare
		Path dirPath = Paths.get(destPath);
		if(!Files.exists(dirPath)) 
			Files.createDirectory(dirPath);
		
		JSONObject obj = msgPrepare("server", "edit", nickname, name, section, dirPath.toAbsolutePath().toString());
		msgSend(obj);
		
		obj = getAckMessage();
		if(obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
			return false;
		}
		else if(obj.get("type").equals("ack")) { 
			addressChat = (String) obj.get("title");
			String msg = (String) obj.get("msg");
			
			//creazione chat multicast 
			try{
				receiver = new UDPChat(addressChat);
				receiver.start();
			}catch(IOException e) {
				System.out.println("ERRORE CREAZIONE CHAT");
				return false;
			}
			//mostro il feedback del server all'utente
			System.out.println(msg);		
		}
		else 
			throw new IllegalArgumentException();

		return true;
	}
	
	public boolean endEdit(String name, String section) throws IOException, IllegalArgumentException {
		String destPath = dirDownload+nickname+"/EDIT/"+name+"_"+section+".txt";
		File fileEdited = new File(destPath);	
		if(!fileEdited.exists()) {
			System.out.println("Documento non esistente nella cartella");
			return true;
		}
		
		JSONObject obj = msgPrepare("server", "endedit", nickname, name+"_"+section+".txt", fileEdited.getAbsolutePath().toString(), "");
		msgSend(obj);
		
		obj = getAckMessage();
		if(obj.get("type").equals("ack")) {
			System.out.println(obj.get("msg"));
	
		}
		else if(obj.get("type").equals("error")) {
			System.out.println(obj.get("msg"));
			return true;
		}
		else 
			throw new IllegalArgumentException();
		
		//chiudo canale chat
		addressChat = "";
		receiver.chatStop();
		return false;
	}
	
	public void list() throws IOException, IllegalArgumentException {
		JSONObject obj = msgPrepare("server", "list", nickname, "", "", "");
		
		msgSend(obj);
		obj = getAckMessage();

		if(obj.get("type").equals("ack") || obj.get("type").equals("error")) { 
			System.out.println(obj.get("msg"));
			return;
		}
		else 
			throw new IllegalArgumentException();
	}
	
	//////// CHAT SIDE //////////////
	
	/*metodo che invia un messaggio multicast alla chat*/
	public void send(String msg) {
		try {
			MulticastSocket ms = new MulticastSocket(3500);
			InetAddress address=InetAddress.getByName(addressChat);
			
			//preparo il messaggio con l'orario di invio + il messaggio effettivo
			Calendar now = Calendar.getInstance();
			msg = now.get(Calendar.HOUR_OF_DAY) +"."+ now.get(Calendar.MINUTE) +" "+ nickname +": "+ msg;
			//accodo al messaggio un header per riconoscere chi ha inviato il messaggio
			byte[] buff = (nickname+">>>>"+msg).getBytes();
			DatagramPacket dp = new DatagramPacket(buff,buff.length, address, 3500);
			dp.setAddress(address);
			ms.send(dp);
			ms.close();
			System.out.println("Messaggio inviato sulla chat");
		}catch(IOException ex) {
			System.out.println("ERRORE: Messaggio non inviato");
		}
	}
	
	/*metodo per ricevere i messaggi dalla chat*/
	public void receive() {
		receiver.receive(nickname);
	}
	
	/////////////////////////////////
	
	/////// READ/WRITE FILES ///////
	
	/*metodo per poter scrivere il contenuto del file scaricato in un file destinazione*/
	public void writeFile(JSONObject obj, String dest) throws IOException {
		String destination = dest+"/"+((String) obj.get("title"));
		String files = (String) obj.get("msg");
		int size = Integer.parseInt((String) obj.get("size"));
		Path pathClient = Paths.get(destination);
		
		//creo il file se non esiste
		if (!Files.exists(pathClient)) {
		    try {
		        Files.createFile(pathClient);
		    } catch (IOException e) {
		    	throw new IOException();
		    }
		}
		
		byte[] bc;
		ByteBuffer buffer = ByteBuffer.allocateDirect(size + 1024);
		bc = files.getBytes();
		
		//apro il file channel
		FileChannel outChannel = FileChannel.open(pathClient, 
				StandardOpenOption.WRITE);		

		//scrivo il contenuto nel nuovo file
		buffer.put(bc);
		buffer.flip();
		while (buffer.hasRemaining())
				outChannel.write(buffer);
		buffer.clear();
		outChannel.close(); 	 
	}
	
	/*metodo che legge il contenuto di un file*/
	public String sendFile(String nameDoc) throws IOException {
		File fileSent =new File(nameDoc);
		int dimFile = (int) fileSent.length(); 
		int reads=0;
		List<Byte> b = new ArrayList<Byte>();
		nameDoc=nameDoc.trim();
		//apro un file channel
		FileChannel f = FileChannel.open(Paths.get(nameDoc),StandardOpenOption.READ);
		ByteBuffer tmp = ByteBuffer.allocate(dimFile + 1024);
		//leggo il suo contenuto all'interno salvando in una lista di byte
		try {
			while((reads = f.read(tmp))>0){
				tmp.flip();
				byte[] tmp1 = tmp.array();
				for(int i = 0; i<reads; i++) {
					b.add(tmp1[i]);
				}
				tmp.clear();
			}
		} catch (IOException e) {}
		
		//salvo il contenuto appena letto in un array di byte
		byte[] bt = new byte[b.size()];
		for(int i =0; i<b.size(); i++) {
			bt[i]=b.get(i);
		}
		
		String file= new String(ByteBuffer.wrap(bt).array());
		return file;
	}
}