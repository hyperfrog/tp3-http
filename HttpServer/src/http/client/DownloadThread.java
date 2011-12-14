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
	// Tous les �tats possible du t�l�chargement
	// TODO : Ajouter un texte associ� avec chaque �tat pour l'affichage dans le tableau
	public static enum DownloadState {NEW, DOWNLOADING, DONE, RETRYING, WAITING, ERROR, NOT_FOUND, FORBIDDEN};
	
	// Le �user-agent� utilis� pour signifier que c'est l'outils de t�l�chargement
	private static final String USER_AGENT = "HttpClient Downloader";
	// Interval entre les essais si il y a �chec lors du t�l�chargement
	private static final long RETRY_WAIT_TIME = 5000;
	
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
		
		// TODO : Probl�me lorsque l'adresse URL ne termine pas par un fichier. � corriger.
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
	private void openConnection()
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
				
				// Envoi la requ�te
				if (this.request.send(this.socket.getOutputStream()))
				{
					// Attend de recevoir un header pour la r�ponse
					this.response = new HttpResponse();
					HttpResponseHeader responseHeader = this.response.getHeader();
					responseHeader.receive(this.socket.getInputStream());
					
					// Tente de parser le header
					boolean responseIsGood = responseHeader.parse();
					
					// Si la r�ponse est bien form�e
					if (responseIsGood)
					{
						this.fileSize = Integer.parseInt(responseHeader.getField("Content-Length"));
						
						// Si on re�oit un code 404, la page n'existe pas
						if (this.response.getHeader().getStatusCode() == 404 || fileSize <= 0)
						{
							this.setCurrentState(DownloadState.NOT_FOUND);
						}
						// Si on re�oit un code 403, c'est peut-�tre un r�pertoire ou l'acc�s est interdit
						else if (this.response.getHeader().getStatusCode() == 403)
						{
							this.setCurrentState(DownloadState.FORBIDDEN);
						}
						// Si on re�oit un code 403, il y a une erreur dans la requ�te
						// Si on re�oit un code 501, on demande un protocol non impl�ment�
						else if (this.response.getHeader().getStatusCode() == 501 || this.response.getHeader().getStatusCode() == 403)
						{
							this.setCurrentState(DownloadState.ERROR);
						}
						else
						{
							File f = new File(this.savePath + this.fileName + "." + this.extName);
							
							// On renomme le fichier en ajoutant �(i)� � la fin du nom du fichier
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
							
							// Indique le chemin du fichier � utiliser pour sauvegarder
							this.response.setFileName(f.getAbsolutePath());
							
							this.setCurrentState(DownloadState.DOWNLOADING);
							
							// Effectue la sauvegarde
							this.response.receiveContent(this.socket.getInputStream());
							
							this.setCurrentState(DownloadState.DONE);
						}
						
						retry = false;
					}
				}
				
				// Si il y a eu une erreur lors de l'envoi de la requ�te on r�essait d'envoyer la requ�te
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
