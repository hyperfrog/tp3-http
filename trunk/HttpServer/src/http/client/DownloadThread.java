package http.client;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import http.common.BadHeaderException;
import http.common.HttpRequest;
import http.common.HttpResponse;
import http.common.TransferController;
import http.common.HttpResponseHeader;

/**
 * La classe DownloadThread crée un nouveau thread qui sert à télécharger un fichier
 * et à le sauvegarder sur l'ordinateur.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class DownloadThread implements Runnable
{
	// Tous les états possible du téléchargement
	public static enum DownloadState
	{
		NEW ("Arrêté"),
		DOWNLOADING ("Téléchargement en cours..."), 
		DONE ("Terminé"), 
		RETRYING (String.format("Renvoi de la requête dans %d secondes", RETRY_WAIT_TIME / 1000)), 
		WAITING ("En attente..."),
		WAITING_SERVER ("En attente du serveur..."), 
		ERROR ("Erreur"), 
		NOT_FOUND ("Page introuvable"), 
		FORBIDDEN ("Accès interdit"),
		STOPPED ("Arrêté");
		
		private final String message;
		
		DownloadState(String message)
		{
			this.message = message;
		}
		
		@Override
		public String toString()
		{
			return this.message;
		}
	};
	
	// Le «user-agent» utilisé pour signifier que c'est l'outils de téléchargement
	private static final String USER_AGENT = "HttpClient Downloader";
	// Interval entre les essais si il y a échec lors du téléchargement
	private static final long RETRY_WAIT_TIME = 5000;
	// Nombre maximal d'essais
	private static final int RETRY_ATTEMPT = 3;
	
	// L'objet AppDownload contenant la liste des téléchargement
	private AppDownload appDownload;
	// Socket de la connexion
	private Socket socket = null;
	// Requête utilisée par le thread
	private HttpRequest request;
	// Réponse utilisée par le thread
	private HttpResponse response;
	// Adresse URL du fichier à télécharger
	private URL path;
	// Nombre d'essais fait par le thread
	private int connectionAttempt;
	
	// Destination de sauvegarde
	private String savePath;
	// Nom du fichier
	private String fileName;
	// Extenstion du fichier
	private String extName;
	
	// Représentation en chaine de caractère de l'adresse URL
	private String urlName;
	// Taille du fichier
	private int fileSize;
	// État du téléchargement
	private DownloadState currentState;
	
	// Propriétés du transfert
	private TransferController tc = null;
	
	/**
	 * Créer un nouveau thread pour télécharger le fichier situé à l'adresse URL
	 * passée en paramètre et le sauvegarder à la destination passée à la suite.
	 * 
	 * @param appDownload l'objet parent contenant la liste des threads
	 * @param path l'adresse URL à sauvegarder
	 * @param savePath l'endroit où sauvegarder le fichier
	 */	
	public DownloadThread(AppDownload appDownload, URL path, String savePath)
	{
		this.appDownload = appDownload;
		this.path = path;
		this.savePath = savePath;
		
		this.urlName = path.toExternalForm();
		this.currentState = DownloadState.NEW;
		this.connectionAttempt = 0;
		
		this.fileName = path.getPath().substring(path.getPath().lastIndexOf("/") + 1, path.getPath().lastIndexOf("."));
		this.extName = path.getPath().substring(path.getPath().lastIndexOf(".") + 1, path.getPath().length());
		
		// Construit la requête à envoyer
		this.buildRequest();
	}
	
	// Construit la requête pour tenter de récupérer le fichier demandé
	private void buildRequest()
	{
		this.request = new HttpRequest();
		this.request.getHeader().setMethod("GET");
		this.request.getHeader().setFullPath(this.path.getFile());
		this.request.getHeader().setProtocol("HTTP/1.1");
		
		this.request.getHeader().setField("Host", this.path.getHost());
		this.request.getHeader().setField("User-Agent", DownloadThread.USER_AGENT);
		this.request.getHeader().setField("Accept", "*/*");
	}
	
	// Tente d'ouvrir une connection avec le serveur demandé
	private boolean openConnection()
	{
		boolean hasFailed = false;
		
		try
		{
			// Si aucun port n'est spécifié dans l'adresse URL alors on utilise le port 80 par défaut
			this.socket = new Socket(this.path.getHost(), (this.path.getPort() != -1) ? this.path.getPort() : this.path.getDefaultPort());
		}
		catch (UnknownHostException e)
		{
			hasFailed = true;
			e.printStackTrace();
		}
		catch (ConnectException e)
		{
		    hasFailed = true;
		    e.printStackTrace();
		} 
		catch (IOException e)
		{
			hasFailed = true;
			e.printStackTrace();
		}
		
		if (hasFailed)
		{
			this.setCurrentState(DownloadState.ERROR);
		}
		
		return !hasFailed;
	}
	
	@Override
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		// Limite le transfert à 100 Ko/s
		this.tc = new TransferController(100);
		
		try
		{
			boolean retry = true;
			
			do
			{
				this.setCurrentState(DownloadState.WAITING_SERVER);
				
				if (this.openConnection())
				{
					try
					{						
						// Envoie la requête
						this.request.send(this.socket.getOutputStream());
	
						// Attend de recevoir un header pour la réponse
						this.response = new HttpResponse();
						HttpResponseHeader responseHeader = this.response.getHeader();
						responseHeader.receive(this.socket.getInputStream());
						
						try
						{
							// Tente de parser le header
							responseHeader.parse();
							
							if (responseHeader.getStatusCode() == 400)
							{
								this.setCurrentState(DownloadState.ERROR);
								retry = false;
							}
							// Si on reçoit un code 404, la page n'existe pas
							else if (responseHeader.getStatusCode() == 404)
							{
								this.setCurrentState(DownloadState.NOT_FOUND);
								retry = false;
							}
							// Si on reçoit un code 403, c'est peut-être un répertoire ou l'accès est interdit
							else if (responseHeader.getStatusCode() == 403)
							{
								this.setCurrentState(DownloadState.FORBIDDEN);
								retry = false;
							}
							// Si on reçoit un code 501, on demande un protocole non implémenté
							else if (responseHeader.getStatusCode() == 501)
							{
								this.setCurrentState(DownloadState.ERROR);
								retry = false;
							}
							// Si on reçoit un code 500, le serveur est dans les patates, 
							// alors on essaie de nouveau plus tard
							else if (responseHeader.getStatusCode() == 500)
							{
								this.setCurrentState(DownloadState.ERROR);
							}
							else
							{
								this.fileSize = Integer.parseInt(responseHeader.getField("Content-Length"));
								
								String checkPath = this.savePath + this.fileName + "." + this.extName;
								
								// On renomme le fichier en ajoutant «(i)» à la fin du nom du fichier
								if (new File(checkPath).exists() || new File(checkPath + ".tmp").exists())
								{
									int i = 1;
									do
									{
										checkPath = this.savePath + this.fileName + " (" + i + ")." + this.extName;
										i++;
									}
									while (new File(checkPath).exists() || new File(checkPath + ".tmp").exists());
								}
								
								File f = new File(checkPath);
								File tmp = new File(checkPath + ".tmp");
								
								// Indique le chemin du fichier à utiliser pour sauvegarder
								this.response.setFileName(f.getAbsolutePath());
								
								this.setCurrentState(DownloadState.DOWNLOADING);
								
								retry = false;
								
								// Effectue la sauvegarde
								boolean downSucceed = this.response.receiveContent(this.socket.getInputStream(), this.tc); 
								
								// Si la sauvegarde s'est déroulée sans problème
								if (downSucceed)
								{
									this.setCurrentState(DownloadState.DONE);
								}
								// Si il y a eu une erreur lors de la sauvegarde, on supprime le fichier
								// temporaire et on essaie de nouveau plus tard
								else if (!downSucceed && !this.getCurrentState().equals(DownloadState.STOPPED))
								{
									this.setCurrentState(DownloadState.ERROR);
									if (tmp.exists())
									{
										tmp.delete();
									}
									retry = true;
								}
								
								// On supprime le fichier temporaire si on arrête le téléchargement
								// pendant qu'il en train de sauvegarder.
								if (this.getCurrentState().equals(DownloadState.STOPPED) && tmp.exists())
								{
									tmp.delete();
								}
							}
						}
						catch (BadHeaderException e) // Incapable d'analyser le header de réponse 
						{
							System.err.println("He's a bad, bad server. No cookies for him today.");
						}
						
					}
					catch (BadHeaderException e) // Tentative infructueuse de création d'un header pour la requête
					{
						System.err.println("I'm a bad, bad client. No cookies for me today.");
						this.setCurrentState(DownloadState.ERROR);
						
						// Ça ne donne rien de réessayer; on obtiendrait le même résultat
						retry = false;
					}
					
					this.closeConnection();
				}
				else
				{
					retry = false;
				}
				
				// S'il y a eu une erreur lors de l'envoi de la requête, on essaie de nouveau
				if (retry)
				{
					this.connectionAttempt++;
					
					this.setCurrentState(DownloadState.RETRYING);
					this.sleepFor(DownloadThread.RETRY_WAIT_TIME);
				}
			}
			while (retry && this.connectionAttempt < RETRY_ATTEMPT);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		if (this.connectionAttempt >= RETRY_ATTEMPT)
		{
			this.setCurrentState(DownloadState.ERROR);
		}
	}
	
	// Ferme la connection
	private void closeConnection()
	{
		try
		{
			this.socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Retourne vrai si le téléchargement est terminé, sinon faux.
	 * 
	 * @return vrai si le téléchargement est terminé, sinon faux
	 */
	public boolean isDone()
	{
		return this.currentState.equals(DownloadState.DONE);
	}
	
	/**
	 * Retourne l'adresse URL à télécharger
	 * 
	 * @return l'adresse URL à télécharger
	 */
	public String getUrl()
	{
		return this.urlName;
	}
	
	/**
	 * Retourne la taille de fichier à télécharger.
	 * 
	 * @return la taille de fichier à télécharger
	 */
	public String getSize()
	{
		return byteToStringRepresentation(this.fileSize);
	}
	
	/**
	 * Modifie l'état courant du téléchargement. La nouvelle état passée
	 * en paramètre ne doit pas être null.
	 * 
	 * @param newState nouvelle état du téléchargement. Ne doit pas être null.
	 */
	public void setCurrentState(DownloadState newState)
	{
		if (newState != null)
		{
			this.currentState = newState;
			
			if (newState.equals(DownloadState.STOPPED) && this.tc != null)
			{
				this.tc.stopped = true;
			}

			this.appDownload.update(this);
		}
	}
	
	/**
	 * Retourne l'état courant du téléchargement.
	 * 
	 * @return l'état courant du téléchargement
	 */
	public DownloadState getCurrentState()
	{
		return this.currentState;
	}
	
	// Fait attendre le processus courant pour «msec» millisecondes
	private void sleepFor(long msec)
	{
		try
		{
			Thread.sleep(msec);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Reçoit une taille en bytes et retourne représentation en chaine de
	 * caractère compréhensible.
	 * 
	 * @param bytes la taille en bytes
	 * @return une chaine de caractère représentant la taille de façon lisible
	 */
	public static String byteToStringRepresentation(long bytes)
	{
		final int unit = 1024;
		
		if (bytes == 0)
		{
			return "0";
		}
		
		if (bytes < unit)
		{
			return bytes + " o";
		}
		
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		
		char prefix = "KMGTPE".charAt(exp - 1);
		
		return String.format("%.1f %so", bytes / Math.pow(unit, exp), prefix);
	}
}
