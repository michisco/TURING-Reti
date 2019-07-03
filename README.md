# TURING: disTribUted collaboRative edItiNG

Una soluzione del progetto di fine corso di laboratorio di Reti dell'a.a 18/19.
Tutte le specifiche del progetto risiedono nel file *ProgettoLPR1819v2.pdf*

**N.B.** Il progetto potrebbe contenere sezioni o porzioni di codice ambigui. Se qualcuno avesse idee su come migliorarlo può sottomettere una pull request.

Il progetto è diviso in due parti: 
- il server che contiene i file: MainClassServer.java, ServerTuring.java, Task.java, User.java, Document.java e CommunicationInterface.java, ReadWriteServer.java, registerUser.java, registerUserInterface.java, json-simple-1.1.1.jar e la cartella TuringException con tutte le eccezioni personali create per il progetto.
- il client che contiene i file: ClientTuring.java, MainClassTuring.java, UDPChat.java, json-simple-1.1.1.jar e la cartella TuringException con tutte le eccezioni personali create per il progetto.

Per capire come funziona potete leggere la relazione scritta nel file *Relazione_Progetto_Reti.pdf*


##Compilazione ed esecuzione del codice

Per questo progetto si è usato la libreria esterna JSON-Simple per implementare gli oggetti JSON.
Inoltre è stato scritto con Eclipse e testato su un sistema operativo Windows.

I file sono divisi in due cartelle, Server e Client.
Per poter compilare ed eseguire il progetto bisogna prima di tutto digitare nel terminale il seguente
comando per entrambe le cartelle:
**javac TuringException/*.java**
Tale comando compila tutte le classi contenute nella cartella (package) TuringException, ovvero tutte
le eccezioni personalizzate per questo progetto.
Dopodiché, si digita i seguenti comandi nell’ordine per compilare il client:
**javac UDPChat.java**
**javac -cp “.;./json-simple-1.1.1.jar” MainClassTuringClient.java**
Di conseguenza, si digita i seguenti comandi nell’ordine per compilare il server:
**avac CommunicationInterface.java
javac -cp “.;./json-simple-1.1.1.jar” User.java**
**javac registerUserInterface.java
javac -cp “.;./json-simple-1.1.1.jar” registerUser.java
javac MainClassTuringServer.java**
Successivamente, per eseguire il progetto si digita nel terminale i seguenti due comandi nell’ordine:
**java -classpath ".;./json-simple-1.1.1.jar" MainClassTuringServer
java -classpath ".;./json-simple-1.1.1.jar" MainClassTuringClient**
I comandi per eseguire le operazioni di TURING sono le stesse suggerite nelle specifiche del progetto:
**$ turing --help
usage: turing COMMAND [ARGS ...]
commands:**
*register* <username > <password > registra l'utente
*login* <username > <password > effettua il login
*logout* effettua il logout
create <doc > <numsezioni > crea un documento
share <doc > <username > condivide il documento
show <doc > <sec > mostra una sezione del documento
show <doc > mostra l'intero documento
list mostra la lista dei documenti
edit <doc > <sec > modifica una sezione del documento
end - edit <doc > <sec > fine modifica della sezione del doc.
send <msg > invia un msg sulla chat dove il msg è tra “msg”
(virgolette)
receive visualizza i msg ricevuti sulla chat
Dato che il progetto non adotta
