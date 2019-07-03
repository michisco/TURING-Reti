import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*classe che gestisce la concorrenza della lettura/scrittura dei documenti nel server*/
public class ReadWriteServer {

	String pathDoc; 
	//inizializzo lock di tipo ReadWriteLock
	private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
	private Lock read = readWriteLock.readLock();
	private Lock write = readWriteLock.writeLock();
	
	/*public ReadWriteServer(String path) {
		this.pathDoc = path;
	}*/
	public ReadWriteServer() {}

	/*metodo che legge il contenuto di un file*/
	@SuppressWarnings("resource")
	public void sendFile(File target, String file) throws IOException {
		//prendo lock lettore
		read.lock();
		
		FileChannel destinationChannel = null;
		FileChannel sourceChannel = null;
		try {
			destinationChannel = new FileOutputStream(target).getChannel();
			sourceChannel = new FileInputStream (file).getChannel();
			sourceChannel.transferTo (0, sourceChannel.size(), destinationChannel);
			sourceChannel.close();
	    } finally {
	        if (null != sourceChannel) {
	        	sourceChannel.close();
	        }
	        if (null != destinationChannel) {
	        	destinationChannel.close();
	        }
	    }
		
		//rilascio lock lettore dopo aver letto il documento
		read.unlock();
	}

	/*metodo per poter scrivere il contenuto del file scaricato in un file destinazione*/
	@SuppressWarnings("resource")
	public synchronized void writeFile(String target, File file) throws IOException {
		write.lock();
		
		FileChannel destinationChannel = null;
		FileChannel sourceChannel = null;
		try {
			destinationChannel = new FileOutputStream(target).getChannel();
			sourceChannel = new FileInputStream (file).getChannel();
			sourceChannel.transferTo (0, sourceChannel.size(), destinationChannel);
			sourceChannel.close();
	    } finally {
	        if (null != sourceChannel) {
	        	sourceChannel.close();
	        }
	        if (null != destinationChannel) {
	        	destinationChannel.close();
	        }
	    }
		write.unlock();
	}
}
