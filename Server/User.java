import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/*Classe User rappresenta tutte le info sugli utenti registrati*/
public class User {
	String username, password;
	int state;
	Socket socket;
	List<String> invites;
	
	public User(String user, String pass) {
		this.username = user;
		this.password = pass;
		invites = new ArrayList<String>();
	}
	
	/*metodo per restituire lo stato di connessione di un utente
	 * @return: state - stato di connessione*/
	public int getState() {
		return state;
	}
	
	/*metodo per settare lo stato di connessione
	 * @args: s - stato di connessione solo valori 0/1*/
	public synchronized  void setState(int s) {
		if(s==0 || s==1) {
			this.state=s;
		}
	}

	/*metodo per restituire il nome dell'utente 
	 * @return: username - nome utente*/
	public String getUsername() {
		return username;
	}
	
	/*metodo per settare un socket allo specifico utente
	 * @args: s - socket dell'utente*/
	public synchronized void setSocket(Socket s) {
		try {
			socket.close();
		} catch (Exception e) {}
		socket=s;
		/*if(s==null) 
			state = 0; 
		else 
			state = 1;*/
	}
	
	/*metodo per chiudere il socket dell'utente*/
	public void closeSocket() {
		try {
			socket.close();
		} catch (IOException e) {}
	}

	/*metodo per ottenere il socket associato all'utente
	 * @return: socket - socket dell'utente*/
	public Socket getsocket() {
		return socket;
	}
	
	/*metodo per aggiungere la notifica di invito allo specifico utente 
	 * @args: invite - invito per l'utente*/
	public synchronized void addInvite(String invite) {
		invites.add(invite);
	}
	
	/*metodo per ottenere tutti gli inviti pendenti per l'utente
	 * @return: invites - lista inviti pendenti*/
	public List<String> getOldInvite(){
		return invites;
	}
	
	/*metodo per cancellare tutti gli inviti pendenti nella lista*/
	public synchronized void clearInvites() {
		invites.clear();
	}
}
