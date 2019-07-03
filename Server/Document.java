import java.util.ArrayList;
import java.util.List;

/*Classe Documento rappresenta tutte le info sui documenti creati*/
public class Document {
	String name;
	String creator;
	List<String> collaborators;
	List<String> sections;
	int nsections;
	String addressChat; 
	
	public Document(String name, String creator, int section, String address) {
		this.name = name;
		this.creator = creator;
		this.nsections = section;
		this.sections = new ArrayList<String>(nsections);
		for(int i = 0; i < nsections; i++) 
			sections.add(i, "");
		this.collaborators = new ArrayList<String>();
		this.addressChat = address;
	}
	
	/*metodo per restituire il nome del documento
	 * @return: name - nome documento*/
	public String getName() {
		return name;
	}
	
	/*metodo per restituire il creatore del documento
	 * @return: creator - username creatore*/
	public String getCreator() {
		return creator;
	}
	
	/*metodo per restituire il numero di sezioni del documento
	 * @return: nsections - numero sezioni*/
	public int getSections() {
		return nsections;
	}
	
	/*metodo per restituire lo stato di editing di una sezione del documento
	 * @args: index - indice relativo alla sezione da cambiare lo stato
	 * @return: string - nome dell'utente che sta modificando quella sezione*/
	public String getEditState(int index) {
		return sections.get(index);
	}
	
	/*metodo per settare la porta per l'ascolto della chat multicast
	 * @args: address - porta per l'ascolto chat*/
	public synchronized void setAddress(String address) {
		addressChat = address;
	}
	
	/*metodo per restituire la porta della chat multicast associata al documento
	 * @return: addressChat -  porta per ascolto chat*/
	public String getAddress() {
		return addressChat;
	}
	
	/*metodo per settare il cambio di stato dell'editing associato a una specifica sezione
	 * @args: editor - utente che sta modificando il file
	 * 		  section - sezione specifica da modificare
	 * @return: risposta: true - stato editing cambiato
	 * 					  false - altrimenti*/
	public synchronized boolean changeEditState(String editor, int section) {
		section--;
		if(getEditState(section).equals(""))
			sections.set(section, editor);
		else
			return false;
		
		return true;
	}

	public synchronized void endEditState(String user, String editor, int section){
		section--;
		if(getEditState(section).equals(user))
			sections.set(section, editor);
	}
	
	/*metodo per aggiungere un collaboratore al documento
	 * @args: name - username dell'utente*/
	public synchronized void addCollaborator(String name) {
		collaborators.add(name);
	}
	
	/*metodo per verificare se lo specifico utente è collaboratore del documento
	 * @args: nameCollaborator - username del collaboratore
	 * @return: booleano - risposta del controllo: true - è collaboratore
	 * 											   false - non lo è*/
	public synchronized boolean isCollaborator(String nameCollaborator) {
		if(collaborators.size() == 0)
			return false;
		for(int i = 0; i < collaborators.size(); i++) {
			if(collaborators.get(i).equals(nameCollaborator))
				return true;
		}
		return false;		
	}
	
}
