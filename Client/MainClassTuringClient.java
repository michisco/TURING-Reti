import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class MainClassTuringClient {
	
	public static void main(String[] args) throws RemoteException {
		
		String[] commands = null;
		//variabile che rappresenta lo stato di login
		boolean stateLogged = false;
		//variabile che rappresenta lo stato di editing
		boolean stateEdit = false;
		ClientTuring client = null;
		try {
			client = new ClientTuring();
			client.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//creo un ciclo continuo finché l'utente non decide di uscire dal programma
		while(true) {
			System.out.print("$ ");
			
			//l'utente deve digitare il comando
			try{
				@SuppressWarnings("resource")
				Scanner in = new Scanner(System.in);
				String op = in.nextLine();
				commands = op.split(" ");
			}catch(Exception e) {
				System.out.println("Close session");
			}
			
			//controllo che il comando sia corretto
			try {
				if(commands[0].equals("turing")) {
					/*menù dei comandi possibili*/
					//comando help
					if(commands[1].equals("--help")) {
						System.out.println("usage : turing COMMAND [ ARGS ...]\r\n" + 
								"commands :\r\n" + 
								"register < username > < password > registra l ’ utente\r\n" + 
								"login < username > < password > effettua il login\r\n" + 
								"logout effettua il logout\r\n" + 
								"create <doc > < numsezioni > crea un documento\r\n" + 
								"share <doc > < username > condivide il documento\r\n" + 
								"show <doc > <sec > mostra una sezione del documento\r\n" + 
								"show <doc > mostra l ’ intero documento\r\n" + 
								"list mostra la lista dei documenti\r\n" + 
								"edit <doc > <sec > modifica una sezione del documento\r\n" + 
								"end - edit <doc > <sec > fine modifica della sezione del doc .\r\n" + 
								"send <msg > invia un msg sulla chat\r\n" + 
								"receive visualizza i msg ricevuti sulla chat");	
					}
					//comando di registrazione
					else if(commands[1].equals("register")) {
						if(commands.length == 4) {
							if(!stateLogged)
								client.register(commands[2], commands[3]);
							else
								throw new Exception("Comando disponibile solamente quando non si è loggati");
						}
						else
							throw new Exception("register < username > < password >");
					}
					//comando per loggare al servizio
					else if(commands[1].equals("login")) {
						if(commands.length == 4) {
							if(!stateLogged) 
								stateLogged = client.login(commands[2], commands[3]);
							else
								throw new Exception("Hai eseguito già il login");		
						}
						else
							throw new Exception("login < username > < password >");		
					}
					/*menù dei comandi possibili quando si è loggati*/
					//comando per fare il logout al servizio
					else if(commands[1].equals("logout")) {		
						if(!stateEdit) {
							try {
								stateLogged = client.logout(stateLogged);
								if(!stateLogged) System.exit(0);
							}catch(NullPointerException e) {
								throw new Exception("Non puoi fare il logout se non sei loggato");
							}
						}
						else
							throw new Exception("Questo comando non puoi usarlo quando stai modificando un documento");	
					}
					//comando per la creazione di un nuovo documento
					else if(commands[1].equals("create")) {
						if(commands.length == 4) {
							if(!stateEdit) {
								if(stateLogged)
									client.create(commands[2], commands[3]);
								else
									throw new Exception("Devi prima fare il login per usare questo comando");
							}
							else
								throw new Exception("Questo comando non puoi usarlo quando stai modificando un documento");		
						}
						else
							throw new Exception("create <doc > < numsezioni >");
					}
					//comando per la condivisione del documento
					else if(commands[1].equals("share")) {
						if(commands.length == 4) {
							if(!stateEdit) {
								if(stateLogged)
									client.share(commands[2], commands[3]);
								else
									throw new Exception("Devi prima fare il login per usare questo comando");
							}
							else
								throw new Exception("Questo comando non puoi usarlo quando stai modificando un documento");					
						}
						else
							throw new Exception("share <doc > < username >");
					}
					//comando per mostrare una sezione del documento o il documento
					else if(commands[1].equals("show")) {
						//caso in cui l'utente vuole vedere tutto il documento
						if(commands.length == 3) {
							if(!stateEdit) {
								if(stateLogged)
									client.showAll(commands[2]);
								else
									throw new Exception("Devi prima fare il login per usare questo comando");
							}
							else
								throw new Exception("Questo comando non puoi usarlo quando stai modificando un documento");					
							}
						//caso in cui l'utente vuole vedere una sezione del documento
						else if(commands.length == 4) {
							if(!stateEdit) {
								if(stateLogged)
									client.showSection(commands[2], commands[3]);
								else
									throw new Exception("Devi prima fare il login per usare questo comando");
							}
							else
								throw new Exception("Questo comando non puoi usarlo quando stai modificando un documento");					
						}
						else
							throw new Exception("show <doc > oppure show <doc > < sec >");
					}
					//comando per mostrare la lista dei documenti
					else if(commands[1].equals("list")) {
						if(!stateEdit) {
							if(stateLogged)
								client.list();
							else
								throw new Exception("Devi prima fare il login per usare questo comando");
						}
						else
							throw new Exception("Questo comando non puoi usarlo quando stai modificando un documento");					
					}
					//comando per modificare una sezione del documento
					else if(commands[1].equals("edit")) {
						if(commands.length == 4) {
							if(stateLogged && !stateEdit) 
								stateEdit = client.edit(commands[2], commands[3]);
							else
								throw new Exception("Non puoi usare questo comando se non sei loggato o se stai già editando");
						}
						else
							throw new Exception("edit <doc > <sec >");
					}
					//comando per porre fine alla modifica del documento
					else if(commands[1].equals("end-edit")) {
						if(commands.length == 4) {
							if(stateLogged && stateEdit) 
								stateEdit = client.endEdit(commands[2], commands[3]);
							else
								throw new Exception("Non puoi usare questo comando se non sei loggato o se non stai editando");
						}
						else
							throw new Exception("end-edit <doc > <sec >");
					}
					//comando per inviare un messaggio sulla chat
					else if(commands[1].equals("send")) {
						if(commands.length >= 3) {
							if(commands[2].contains("\"")) {
								String msg = "";
								if(commands.length == 3) {
									//prendo la sottostringa compresa tra le due virgolette
									commands[2] = commands[2].substring(commands[2].indexOf("\"") + 1);
									commands[2] = commands[2].substring(0, commands[2].indexOf("\""));
									msg = commands[2];
								}
								else{
									//prendo tutto il messaggio all'interno delle virgolette
									commands[2] = commands[2].substring(1, commands[2].length());
									msg = commands[2];
									for(int i = 3; i < commands.length - 1; i++)
										msg = msg +" "+ commands[i];
									
									if(!commands[commands.length - 1].contains("\""))
										throw new Exception("send <\"msg\">");
									commands[commands.length - 1] = commands[commands.length - 1].substring(0, commands[commands.length - 1].length() - 1);
									msg = msg +" "+ commands[commands.length - 1];
								}
								
								if(stateLogged && stateEdit) {
									client.send(msg);
								}
								else
									throw new Exception("Non puoi usare questo comando se non sei loggato o se non stai editando");
							}
							else
								throw new Exception("send \"msg\"");
						}
						else
							throw new Exception("send <msg >");
					}
					//comando per mostrare tutti i messaggi ricevuti nella chat
					else if(commands[1].equals("receive")) {
						if(stateLogged && stateEdit) {
							client.receive();
						}
						else
							throw new Exception("Non puoi usare questo comando se non sei loggato o se non stai editando");
					}
					//default
					else 
						throw new Exception("Unknown command");
				}
				else if(commands[0].equals(""))
					continue;
				else 
					throw new Exception("turing COMMAND [ ARGS ...]");
			}catch(IOException ex) {
				System.out.println("Errore di connessione: "+ex.getMessage());
				System.exit(0);
			}
			catch(Exception ex) {
				System.out.println("Errore: "+ex.getMessage());
			}
		}
	}
}
