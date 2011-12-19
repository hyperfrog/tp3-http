package http.server;

import static util.StringUtil.stringToMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import http.common.HttpRequest;
import http.common.HttpResponse;
import http.server.event.RequestEvent;
import http.server.event.RequestEventProcessor;

/**
 * La classe Dispatcher agit comme un répartiteur qui accepte les connexions entrantes  
 * et crée pour chacune un thread où s'exécute d'une instance de serveur HTTP. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 */
public class Dispatcher implements RequestEventProcessor, Runnable
{
	// Nom du fichier contenant la liste des extensions et des types MIME correspondants
	private final static String MIME_TYPES_FILE = "mime_types.txt";
	
	// Nombre max. de de connexions simultanées
	private final static int MAX_CONNECTIONS = 2;
	
	// Nombre max. de connexions en attente dans la file 
	private final static int BACKLOG = 10;

	// Chemin absolu du dossier où se trouvent les fichiers nécessaires au serveur
	private final String serverPath;
	
	// Chemin relatif du dossier où se trouvent les fichiers du site Web à servir
	private final String siteFolder;
	
	// Addresse IP utilisée pour les connexions entrantes
	private final String ipAddress; 
	
	// Port utilisé pour les connexions entrantes
	private final int portNum;
	
	// Dictionnaire des extensions et des types MIME correspondants
	private static Map<String, String> mimeTypes = null;
	
	// Liste des serveurs actifs
	private List<HttpServerThread> serverThreads = new ArrayList<HttpServerThread>();
	
	// Compteur de transactions
	private int transactionId = 1; 
	
	// Indique si le thread du répartiteur doit s'arrêter
	private volatile Thread runThread;
	
	/**
	 * Construit un répartiteur de requêtes.
	 * 
	 * @param serverPath Chemin absolu du dossier où se trouvent les fichiers nécessaires au serveur
	 * @param siteFolder Chemin relatif du dossier où se trouvent les fichiers du site Web à servir
	 * @param ipAddress Addresse IP utilisée pour les connexions entrantes
	 * @param portNum Port utilisé pour les connexions entrantes
	 */
	public Dispatcher(String serverPath, String siteFolder, String ipAddress, int portNum)
	{
		this.serverPath = serverPath;
		this.siteFolder = siteFolder;
		this.ipAddress = ipAddress;
		this.portNum = portNum;
		
		// Lecture du fichiers des types MIME à la première instanciation de la classe
		if (Dispatcher.mimeTypes == null)
		{
			Dispatcher.mimeTypes = new HashMap<String, String>();

			File mtFile = new File(serverPath + MIME_TYPES_FILE);
			FileInputStream fis = null;
			try
			{
				fis = new FileInputStream(mtFile);
				byte[] mtData = new byte[(int) mtFile.length()];
				fis.read(mtData);
				Dispatcher.mimeTypes = stringToMap(new String(mtData), "\n", "=", true);
			}
			catch (IOException e)
			{
				System.err.println("Problème de lecture du fichier des types MIME : " + e.getMessage());
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
	 * Arrête le thread associé à cet objet.
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
			
			// Programme un délai d'attente de connexion pour éviter un blocage
			listener.setSoTimeout(100);
			
			Socket clientSocket;

			while (myThread == this.runThread) 
			{
				try
				{
					clientSocket = listener.accept();
					
					// Si le nombre max. de connexions est atteint 
					while (this.serverThreads.size() >= MAX_CONNECTIONS)
					{
						// Attend un peu
						try { Thread.sleep(100); } catch (InterruptedException unused) {}

						// Enlève les serveurs inactifs de la liste
						Iterator<HttpServerThread> iter = this.serverThreads.iterator();
						
						while (iter.hasNext())
						{
						    if (iter.next().isDone()) { iter.remove(); }
						}
					}

					// Crée un nouveau serveur 
					HttpServerThread server = new HttpServerThread(clientSocket, serverPath, siteFolder, Dispatcher.mimeTypes, this);
					this.serverThreads.add(server);
					
					// Crée le thread du serveur en mode daemon pour éviter qu'il reste vivant après une demande d'arrêt du répartiteur
					Thread t = new Thread(server);
					t.setDaemon(true);
					t.setName("" + this.transactionId++);
					t.start();
				}
				catch (SocketTimeoutException e)
				{
				}
				catch (IOException e)
				{
					System.err.println("Erreur d'E/S à l'acceptation d'une connexion :\n" + e.getMessage());
					e.printStackTrace();
				}
			}
			
			// Arrête tous les serveurs encore actifs
			for (HttpServerThread server : serverThreads)
			{
				if (!server.isDone()) { server.stop(); }
			}
			
			System.out.println("Arrêt dans 3 secondes...");
			try { Thread.sleep(3000); } catch (InterruptedException unused) {}

		}
		catch (IOException e)
		{
			System.out.println("Erreur d'E/S au «binding» du socket :\n" + e.getMessage());
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
		
		// Intercepte une requête pour la ressource "/paramecho" 
		if (resourcePath.equals("/paramecho"))
		{
			evt.cancel = true;

			response.getHeader().setStatusCode(200);
			response.getHeader().setCacheable(false);

			// Renvoie au client les paramètres qu'il a envoyés
			String content = new String();
			
			Set<String> keys = request.getHeader().getParamKeySet();
			
			for(String key : keys)
			{
				String value = request.getHeader().getParam(key);
				content += key + " = " + value + "\n";
			}
			
//			res.setContent(content, Charset.forName("ISO-8859-1"));
			response.setContent(content, Charset.forName("UTF-8"));

		}
		// Intercepte une requête pour la ressource "/admin" 
		else if (resourcePath.equals("/admin"))
		{
			evt.cancel = true;

			response.getHeader().setStatusCode(200);

			// Lit le paramètre "command"
			String command = request.getHeader().getParam("command");
			
			String content = "Administration du serveur :\n\n";

			// Si la commande est "shutdown", arrête le serveur
			if (command != null && command.equals("shutdown"))
			{
				content += "Arrêt du serveur.\n\n";
			}

			response.setContent(content, Charset.forName("ISO-8859-1"));
			
			if (command != null && command.equals("shutdown"))
			{
				this.stop();
			}
			
//			try { Thread.sleep(500); } catch (InterruptedException unused) {}
		}
	}
}
