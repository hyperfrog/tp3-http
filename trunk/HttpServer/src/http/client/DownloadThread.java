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
 * La classe DownloadThread cr�e un nouveau thread qui sert � t�l�charger un fichier
 * et � le sauvegarder sur l'ordinateur.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class DownloadThread implements Runnable
{
	// Tous les �tats possible du t�l�chargement
	public static enum DownloadState
	{
		NEW ("Arr�t�"),
		DOWNLOADING ("T�l�chargement en cours..."), 
		DONE ("Termin�"), 
		RETRYING (String.format("Renvoi de la requ�te dans %d secondes", RETRY_WAIT_TIME / 1000)), 
		WAITING ("En attente..."),
		WAITING_SERVER ("En attente du serveur..."), 
		ERROR ("Erreur"), 
		NOT_FOUND ("Page introuvable"), 
		FORBIDDEN ("Acc�s interdit"),
		STOPPED ("Arr�t�");
		
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
	
	// Le �user-agent� utilis� pour signifier que c'est l'outils de t�l�chargement
	private static final String USER_AGENT = "HttpClient Downloader";
	// Interval entre les essais si il y a �chec lors du t�l�chargement
	private static final long RETRY_WAIT_TIME = 5000;
	// Nombre maximal d'essais
	private static final int RETRY_ATTEMPT = 3;
	
	// L'objet AppDownload contenant la liste des t�l�chargement
	private AppDownload appDownload;
	// Socket de la connexion
	private Socket socket = null;
	// Requ�te utilis�e par le thread
	private HttpRequest request;
	// R�ponse utilis�e par le thread
	private HttpResponse response;
	// Adresse URL du fichier � t�l�charger
	private URL path;
	// Nombre d'essais fait par le thread
	private int connectionAttempt;
	
	// Destination de sauvegarde
	private String savePath;
	// Nom du fichier
	private String fileName;
	// Extenstion du fichier
	private String extName;
	
	// Repr�sentation en chaine de caract�re de l'adresse URL
	private String urlName;
	// Taille du fichier
	private int fileSize;
	// �tat du t�l�chargement
	private DownloadState currentState;
	
	// Propri�t�s du transfert
	private TransferController tc = null;
	
	/**
	 * Cr�er un nouveau thread pour t�l�charger le fichier situ� � l'adresse URL
	 * pass�e en param�tre et le sauvegarder � la destination pass�e � la suite.
	 * 
	 * @param appDownload l'objet parent contenant la liste des threads
	 * @param path l'adresse URL � sauvegarder
	 * @param savePath l'endroit o� sauvegarder le fichier
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
		
		// Construit la requ�te � envoyer
		this.buildRequest();
	}
	
	// Construit la requ�te pour tenter de r�cup�rer le fichier demand�
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
	
	// Tente d'ouvrir une connection avec le serveur demand�
	private boolean openConnection()
	{
		boolean hasFailed = false;
		
		try
		{
			// Si aucun port n'est sp�cifi� dans l'adresse URL alors on utilise le port 80 par d�faut
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
		// Limite le transfert � 100 Ko/s
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
						// Envoie la requ�te
						this.request.send(this.socket.getOutputStream());
	
						// Attend de recevoir un header pour la r�ponse
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
							// Si on re�oit un code 404, la page n'existe pas
							else if (responseHeader.getStatusCode() == 404)
							{
								this.setCurrentState(DownloadState.NOT_FOUND);
								retry = false;
							}
							// Si on re�oit un code 403, c'est peut-�tre un r�pertoire ou l'acc�s est interdit
							else if (responseHeader.getStatusCode() == 403)
							{
								this.setCurrentState(DownloadState.FORBIDDEN);
								retry = false;
							}
							// Si on re�oit un code 501, on demande un protocole non impl�ment�
							else if (responseHeader.getStatusCode() == 501)
							{
								this.setCurrentState(DownloadState.ERROR);
								retry = false;
							}
							// Si on re�oit un code 500, le serveur est dans les patates, 
							// alors on essaie de nouveau plus tard
							else if (responseHeader.getStatusCode() == 500)
							{
								this.setCurrentState(DownloadState.ERROR);
							}
							else
							{
								this.fileSize = Integer.parseInt(responseHeader.getField("Content-Length"));
								
								String checkPath = this.savePath + this.fileName + "." + this.extName;
								
								// On renomme le fichier en ajoutant �(i)� � la fin du nom du fichier
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
								
								// Indique le chemin du fichier � utiliser pour sauvegarder
								this.response.setFileName(f.getAbsolutePath());
								
								this.setCurrentState(DownloadState.DOWNLOADING);
								
								retry = false;
								
								// Effectue la sauvegarde
								boolean downSucceed = this.response.receiveContent(this.socket.getInputStream(), this.tc); 
								
								// Si la sauvegarde s'est d�roul�e sans probl�me
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
								
								// On supprime le fichier temporaire si on arr�te le t�l�chargement
								// pendant qu'il en train de sauvegarder.
								if (this.getCurrentState().equals(DownloadState.STOPPED) && tmp.exists())
								{
									tmp.delete();
								}
							}
						}
						catch (BadHeaderException e) // Incapable d'analyser le header de r�ponse 
						{
							System.err.println("He's a bad, bad server. No cookies for him today.");
						}
						
					}
					catch (BadHeaderException e) // Tentative infructueuse de cr�ation d'un header pour la requ�te
					{
						System.err.println("I'm a bad, bad client. No cookies for me today.");
						this.setCurrentState(DownloadState.ERROR);
						
						// �a ne donne rien de r�essayer; on obtiendrait le m�me r�sultat
						retry = false;
					}
					
					this.closeConnection();
				}
				else
				{
					retry = false;
				}
				
				// S'il y a eu une erreur lors de l'envoi de la requ�te, on essaie de nouveau
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
	 * Retourne vrai si le t�l�chargement est termin�, sinon faux.
	 * 
	 * @return vrai si le t�l�chargement est termin�, sinon faux
	 */
	public boolean isDone()
	{
		return this.currentState.equals(DownloadState.DONE);
	}
	
	/**
	 * Retourne l'adresse URL � t�l�charger
	 * 
	 * @return l'adresse URL � t�l�charger
	 */
	public String getUrl()
	{
		return this.urlName;
	}
	
	/**
	 * Retourne la taille de fichier � t�l�charger.
	 * 
	 * @return la taille de fichier � t�l�charger
	 */
	public String getSize()
	{
		return byteToStringRepresentation(this.fileSize);
	}
	
	/**
	 * Modifie l'�tat courant du t�l�chargement. La nouvelle �tat pass�e
	 * en param�tre ne doit pas �tre null.
	 * 
	 * @param newState nouvelle �tat du t�l�chargement. Ne doit pas �tre null.
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
	 * Retourne l'�tat courant du t�l�chargement.
	 * 
	 * @return l'�tat courant du t�l�chargement
	 */
	public DownloadState getCurrentState()
	{
		return this.currentState;
	}
	
	// Fait attendre le processus courant pour �msec� millisecondes
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
	 * Re�oit une taille en bytes et retourne repr�sentation en chaine de
	 * caract�re compr�hensible.
	 * 
	 * @param bytes la taille en bytes
	 * @return une chaine de caract�re repr�sentant la taille de fa�on lisible
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
