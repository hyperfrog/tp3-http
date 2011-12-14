package http.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import http.common.HttpRequest;
import http.common.HttpResponse;
import http.common.HttpResponseHeader;

/**
 * La classe DownloadThread
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class DownloadThread extends Thread
{
	// Tous les états possible du téléchargement
	// TODO : Ajouter un texte associé avec chaque état pour l'affichage dans le tableau
	public static enum DownloadState {NEW, DOWNLOADING, DONE, RETRYING, WAITING, ERROR, NOT_FOUND, FORBIDDEN};
	
	// Le «user-agent» utilisé pour signifier que c'est l'outils de téléchargement
	private static final String USER_AGENT = "HttpClient Downloader";
	// Interval entre les essais si il y a échec lors du téléchargement
	private static final long RETRY_WAIT_TIME = 5000;
	
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
		
		// TODO : Problème lorsque l'adresse URL ne termine pas par un fichier. À corriger.
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
	private void openConnection()
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
		catch (IOException e)
		{
			hasFailed = true;
			e.printStackTrace();
		}
		
		if (hasFailed)
		{
			this.setCurrentState(DownloadState.ERROR);
		}
	}
	
	@Override
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			boolean retry = true;
			
			do
			{
				if (retry)
				{
					this.openConnection();
				}
				
				// Envoi la requête
				if (this.request.send(this.socket.getOutputStream()))
				{
					// Attend de recevoir un header pour la réponse
					this.response = new HttpResponse();
					HttpResponseHeader responseHeader = this.response.getHeader();
					responseHeader.receive(this.socket.getInputStream());
					
					// Tente de parser le header
					boolean responseIsGood = responseHeader.parse();
					
					// Si la réponse est bien formée
					if (responseIsGood)
					{
						this.fileSize = Integer.parseInt(responseHeader.getField("Content-Length"));
						
						// Si on reçoit un code 404, la page n'existe pas
						if (this.response.getHeader().getStatusCode() == 404 || fileSize <= 0)
						{
							this.setCurrentState(DownloadState.NOT_FOUND);
						}
						// Si on reçoit un code 403, c'est peut-être un répertoire ou l'accès est interdit
						else if (this.response.getHeader().getStatusCode() == 403)
						{
							this.setCurrentState(DownloadState.FORBIDDEN);
						}
						// Si on reçoit un code 403, il y a une erreur dans la requête
						// Si on reçoit un code 501, on demande un protocol non implémenté
						else if (this.response.getHeader().getStatusCode() == 501 || this.response.getHeader().getStatusCode() == 403)
						{
							this.setCurrentState(DownloadState.ERROR);
						}
						else
						{
							File f = new File(this.savePath + this.fileName + "." + this.extName);
							
							// On renomme le fichier en ajoutant «(i)» à la fin du nom du fichier
							if (f.exists())
							{
								int i = 1;
								do
								{
									f.renameTo(new File(this.savePath + this.fileName + " (" + i + ")." + this.extName));
									i++;
								}
								while (f.exists());
							}
							
							// Indique le chemin du fichier à utiliser pour sauvegarder
							this.response.setFileName(f.getAbsolutePath());
							
							this.setCurrentState(DownloadState.DOWNLOADING);
							
							// Effectue la sauvegarde
							this.response.receiveContent(this.socket.getInputStream());
							
							this.setCurrentState(DownloadState.DONE);
						}
						
						retry = false;
					}
				}
				
				// Si il y a eu une erreur lors de l'envoi de la requête on réessait d'envoyer la requête
				if (retry)
				{
					this.setCurrentState(DownloadState.RETRYING);
					this.sleepFor(DownloadThread.RETRY_WAIT_TIME);
				}
				
				this.closeConnection();
			}
			while (retry);
		}
		catch (IOException e)
		{
			e.printStackTrace();
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
		int unit = 1000;
		
		if (bytes < unit)
		{
			return bytes + " B";
		}
		
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		
		char prefix = "KMGTPE".charAt(exp - 1);
		
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), prefix);
	}
}
