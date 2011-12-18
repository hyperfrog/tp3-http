package http.server;

import static util.BasicString.join;
import static util.BasicString.split;
import static util.BasicString.stringToMap;
import http.common.BadHeaderException;
import http.common.HttpRequest;
import http.common.HttpRequestHeader;
import http.common.HttpResponse;
import http.common.HttpResponseHeader;
import http.common.HttpResponse.TransferController;
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
 * Cette classe mod�lise un serveur HTTP s'ex�cutant dans un thread.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class HttpServerThread implements Runnable
{
	// Socket de la connexion li�e au thread
	private Socket socket = null;
	
	// Chemin absolu du dossier o� se trouvent les fichiers n�cessaires au serveur
	private String serverPath;
	
	// Chemin relatif du dossier o� se trouvent les fichiers du site Web � servir
	private String siteFolder;
	
	// Dictionnaire des extensions et des types MIME correspondants
	private Map<String, String> mimeTypes;
	
	// Objet traitant les �v�nements de requ�te re�ue
	private RequestEventProcessor evtProcessor;
	
	// Requ�te d'entr�e
	private HttpRequest request;
	
	// R�ponse de sortie
	private HttpResponse response;
	
	// Objet servant � contr�ler le t�l�chargement
	private TransferController tc;
	
	// S�parateur de noms de dossier propre � la plateforme
	private static final String FILE_SEP = System.getProperties().getProperty("file.separator");

	/**
	 * Construit une instance de serveur HTTP s'ex�cutant dans un thread.  
	 * 
	 * @param client socket ouvert pour la communication avec le client
	 * @param serverPath chemin absolu du dossier o� se trouvent les fichiers n�cessaires au serveur
	 * @param siteFolder chemin relatif du dossier o� se trouvent les fichiers du site Web � servir
	 * @param mimeTypes dictionnaire des extensions et des types MIME correspondants 
	 * @param ep objet traitant les �v�nements de requ�te re�ue
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
			// Cr�e une requ�te d'entr�e
			this.request = new HttpRequest();
			
			// Attend de recevoir un header pour la requ�te
			HttpRequestHeader requestHeader = this.request.getHeader();
			requestHeader.receive(this.socket.getInputStream());
			
			this.response = new HttpResponse();
			HttpResponseHeader responseHeader = this.response.getHeader();

			this.tc = this.response.new TransferController(0, false);

			try
			{
				// Tente de parser le header
				requestHeader.parse();
				
				System.out.println(String.format("Thread %d (Requ�te)", Thread.currentThread().getId()));
				System.out.print(requestHeader.getText());
			
				// Envoie un �v�nement de requ�te re�ue pour permettre d'effectuer un traitement diff�rent
				RequestEvent evt = new RequestEvent(this);
				this.evtProcessor.requestEventReceived(evt);

				// Si l'�v�nement n'a pas �t� annul� 
				if (!evt.cancel) 
				{
					// Si la requ�te utilise la m�thode GET ou HEAD
					if (requestHeader.getMethod().equals("GET") || requestHeader.getMethod().equals("HEAD"))
					{
						if (requestHeader.getMethod().equals("HEAD"))
						{
							this.response.setContentSendable(false);
						}
						
						// La r�ponse sera �cachable� puisqu'on s'appr�te � servir un fichier du syst�me de fichiers 
						responseHeader.setCacheable(true);
						
						String filePath = this.serverPath + this.siteFolder + requestHeader.getPath();
						
						// Remplace les / par autre chose au besoin
						if (FILE_SEP != "/")
						{
							filePath = join(split(filePath, "/"), FILE_SEP);
						}

						System.out.println("Fichier � servir : " + filePath);

						File f = new File(filePath);
						
						// Fichier inexistant ?
						if (!f.exists()) 
						{
							responseHeader.setStatusCode(404); // Not Found
						}
						// R�pertoire ? Permission lecture manquante ?
						else if (f.isDirectory() || !f.canRead()) 
						{
							responseHeader.setStatusCode(403); // Forbidden
						}
						else
						{
							this.response.setFileName(f.getAbsolutePath());

							// Si date demand�e n'est pas ant�rieure � date du fichier
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

					// Si un fichier de r�ponse existe pour ce type d'erreur  
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
					System.out.println(String.format("Thread %d (R�ponse)", Thread.currentThread().getId()));
					responseHeader.make();
					
					System.out.print(responseHeader.getText());

					// Envoie la r�ponse
					this.response.send(this.socket.getOutputStream(), this.tc);
				}
				catch (BadHeaderException e1) // Pas capable de cr�er le header...
				{
					try // Essaie de faire un header avec le code 500 
					{
						responseHeader.setStatusCode(500); // Internal Server Error
						
						response.setContent(e1.getMessage().getBytes());
						
						responseHeader.make();
						
						System.out.print(responseHeader.getText());

						// Envoie la r�ponse
						this.response.send(this.socket.getOutputStream(), this.tc);
					}
					catch (BadHeaderException e2) // Pas capable..
					{
						// �crit directement sur le socket.
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
	 * Retourne le socket associ� � cette instance de serveur.
	 * 
	 * @return socket associ� � cette instance de serveur
	 */
	public Socket getSocket()
	{
		return this.socket;
	}
}
