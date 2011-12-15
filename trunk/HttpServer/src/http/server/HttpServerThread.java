package http.server;

import static util.BasicString.join;
import static util.BasicString.split;
import static util.BasicString.stringToMap;
import http.common.BadHeaderException;
import http.common.HttpRequest;
import http.common.HttpRequestHeader;
import http.common.HttpResponse;
import http.common.HttpResponseHeader;
import http.server.event.RequestEvent;
import http.server.event.RequestEventProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.DateUtil;

/**
 * @author Christian
 *
 */
public class HttpServerThread implements Runnable
{
	// Socket de la connexion liée au thread
	private Socket socket = null;
	
	// Chemin absolu du dossier où se trouvent les fichiers nécessaires au serveur
	private String serverPath;
	
	// Chemin relatif du dossier où se trouvent les fichiers du site Web à servir
	private String siteFolder;
	
	private Map<String, String> mimeTypes;
	private RequestEventProcessor evtProcessor;
	private HttpRequest request;
	private HttpResponse response;
	
	private static final String FILE_SEP = System.getProperties().getProperty("file.separator");

	/**
	 * @param client
	 * @param serverPath
	 * @param siteFolder
	 * @param mimeTypes
	 * @param ep
	 */
	public HttpServerThread(Socket client, String serverPath, String siteFolder, Map<String, String> mimeTypes, RequestEventProcessor ep)
	{
		this.socket = client;
		
		File f = new File(serverPath);
		this.serverPath = f.isDirectory() ? f.getAbsolutePath() : ".";
		this.serverPath += FILE_SEP;
		
		f = new File(this.serverPath + siteFolder);
		this.siteFolder = f.isDirectory() ? siteFolder : "www";
		
		this.mimeTypes = mimeTypes;

		this.evtProcessor = ep;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			// Crée une requête d'entrée
			this.request = new HttpRequest();
			
			// Attend de recevoir un header pour la requête
			HttpRequestHeader requestHeader = this.request.getHeader();
			requestHeader.receive(this.socket.getInputStream());
			
			this.response = new HttpResponse();
			HttpResponseHeader responseHeader = this.response.getHeader();

			try
			{
				// Tente de parser le header
				requestHeader.parse();
				
				System.out.println(String.format("Thread %d (Requête)", Thread.currentThread().getId()));
				System.out.print(requestHeader.getText());
			
				// Envoie un évènement de requête reçue pour permettre d'effectuer un traitement différent
				RequestEvent evt = new RequestEvent(this);
				this.evtProcessor.requestEventReceived(evt);

				// Si l'évènement n'a pas été annulé 
				if (!evt.cancel) 
				{
					// Si la requête utilise la méthode GET ou HEAD
					if (requestHeader.getMethod().equals("GET") || requestHeader.getMethod().equals("HEAD"))
					{
						if (requestHeader.getMethod().equals("HEAD"))
						{
							this.response.setContentSendable(false);
						}
						
						// La réponse sera «cachable» puisqu'on s'apprête à servir un fichier du système de fichiers 
						this.response.setCacheable(true);
						
						String filePath = this.serverPath + this.siteFolder + requestHeader.getPath();
						
						// Remplace les / par des \ au besoin
						if (FILE_SEP != "/")
						{
							filePath = join(split(filePath, "/"), FILE_SEP);
						}

						System.out.println("Fichier à servir : " + filePath);

						File f = new File(filePath);
						
						// Fichier inexistant ?
						if (!f.exists()) 
						{
							responseHeader.setStatusCode(404); // Not Found
						}
						// Répertoire ? Permission lecture manquante ?
						else if (f.isDirectory() || !f.canRead()) 
						{
							responseHeader.setStatusCode(403); // Forbidden
						}
						else
						{
							this.response.setFileName(f.getAbsolutePath());

							// Si date demandée n'est pas antérieure à date du fichier
							if (requestHeader.getField("If-Modified-Since") != null 
									&& !DateUtil.parseDate(requestHeader.getField("If-Modified-Since")).before(
											DateUtil.parseDate(DateUtil.formatDate(new Date(f.lastModified())))))
							{
								responseHeader.setStatusCode(304); // Not Modified
							}
							else
							{
								responseHeader.setStatusCode(200); // OK

								Pattern pFileExtension = Pattern.compile("\\.([^\\.]+)\\Z");
								Matcher mFileExtension = pFileExtension.matcher(this.response.getFileName());

								if (mFileExtension.find()) 
								{
									String fileMimeType = this.mimeTypes.get(mFileExtension.group(1));
									responseHeader.setField("Content-Type", fileMimeType);
								}

								responseHeader.setField("Cache-Control", "public");
								responseHeader.setField("Last-Modified", DateUtil.formatDate(new Date(f.lastModified())));
								responseHeader.setField("Content-Length", f.length() + "");
							}
						}
					}
					else // POST, PUT, DELETE, TRACE, OPTIONS, CONNECT, PATCH
					{
						responseHeader.setStatusCode(501); // Not Implemented
					}
				}
			}
			catch (BadHeaderException e)
			{
				responseHeader.setStatusCode(400); // Bad Request
			}
			
			// Si le socket est encore ouvert
			if (!this.socket.isClosed())
			{
				int statusCode = responseHeader.getStatusCode();

				// S'il s'agit d'un code d'erreur
				if (statusCode >= 400)
				{
					File fError = new File(this.serverPath + "error_" + statusCode + ".htm");

					// Si un fichier de réponse existe pour ce type d'erreur  
					if (fError.exists())
					{
						this.response.setFileName(fError.getAbsolutePath());

						responseHeader.setField("Content-Type", "text/html");
						responseHeader.setField("Content-Length", fError.length() + "");
					}
					else if (responseHeader.getField("Content-Length") == null)
					{
						responseHeader.setField("Content-Length", "0");
					}
				}

				try
				{
					System.out.println(String.format("Thread %d (Réponse)", Thread.currentThread().getId()));
					responseHeader.make();
					
					System.out.print(responseHeader.getText());

					// Envoie la réponse
					this.response.send(this.socket.getOutputStream());
				}
				catch (BadHeaderException e1)
				{
					try
					{
						responseHeader.setStatusCode(500); // Internal Server Error
						
						response.setContent(e1.getMessage().getBytes());
						
						responseHeader.make();
						
						System.out.print(responseHeader.getText());

						// Envoie la réponse
						this.response.send(this.socket.getOutputStream());
					}
					catch (BadHeaderException e2)
					{
						this.socket.getOutputStream().write(new String(
								"HTTP/1.1 500 Internal Server Error\r\n" +
								"Content-Length: 0\r\n\r\n").getBytes());
					}
				}
				finally
				{
					this.socket.close();
				}
			}
		}
		catch (IOException e)
		{
			System.err.println("Erreur d'E/S : " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Retourne l'objet request de cette instance du serveur.
	 * 
	 * @return objet request de cette instance du serveur
	 */
	public HttpRequest getRequest()
	{
		return this.request;
	}

	/**
	 * Retourne l'objet response de cette instance du serveur.
	 * 
	 * @return objet response de cette instance du serveur
	 */
	public HttpResponse getResponse()
	{
		return this.response;
	}

	/**
	 * Retourne le socket associé à cette instance de serveur.
	 * 
	 * @return socket associé à cette instance de serveur
	 */
	public Socket getSocket()
	{
		return this.socket;
	}
}
