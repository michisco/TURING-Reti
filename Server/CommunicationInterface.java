import java.io.IOException;
import java.net.Socket;
import java.util.List;

import TuringException.AlreadyEditingException;
import TuringException.AlreadyExistDocumentException;
import TuringException.AlreadyLoggedException;
import TuringException.InvalidEditDocumentException;
import TuringException.InvalidShowDocumentException;
import TuringException.NotExistDocumentException;
import TuringException.NotLoggedException;
import TuringException.NotRegisteredException;

public interface CommunicationInterface {

	
	/*metodo per loggare al servizio
	 * @args: username - stringa che rappresenta l'username dell'utente
	 * 		  password - stringa che rappresenta la password
	 * 		  socket - socket per aprire la connessione
	 * @throws: NullPointerException - se username e password sono NULL
	 * 			IllegalArgumentException - se la password è errata
	 * 			AlreadyLoggedException - se l'utente è già loggato
	 * 			NotRegisteredException - se l'utente non è registrato al servizio*/
	public void login(String username, String password, Socket socket) throws NullPointerException, IllegalArgumentException, AlreadyLoggedException, NotRegisteredException;
	
	/*metodo per sloggare dal servizio  
	 * @args: username - stringa che rappresenta l'username dell'utente
	 * @throws: NullPointerException - se username è NULL
	 * 			NotLoggedException - se l'utente non è già loggato */
	public void logout(String username) throws NullPointerException, NotLoggedException;
	
	/*metodo per creare un nuovo documento 
	 * @args: name - stringa che rappresenta il nome del documento
	 * 		  n - il numero delle sezioni 
	 * @throws: NullPointerException - se name e username sono NULL
	 * 			IllegalArgumentException - se il numero di sezioni <= 0 oppure il documento esiste già*/
	public void create(String name, int n, String username) throws NullPointerException, IllegalArgumentException, IOException, AlreadyExistDocumentException;
	
	/*metodo per aggiungere un collaboratore a un documento
	 * @args: name - nome del documento
	 * 		  usernameCollaborator - l'utente con cui condividere il documento 
	 * 		  usernameCreator - il creatore del documento
	 * @throws: NullPointerException - se name, usernameCollaborator e usernameCreator sono NULL
	 * 			IllegalArgumentException - se l'utente con cui si vuole condividere non esiste oppure l'utente è già collaboratore
	 * 			NotExistDocumentException - se il documento non esiste
	 * 			InvalidShowDocumentException - se l'utente che fa richiesta non è creatore*/
	public void share(String name, String usernameCollaborator, String usernameCreator) throws NullPointerException, IllegalArgumentException, InvalidShowDocumentException, NotExistDocumentException;
	
	/*metodo per visualizzare tutto il documento
	 * @args: name - nome del documento
	 * 		  username - utente creatore o collaboratore del documento
	 * 		  destPath - path destinazione per la scrittura dei files
	 * @throws: NullPointerException - se name e username sono NULL
	 * 			NotExistDocumentException - se il documento non esiste
	 * 			InvalidShowDocumentException - se il documento non 'appartiene' al creatore o al collaboratore
	 * 			IOException - errore nella scrittura/lettura del file da visualizzare*/
	public void showAll(String name, String username, String destPath) throws NullPointerException, NotExistDocumentException, InvalidShowDocumentException, IOException;
	
	/*metodo per visualizzare una sezione specifica del documento
	 * @args: name - nome del documento
	 * 		  section - sezione del documento 
	 * 		  username - utente creatore o collaboratore del documento
	 * 		  destPath - path destinazione per la scrittura del file
	 * @throws: NullPointerException - se name e username sono NULL
	 * 			IllegalArgumentException - se il numero di sezioni <= 0 oppure non è una sezione esistente
	 * 			NotExistDocumentException - se il documento non esiste
	 * 			InvalidShowDocumentException - se il documento non 'appartiene' al creatore o al collaboratore
	 * 			IOException - errore nella scrittura/lettura del file da visualizzare*/
	public void showSection(String name, int section, String username, String destPath) throws NullPointerException, IllegalArgumentException, NotExistDocumentException, InvalidShowDocumentException, IOException;
	
	/*metodo per mostrare tutti i documenti creati da un utente o che ne collabora
	 * @args: username - nome dell'utente
	 * @throws: NullPointerException - se username è NULL
	 * @return: lista di tutti i documenti */
	public List<Document> list(String username) throws NullPointerException;
	
	/*metodo per iniziare lo stato di modifica di una sezione del documento
	 * @args: name - nome del documento
	 * 		  section - sezione del documento 
	 * 		  username - utente creatore o collaboratore del documento
	 *        destPath - path destinazione per la scrittura del file
	 * @throws: NullPointerException - se name e username sono NULL
	 * 			IllegalArgumentException - se il numero di sezioni <= 0 oppure non è una sezione esistente
	 * 			NotExistDocumentException - se il documento non esiste
	 * 			InvalidShowDocumentException - se il documento non 'appartiene' al creatore o al collaboratore
	 * 			AlreadyEditingException - se un utente sta già editando una sezione del documento
	 * 			IOException - errore nella scrittura/lettura del file da visualizzare
	 * @return: indirizzo multicast relativo a quel documento*/
	public String edit(String name, int section, String username, String destPath) throws NullPointerException, IllegalArgumentException, NotExistDocumentException, InvalidShowDocumentException, AlreadyEditingException, IOException;
	
	/*metodo per porre fine allo stato di modifica di una sezione del documento
	 * @args: name - nome del documento
	 * 		  section - sezione del documento 
	 * 		  mittPath - path mittente per la lettura del file
	 * 		  filePath - percorso dove risiede il file da salvare
	 * 		  username - utente creatore o collaboratore del documento
	 * @throws: NullPointerException - se name, username e msg sono NULL
	 * 			IllegalArgumentException - se il numero di sezioni <= 0 oppure non è una sezione esistente
	 * 			NotExistDocumentException - se il documento non esiste
	 * 			InvalidEditDocumentException - se la sezione non era in fase di editing oppure non c'è corrispondenza con l'utente che editava
	 * 			IOException - se ci sono errori durante la scrittura nel file*/
	public void endEdit(String name, int section, String filePath, String username) throws NullPointerException, IllegalArgumentException, NotExistDocumentException, IOException, InvalidEditDocumentException;
	
}
