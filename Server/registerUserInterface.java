import java.rmi.RemoteException;
import java.util.List;

import TuringException.AlreadyRegisteredException;

import java.rmi.Remote;

public interface registerUserInterface extends Remote{
	
	/*metodo che registra un nuovo utente con username e password associato
	 * @args: username - stringa che rappresenta l'username del nuovo utente
	 * 		  password - stringa che rappresenta la password
	 * @throws: NullPointerException - se username e password sono NULL
	 * 			   IllegalArgumentException - se la password è errata
	 * 			   AlreadyRegisteredException - se l'utente è già registrato*/
	public void register (String username, String password) throws RemoteException, AlreadyRegisteredException, NullPointerException, IllegalArgumentException;
	
	/*metodo che restituisce la lista degli utenti registrati
	 * @return: una lista di stringhe*/
	public List<String> getUsers () throws RemoteException;
}
