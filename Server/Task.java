import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import TuringException.AlreadyEditingException;
import TuringException.AlreadyExistDocumentException;
import TuringException.AlreadyLoggedException;
import TuringException.AlreadyRegisteredException;
import TuringException.InvalidEditDocumentException;
import TuringException.InvalidShowDocumentException;
import TuringException.NotExistDocumentException;
import TuringException.NotLoggedException;
import TuringException.NotRegisteredException;

public class Task implements Runnable, CommunicationInterface{
	final String mitt;
	final String dest;
	final String type;
	final String firstParameter;
	final String secondParameter;
	final String thirdParameter;
	Random random;
	Socket sock;
	SocketChannel client;
	JSONObject response;
	private Lock addressLock;

	public Task(JSONObject obj, SocketChannel sockCh) {
		addressLock = new ReentrantLock();
		//ottengo i dati dal JSON
		mitt = (String)obj.get("mitt");
		dest = (String)obj.get("dest");
		type = (String)obj.get("type");
		firstParameter = (String)obj.get("parameter1");
		secondParameter = (String)obj.get("parameter2");
		thirdParameter = (String)obj.get("parameter3");
		
		System.out.println("MESSAGE: Received message of type "+type+" from "+mitt+" to "+dest+" First parameter: "+firstParameter+" Second parameter: "+secondParameter+" Third parameter: "+thirdParameter);
		
		this.client = sockCh;
		this.sock = sockCh.socket();
	
		response= new JSONObject();
	}
	
	@Override
	public void run() {
		
		/*REGISTRAZIONE UTENTE*/
		if(type.equals("register")){
			User dest = null;
			registerUserInterface rUI = null;
			String user = firstParameter;
			String pass = secondParameter;
			try {
				Registry r = LocateRegistry.getRegistry(5000);
				registerUserInterface robj = (registerUserInterface) r.lookup("TURING_SERVER");
				rUI = robj;
			} catch (RemoteException | NotBoundException e) {
				e.printStackTrace();
			}
			try {
				rUI.register(user, pass);
				dest = ServerTuring.users.get(mitt);
				response = msgPrepare(dest.username, "ack", mitt, "Registrazione eseguita con successo", "", null);
				sendMsg(response);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username non valido", mitt);
			} catch (IllegalArgumentException e) {
				errorHandler("ERROR: Password troppo corta (almeno 3 caratteri)", mitt);
			} catch (AlreadyRegisteredException e) {
				errorHandler("ERROR: Utente già registrato", mitt);
			}
			return;
		}
		
		/*LOGIN UTENTE*/
		if(type.equals("login")){
			User dest=null;
			String user = firstParameter;
			String pass = secondParameter;
			try {
				login(user, pass, sock);
				//adesso che ho effettuato il login mando un ack di conferma al client
				dest = ServerTuring.users.get(mitt);
				List<String> invites = dest.getOldInvite();
				response = msgPrepare(dest.username, "ack", mitt, "Login eseguito con successo", "", invites);
				sendMsg(response);
				//ora svuoto cache degli inviti pendenti
				dest.clearInvites();
			} catch (IllegalArgumentException e) {
				errorHandler("ERROR: Password errata", mitt);
			} catch (AlreadyLoggedException e) {
				errorHandler("ERROR: Utente già loggato, provare più tardi", mitt);
			} catch (NotRegisteredException e) {
				errorHandler("ERROR: Utente non registrato", mitt);
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username non valido", mitt);
			}
			return;
		}
		
		/*LOGOUT UTENTE*/
		if(type.equals("logout")){
			try {
				logout(mitt);
			} catch (NullPointerException | IllegalArgumentException e) {
				errorHandler("ERROR: Username non valido", mitt);
			} catch (NotLoggedException e) {
				errorHandler("ERROR: Utente già non loggato", mitt);
			} 
			return;
		}
		
		/*CREAZIONE DOCUMENTO*/
		if(type.equals("create")){
			User dest=null;
			String nameDoc = firstParameter;
			int n = Integer.parseInt(secondParameter);
			try {
				create(nameDoc, n, mitt);
				//mando un ack al client
				dest = ServerTuring.users.get(mitt);
				response = msgPrepare(dest.username, "ack", mitt, "Documento " + nameDoc + " creato con successo composto da " + n + " sezioni", "", null);
				sendMsg(response);
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username o nome documento non valido", mitt);
			} catch (IllegalArgumentException e) {
				errorHandler("ERROR: Numero sezioni non valido", mitt);
			} catch (IOException e) {
				errorHandler("ERROR: Creazione file corrotta", mitt);
			} catch (AlreadyExistDocumentException e) {
				errorHandler("ERROR: Documento già esistente", mitt);
			}
			return;
		}
		
		/*CONDIVIDE DOCUMENTO*/
		if(type.equals("share")){
			User dest1 = null;
			User dest2 = null;
			String name = firstParameter;
			String coll = secondParameter;
			try {
				share(name, coll, mitt);
				//mando un ack al client
				dest1 = ServerTuring.users.get(mitt);
				response = msgPrepare(dest1.username, "ack", mitt, "Documento "+ name +" condiviso con " + coll + " con successo", "", null);
				sendMsg(response);
				
				dest2 = ServerTuring.users.get(coll);
				String notifyMsg = "Ora sei collaboratore del documento "+ name +" creato da " + mitt; 
				//se il collaboratore è online mando subito la notifica di invito
				if(dest2.state == 1) {
					response = msgPrepare(dest2.username, "invite", mitt, notifyMsg, "", null);
					sendInvite(response, dest2.getsocket());
				}	
				//altrimenti salvo la risposta in una lista degli inviti pendenti dell'utente e la invio successivamente
				else 
					dest2.addInvite(notifyMsg);
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username o nome documento non valido", mitt);
			} catch (IllegalArgumentException e) {
				errorHandler("ERROR: L'utente è già collaboratore oppure il nome utente non è valido", mitt);
			} catch (NotExistDocumentException e) {
				errorHandler("ERROR: Documento non esistente", mitt);
			} catch(InvalidShowDocumentException e) {
				errorHandler("ERROR: Permesso non concesso a condividere il documento", mitt);
			}
			
			return;
		}
		
		/*MOSTRA DOCUMENTI CREATORE*/
		if(type.equals("list")){
			User dest=null;
			List<Document> listDocs = null;
			try {
				listDocs = list(mitt);
				//adesso che ho effettuato il logout mando un ack al client
				dest = ServerTuring.users.get(mitt);
				String msgList = messaggeListDocument(listDocs);
				response = msgPrepare(dest.username, "ack", mitt, msgList, "", null);
				sendMsg(response);
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username non valido", mitt);
			} 
			return;
		}
		
		/*SCARICA DOCUMENTO*/
		if(type.equals("show")){
			User dest=null;
			String name = firstParameter;
			//esegue showAll
			if(secondParameter.equals("")) {
				try {
					showAll(name, mitt, thirdParameter);
					//controllo se ci sono sezioni del documento in fase di editing
					Document doc = ServerTuring.documents.get(name);
					List<Integer> sectionsEdited = new ArrayList<Integer>();
					//controllo quali sezioni sono in fase di editing
					for(int i=0; i < doc.getSections()-1; i++) {
						if(!doc.getEditState(i).equals("")) 
							sectionsEdited.add(i+1);
					}
					String notice = "";
					if(sectionsEdited.size() != 0) {
						//ora costruisco il messaggio di notifica
						for(int i = 0; i < sectionsEdited.size()-1; i++) {
							int next = i + 1;
							//controllo se mi trovo nel penultimo ciclo
							if(next < sectionsEdited.size()-1)
								notice = notice + sectionsEdited.get(i) +" e ";
							else
								notice = notice + sectionsEdited.get(i) +", ";
						}
						notice = notice + sectionsEdited.get(sectionsEdited.size()-1);
					}
					
					//mando un ack al client
					dest = ServerTuring.users.get(mitt);
					if(notice.equals(""))
						response = msgPrepare(dest.username, "ack", mitt, "Documento "+ name +" scaricato con successo", "", null);
					else {
						if(sectionsEdited.size() > 1)
							response = msgPrepare(dest.username, "ack", mitt, "Documento "+ name +" scaricato con successo. Attualmente le sezioni "+ notice +" sono in stato di editing", "", null);
						else
							response = msgPrepare(dest.username, "ack", mitt, "Documento "+ name +" scaricato con successo. Attualmente la sezione "+ notice +" è in stato di editing", "", null);
					}
					sendMsg(response);
				} catch (NullPointerException e) {
					errorHandler("ERROR: Username o nome documento non valido", mitt);
				} catch (NotExistDocumentException e) {
					errorHandler("ERROR: Documento non esistente", mitt);
				} catch(InvalidShowDocumentException e) {
					errorHandler("ERROR: Permesso non concesso a visualizzare il documento", mitt);
				} catch (IOException e) {
					errorHandler("ERROR: Lettura/Scrittura corrotta", mitt);
				}
			}
			//altrimenti esegue showSection
			else {
				int section = Integer.parseInt(secondParameter);
				try {
					showSection(name, section, mitt, thirdParameter);
					Document doc = ServerTuring.documents.get(name);
					//mando un ack al client
					dest = ServerTuring.users.get(mitt);
					if(!doc.getEditState(section-1).equals(""))
						response = msgPrepare(dest.username, "ack", mitt, "Sezione "+ section +" scaricata con successo. Attualmente un utente sta modificando la sezione", "", null);
					else
						response = msgPrepare(dest.username, "ack", mitt, "Sezione "+ section +" scaricata con successo", "", null);
					sendMsg(response);
				} catch (NullPointerException e) {
					errorHandler("ERROR: Username o nome documento non valido", mitt);
				} catch (IllegalArgumentException e) {
					errorHandler("ERROR: Numero sezioni non valido", mitt);
				} catch (NotExistDocumentException e) {
					errorHandler("ERROR: Documento non esistente", mitt);
				} catch(InvalidShowDocumentException e) {
					errorHandler("ERROR: Permesso non concesso a visualizzare il documento", mitt);
				} catch (IOException e) {
					errorHandler("ERROR: Lettura/Scrittura corrotta", mitt);
					e.printStackTrace();
				}
			}
			return;
		}
		
		/*FASE EDITING DOCUMENTO*/
		if(type.equals("edit")){
			User dest=null;
			String name = firstParameter;
			int section = Integer.parseInt(secondParameter);
			try {
				String docAddress = edit(name, section, mitt, thirdParameter);
				dest = ServerTuring.users.get(mitt);
				response = msgPrepare(dest.username, "ack", mitt, "Sezione "+ section +" del documento "+ name +" scaricata con successo", docAddress, null);
				sendMsg(response);
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username o nome documento non valido", mitt);
			} catch (IllegalArgumentException e) {
				errorHandler("ERROR: Numero sezioni non valido", mitt);
			} catch (NotExistDocumentException e) {
				errorHandler("ERROR: Documento non esistente", mitt);
			} catch (InvalidShowDocumentException e) {
				errorHandler("ERROR: Permesso non concesso a modificare il documento", mitt);
			} catch (AlreadyEditingException e) {
				errorHandler("ERROR: Un utente sta già editando questa sezione di documento", mitt);
			} catch (IOException e) {
				Document doc = ServerTuring.documents.get(name);
				doc.endEditState(mitt, "", section);
				errorHandler("ERROR: Lettura/Scrittura corrotta", mitt);
			}
			return;
		}
		
		/*FINE FASE EDITING DOCUMENTO*/
		if(type.equals("endedit")){
			User dest=null;
			String nameFile = firstParameter;
			String path = secondParameter;
			try {
				String[] tmp = nameFile.split("_");
				String section = tmp[1].substring(0, 1);
				endEdit(tmp[0], Integer.parseInt(section), path, mitt);
				dest = ServerTuring.users.get(mitt);
				response = msgPrepare(dest.username, "ack", mitt, "Sezione "+ section +" del documento "+ tmp[0] +" aggiornata con successo", "", null);
				sendMsg(response);
			} catch (NullPointerException e) {
				errorHandler("ERROR: Username o nome documento non valido", mitt);
			} catch (IllegalArgumentException e) {
				errorHandler("ERROR: Numero sezioni non valido", mitt);
			} catch (NotExistDocumentException | FileNotFoundException e) {
				errorHandler("ERROR: Documento non esistente", mitt);
			} catch (IOException e) {
				errorHandler("ERROR: Salvataggio modifica corrotta", mitt);
			} catch (InvalidEditDocumentException e) {
				errorHandler("ERROR: Permesso di non modificare", mitt);
			} 
			return;
		}
		
	}
	
	@Override
	public void login(String username, String password, Socket socket)
			throws NullPointerException, IllegalArgumentException, AlreadyLoggedException, NotRegisteredException {
		
		if(username == null) throw new NullPointerException();
		User user = ServerTuring.users.get(username);
		if(user == null) throw new NotRegisteredException();
		if(user.getState() == 1) throw new AlreadyLoggedException();
		if(!user.password.equals(password)) throw new IllegalArgumentException();
		
		user.setSocket(socket);
		user.setState(1);
	}

	@Override
	public void logout(String username) throws NullPointerException, NotLoggedException {
		if(username == null) throw new NullPointerException();
		User user = ServerTuring.users.get(username);
		if(user == null) throw new IllegalArgumentException();
		if(user.getState() == 0) throw new NotLoggedException();
		
		//mando un ack al client prima di chiudere il socket con esso
		response = msgPrepare(mitt, "ack", mitt, "Logout eseguito con successo", "", null);
		sendMsg(response);
		user.closeSocket();
		user.setSocket(null);
		user.setState(0);
	} 
	

	@Override
	public void create(String name, int n, String username) throws NullPointerException, IllegalArgumentException, IOException, AlreadyExistDocumentException {
		if(name == null || username == null) throw new NullPointerException();
		if(n <= 0) throw new IllegalArgumentException();
		//se gli indirizzi dovessero esaurirsi lancio un'eccezione
		if(ServerTuring.UDPAddressChat.equals("")) throw new IllegalArgumentException();
		
		//salvo il documento nella hashtable del server
		Document newDoc = new Document(name, username, n, ServerTuring.UDPAddressChat);
		Document esito = ServerTuring.documents.putIfAbsent(name, newDoc);
		
		/*controllo se l'operazione di inserimento sia andata buon fine
		* se il valore associato alla chiave è diverso da null significa che esiste già il documento con quel nome e lancio eccezione*/
		if(esito != null)
			throw new AlreadyExistDocumentException();
		
		//creo la directory con il nome del documento
		Path dirPath = Paths.get("DB_Document/"+name);
		if(!Files.exists(dirPath)) {
			Files.createDirectories(dirPath);
			for(int i = 1; i <= n; i++) {
				//creo i file di testo all'interno della directory che rappresentano le sezioni
				//nome file: nameDoc_numSezione.txt
				Path filePath = Paths.get("DB_Document/"+name+"/"+name+"_"+i+".txt");
				if (!Files.exists(filePath)) {
				    try {
				        Files.createFile(filePath);
				    } catch (IOException e) {}
				}
				else
					throw new AlreadyExistDocumentException();
			}
		}
		else 
			throw new AlreadyExistDocumentException();
		
		ServerTuring.UDPAddressChat = increaseAddressChat();
	}

	@Override
	public void share(String name, String usernameCollaborator, String usernameCreator)
			throws NullPointerException, IllegalArgumentException, NotExistDocumentException, InvalidShowDocumentException {
		if(name == null || usernameCollaborator == null || usernameCreator == null) throw new NullPointerException();
		Document doc = ServerTuring.documents.get(name);
		if(doc == null) throw new NotExistDocumentException();
		if(!doc.creator.equals(usernameCreator)) throw new InvalidShowDocumentException();
		User user = ServerTuring.users.get(usernameCollaborator);
		if(user == null || doc.isCollaborator(usernameCollaborator)) throw new IllegalArgumentException();
		
		doc.addCollaborator(usernameCollaborator);
	}

	@Override
	public List<Document> list(String username) throws NullPointerException {
		if(username == null) throw new NullPointerException();
		List<Document> res = new ArrayList<Document>();
		Enumeration<String> keys = ServerTuring.documents.keys();
		while(keys.hasMoreElements()) {
			Object key = keys.nextElement();
			//controllo se è un creatore oppure collabora con il relativo documento
			if(ServerTuring.documents.get(key).creator.equals(username) || ServerTuring.documents.get(key).isCollaborator(username))
				res.add(ServerTuring.documents.get(key));
		}
		return res;
	}

	@Override
	public void showAll(String name, String username, String destPath) throws NullPointerException, NotExistDocumentException, InvalidShowDocumentException, IOException {
		if(name == null || username == null) throw new NullPointerException();
		Document doc = ServerTuring.documents.get(name);
		if(doc == null) throw new NotExistDocumentException();
		if(!doc.creator.equals(username) && !doc.isCollaborator(username)) throw new InvalidShowDocumentException();
		
		Integer  sections = doc.getSections();
		ReadWriteServer reader = new ReadWriteServer();
		if(!Files.exists(Paths.get(destPath))) 
			Files.createDirectory(Paths.get(destPath));
		
		for(int i = 1; i <= sections; i++) {
			String file = name+"_"+i+".txt";
			String pathfile = "DB_Document/"+name+"/"+file;
			reader.sendFile(new File(destPath+"/"+file), pathfile);
		}
	}

	@Override
	public void showSection(String name, int section, String username, String destPath)
			throws NullPointerException, IllegalArgumentException, NotExistDocumentException, InvalidShowDocumentException, IOException {
		if(name == null || username == null) throw new NullPointerException();
		Document doc = ServerTuring.documents.get(name);
		if(doc == null) throw new NotExistDocumentException();
		if(section <= 0 || section > doc.nsections) throw new IllegalArgumentException();
		if(!doc.creator.equals(username) && !doc.isCollaborator(username)) throw new InvalidShowDocumentException();
	
		String file = name+"_"+section+".txt";
		String pathfile = "DB_Document/"+name+"/"+file;
		ReadWriteServer reader = new ReadWriteServer();
		//invio il contenuto del file da scaricare 
		reader.sendFile(new File(destPath+"/"+file), pathfile);
	}
	
	@Override
	public String edit(String name, int section, String username, String destPath) throws NullPointerException, IllegalArgumentException,
			NotExistDocumentException, InvalidShowDocumentException, AlreadyEditingException, IOException {
		if(name == null || username == null) throw new NullPointerException();
		Document doc = ServerTuring.documents.get(name);
		if(doc == null) throw new NotExistDocumentException();
		if(section <= 0 || section > doc.nsections) throw new IllegalArgumentException();
		if(!doc.creator.equals(username) && !doc.isCollaborator(username)) throw new InvalidShowDocumentException();
		/*setto che il documento in quella sezione è in fase di editing inserendo il nome dell'utente
		* e controllo se è già in fase di editing*/
		if(!doc.changeEditState(username, section)) throw new AlreadyEditingException();
		
		String file = name+"_"+section+".txt";
		String pathfile = "DB_Document/"+name+"/"+file;
		ReadWriteServer reader = new ReadWriteServer();
		//invio il contenuto del file da scaricare 
		reader.sendFile(new File(destPath+"/"+file), pathfile);
		return doc.getAddress();
	}

	@Override
	public void endEdit(String name, int section, String filePath, String username) throws NullPointerException,
			IllegalArgumentException, NotExistDocumentException, IOException, InvalidEditDocumentException {
		if(name == null || username == null || filePath == null) throw new NullPointerException();
		Document doc = ServerTuring.documents.get(name);
		if(doc == null) throw new NotExistDocumentException();
		if(section <= 0 || section > doc.nsections) throw new IllegalArgumentException();
		if(!doc.getEditState(section-1).equals(username)) throw new InvalidEditDocumentException();
		
		ReadWriteServer writer = new ReadWriteServer();
		writer.writeFile("DB_Document/"+name+"/"+name+"_"+section+".txt", new File(filePath));
		
		//setto che il documento non è più in fase di editing 
		doc.endEditState(username, "", section);
	}
	
	/*metodo che restituisce la lista dei documenti creati o in collaborazione in una stringa*/
	private String messaggeListDocument(List<Document> list) {		
		if(list == null || list.size() == 0) 
			return "Nessun documento creato o in collaborazione";
		
		String res = "";
		for(int i = 0; i < list.size(); i++) {
			Document doc = list.get(i);
			res = res + doc.name+":\n"+"Creatore: "+doc.creator+"\n";
			if(doc.collaborators.size() != 0) {
				res = res + "Collaboratori: ";
				for(int j = 0; j < doc.collaborators.size() - 1; j++)
					res = res + doc.collaborators.get(j) + ", ";
				res = res + doc.collaborators.get(doc.collaborators.size() - 1) + "\n";
			}
		}
		return res;
	}
	
	/*metodo che controlla se incrementa l'indirizzo multicast per la chat
	 * dopo averlo assegnato ad un nuovo documento*/
	public String increaseAddressChat() {
		addressLock.lock();
		String[] octets = (ServerTuring.UDPAddressChat).split(Pattern.quote("."));
		if(octets.length != 4)
			return "";
		int firstOctet = Integer.parseInt(octets[0]);
		int secondOctet = Integer.parseInt(octets[1]);
		int thirdOctet = Integer.parseInt(octets[2]);
		int fourthOctet = Integer.parseInt(octets[3]);
		
		fourthOctet = (fourthOctet+1)%255;
		if(fourthOctet == 0) {
			thirdOctet = (thirdOctet+1)%255;
			if(thirdOctet == 0) {
				secondOctet = (secondOctet+1)%255;
				if(secondOctet == 0) {
					firstOctet = (firstOctet+1)%239;
					secondOctet = 0;
					if(firstOctet == 0)
						return "";
				}
			}
		}
		addressLock.unlock();
		String res = Integer.toString(firstOctet)
						+"."+Integer.toString(secondOctet)
						+"."+Integer.toString(thirdOctet)
						+"."+Integer.toString(fourthOctet);
		System.out.println("ADDRESS: "+res);
		return res;
	}
	
	
	/*metodo che crea il messaggio di errore da inviare al client*/
	private void errorHandler(String msg, String dest) {
		//invio messaggio di errore nel socket sock
		SocketChannel channel = sock.getChannel();
		response = msgPrepare(dest, "error", mitt, msg, "", null);
		ByteBuffer buff= ByteBuffer.wrap(response.toJSONString().getBytes());
		try {
			synchronized(sock) {
				channel.write(buff);
			}
		} catch (IOException e1) {
			//impossibile scrivere su canale effettuo logout
			try {
				System.out.println("Effettuo logout");
				sock.close();
				logout(mitt);
			} catch (NullPointerException | IllegalArgumentException | IOException | NotLoggedException e) {
				// mittente nullo, nessuno da sloggare
				e.printStackTrace();
			} 
		}
		System.out.println("Messaggio errore inviato");
	}
	
	/*metodo che invia il messaggio JSON al client*/
	public synchronized void sendMsg(JSONObject msg) throws NullPointerException {
		if(sock==null) throw new NullPointerException();
		// invio messaggio di ack nel socket sock
		SocketChannel channel = sock.getChannel();
		ByteBuffer buff= ByteBuffer.wrap(msg.toJSONString().getBytes());
		try {
			channel.write(buff);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Messaggio inviato");
	}
	
	/*metodo che invia la notifica di invito al client connesso*/
	public synchronized void sendInvite(JSONObject msg, Socket s) throws NullPointerException {
		if(s == null) throw new NullPointerException();
		// invio messaggio di notifica nel socket sock
		SocketChannel channel = s.getChannel();
		ByteBuffer buff= ByteBuffer.wrap(msg.toJSONString().getBytes());
		try {
			channel.write(buff);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Invito inviato");
	}
	
	@SuppressWarnings("unchecked")
	/*metodo che crea l'oggetto JSON da inviare come messaggio al client*/
	private static JSONObject msgPrepare(String dest, String type, String mitt, String msg, String title, List<String> list) {
		JSONObject message = new JSONObject();
		
		message.put("mitt", mitt);
		message.put("dest", dest);
		message.put("type", type);
		message.put("msg", msg);
		message.put("title", title);
		
		if(list != null) {
			if(list.size() != 0) {
				JSONArray listNotify = new JSONArray();
				listNotify.addAll(list);
				message.put("list", listNotify);
			}
			else 
				message.put("list", "");
		}
		else
			message.put("list", "");
		
		return message;
	}
}
