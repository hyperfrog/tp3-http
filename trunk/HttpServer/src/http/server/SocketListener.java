package http.server;

import static util.BasicString.stringToMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import http.common.HttpRequest;
import http.common.HttpResponse;
import http.server.event.RequestEvent;
import http.server.event.RequestEventProcessor;

/**
 * La classe SocketListener agit comme un r�partiteur qui accepte les connexions entrantes  
 * et cr�e pour chacune un thread pour l'ex�cution d'une instance de serveur HTTP. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 */
public class SocketListener implements RequestEventProcessor, Runnable
{
	// Nom du fichier contenant la liste des extensions et des types MIME correspondants
	private final static String MIME_TYPES_FILE = "mime_types.txt";
	
	// Nombre max. de connexions en attente dans la file 
	private final static int BACKLOG = 10;

	// Nombre max. de de threads simultan�s
	private final static int MAX_THREADS = 10;
	
	// Chemin absolu du dossier o� se trouvent les fichiers n�cessaires au serveur
	private final String serverPath;
	
	// Chemin relatif du dossier o� se trouvent les fichiers du site Web � servir
	private final String siteFolder;
	
	// Addresse IP utilis�e pour les connexions entrantes
	private final String ipAddress; 
	
	// Port utilis� pour les connexions entrantes
	private final int portNum;
	
	// Dictionnaire des extensions et des types MIME correspondants
	private static Map<String, String> mimeTypes = null;
	
	private volatile Thread runThread;
	
	/**
	 * Construit un �couteur de connexions entrantes.
	 * 
	 * @param serverPath Chemin absolu du dossier o� se trouvent les fichiers n�cessaires au serveur
	 * @param siteFolder Chemin relatif du dossier o� se trouvent les fichiers du site Web � servir
	 * @param ipAddress Addresse IP utilis�e pour les connexions entrantes
	 * @param portNum Port utilis� pour les connexions entrantes
	 */
	public SocketListener(String serverPath, String siteFolder, String ipAddress, int portNum)
	{
		this.serverPath = serverPath;
		this.siteFolder = siteFolder;
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		
		// Lecture du fichiers des types MIME � la premi�re instanciation de la classe
		if (SocketListener.mimeTypes == null)
		{
			// Read MIME types
			SocketListener.mimeTypes = new HashMap<String, String>();

			File mtFile = new File(serverPath + MIME_TYPES_FILE);
			FileInputStream fis = null;
			try
			{
				// Read the file   
				fis = new FileInputStream(mtFile);
				byte[] mtData = new byte[(int) mtFile.length()];
				fis.read(mtData);
				SocketListener.mimeTypes = stringToMap(new String(mtData), "\n", "=", true);
			}
			catch (IOException e)
			{
				System.err.println("Probl�me de lecture du fichier des types MIME : " + e.getMessage());
			}
			finally
			{
				if (fis != null)
				{
					try { fis.close(); } catch (IOException unused) {}
				}
			}
		}
	}
	
	/**
	 * Arr�te le thread li� � cet objet.
	 */
	public void stop()
	{
		Thread tmpRunThread = this.runThread;
		this.runThread = null;
		if (tmpRunThread != null)
		{
//			tmpRunThread.interrupt();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
        Thread myThread = Thread.currentThread();
        this.runThread = myThread;
        
		try
		{
			ServerSocket listener = new ServerSocket(portNum, BACKLOG, InetAddress.getByName(ipAddress));
			
			// Programme un d�lai d'attente de connexion pour �viter un blocage
			listener.setSoTimeout(100);
			
			Socket clientSocket;

			while (myThread == this.runThread) 
			{
				try
				{
					clientSocket = listener.accept();
					int nbThreads = Thread.activeCount();
					
					System.out.print(String.format("Il y a pr�sentement %d thread(s) actif(s).\n\n", nbThreads));
					
					// Si le nombre max. de threads est atteint, attend un peu 
					while (nbThreads >= MAX_THREADS)
					{
						try { Thread.sleep(100); } catch (InterruptedException unused) {}
					}

					// Cr�e un serveur pour le thread
					HttpServerThread server = new HttpServerThread(clientSocket, serverPath, siteFolder, SocketListener.mimeTypes, this);
					
					// Cr�e le thread en mode daemon pour �viter qu'il reste vivant apr�s une demande d'arr�t du serveur
					Thread t = new Thread(server);
					t.setDaemon(true);
					t.start();
				}
				catch (SocketTimeoutException e)
				{
//					System.err.println("D�lai de connexion d�pass� � l'acceptation du socket :\n" + e.getMessage());
//					e.printStackTrace();
				}
				catch (IOException e)
				{
					System.err.println("Erreur d'E/S � l'acceptation du socket :\n" + e.getMessage());
					e.printStackTrace();
				}
			}
			
			System.out.println("Arr�t dans 3 secondes...");
			try { Thread.sleep(3000); } catch (InterruptedException unused) {}

		}
		catch (IOException e)
		{
			System.out.println("Erreur d'E/S � l'�coute du socket :\n" + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see http.server.event.RequestEventProcessor#requestEventReceived(http.server.event.RequestEvent)
	 */
	@Override
	public void requestEventReceived(RequestEvent evt)
	{
		HttpServerThread thread = ((HttpServerThread)evt.getSource());
		HttpRequest request = thread.getRequest();
		HttpResponse response = thread.getResponse();
		
		String resourcePath = request.getHeader().getPath();
		
		if (resourcePath.equals("/paramecho"))
		{
			evt.cancel = true;

			response.getHeader().setStatusCode(200);
			response.setCacheable(false);

			String content = new String();
			
			Set<String> keys = request.getHeader().getParamKeySet();
			
			for(String key : keys)
			{
				String value = request.getHeader().getParam(key);
//				if (value != null)
				{
					content += key + " = " + value + "\n";
				}
			}
			
//			res.setContent(content, Charset.forName("ISO-8859-1"));
			response.setContent(content, Charset.forName("UTF-8"));

		}
		else if (resourcePath.equals("/admin"))
		{
			evt.cancel = true;

			response.getHeader().setStatusCode(200);

			String command = request.getHeader().getParam("command");
			
			String content = "Administration du serveur :\n\n";

			if (command != null && command.equals("shutdown"))
			{
				content += "Arr�t du serveur.\n\n";
			}

			response.setContent(content, Charset.forName("ISO-8859-1"));
			
			if (command != null && command.equals("shutdown"))
			{
				this.stop();
			}
			
			try { Thread.sleep(500); } catch (InterruptedException unused) {}
		}
	}
}
