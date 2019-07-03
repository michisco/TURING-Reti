import java.util.Scanner;

public class MainClassTuringServer {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
				
		ServerTuring server = new ServerTuring(3100);
		Thread t = new Thread(server);
		Scanner scan;
		scan=new Scanner(System.in);
		
		t.start();
		System.out.println("Server attivato");
		System.out.println("---- Digita 0 per chiudere il server ---");
		
		int input=1;
		while(input!=0) {
			input=scan.nextInt();
		}
		
		server.serverStop();
		System.out.println("Server chiuso");
		System.exit(0);
	}
}
