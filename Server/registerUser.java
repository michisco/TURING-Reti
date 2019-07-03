import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import TuringException.AlreadyRegisteredException;

/*Classe per la gestione della registrazione dell'utente*/
public class registerUser extends UnicastRemoteObject implements registerUserInterface {
	private static final long serialVersionUID = 1L;
	
	protected registerUser() throws RemoteException {
		super();
	}

	@Override
	public void register(String username, String password) throws RemoteException, AlreadyRegisteredException, NullPointerException, IllegalArgumentException {
		if(username == null || password == null) throw new NullPointerException();
		//se la password è minore di 3 caratteri lancio un eccezione
		if(password.length() > 2) {
			User u = new User(username, password);
			User esito = ServerTuring.users.putIfAbsent(username, u);
			/*controllo se l'operazione di inserimento sia andata buon fine
			* se il valore associato alla chiave è diverso da null significa che esiste già l'utente con quel nome e lancio eccezione*/
			if(esito != null)
				throw new AlreadyRegisteredException();
		}
		else
			throw new IllegalArgumentException();
	}

	@Override
	public List<String> getUsers() throws RemoteException {
		//ottengo gli username (chiavi) degli utenti
		Set<String> keySet = ServerTuring.users.keySet();
		//li copio nella lista da restituire
		ArrayList<String> res = new ArrayList<String>(keySet);	
		return res;
	}
}
