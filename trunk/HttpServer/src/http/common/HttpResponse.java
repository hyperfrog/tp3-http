package http.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * La classe HttpResponse modélise une réponse HTTP. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class HttpResponse
{
	// Entête de la réponse
	private HttpResponseHeader header;
	
	// Contenu associé à la réponse
	private byte[] content;
	
	// Chemin du fichier à envoyer ou recevoir
	private String fileName;
	
	// Indique si la réponse peut être mise en cache par le client ou les proxys
	private boolean isCacheable;
	
	// Indique si le contenu doit être envoyé
	private boolean isContentSendable;
	
	/**
	 * Construit une réponse HTTP.
	 */
	public HttpResponse()
	{
		this.header = new HttpResponseHeader();
		this.content = null;
		this.fileName = "";
		this.header.setProtocol("HTTP/1.1");
		this.header.setStatusCode(0);
		this.header.setField("Server", "CLES 0.1");
		this.header.setField("Connection", "close");
		this.isCacheable = true;
		this.isContentSendable = true;
	}
	
	/**
	 * Envoie la réponse HTTP sur la stream de sortie passée en paramètre. 
	 * 
	 * @param os OutputStream pour l'écriture de la réponse 
	 * @param tc Contrôleur du transfert
	 * @return vrai si le téléversement a été complété, faux sinon 
	 * @throws IOException
	 */
	public boolean send(OutputStream os, TransferController tc) throws IOException, BadHeaderException
	{
		boolean success = false;
		
		// Envoie le header
		this.header.send(os);
		
		// Envoie le contenu s'il y en a un et s'il doit être envoyé 
		if (this.isContentSendable 
				&& this.header.getField("Content-Length") != null 
				&& !this.header.getField("Content-Length").equals("0"))
		{
			InputStream is = (this.content == null) ? new FileInputStream(new File(this.fileName)) : new ByteArrayInputStream(this.content);
			
			success = this.doCopy(is, os, tc);

			is.close();
			os.flush();
		}
		
		return success;
	}
	
	/**
	 * Reçoit la partie contenu d'une réponse HTTP.
	 * 
	 * @param is InputStream pour la lecture du contenu de la réponse
	 * @param tc Contrôleur du transfert  
	 * @return vrai si le téléchargement a été complété, faux sinon
	 * @throws IOException
	 */
	public boolean receiveContent(InputStream is, TransferController tc) throws IOException
	{
		if (this.header.getField("Content-Length") != null && !this.header.getField("Content-Length").equals("0"))
		{
			long length = 0;
			
			try
			{
				length = Long.parseLong(this.header.getField("Content-Length"));
				
				File outputFile = new File(this.fileName + ".tmp");
				
				OutputStream os = new FileOutputStream(outputFile);
				
				this.doCopy(is, os, tc);
				
				os.close();
				
				if (outputFile.length() == length)
				{
					outputFile.renameTo(new File(this.fileName));
					return true;
				}
			}
			catch (NumberFormatException e)
			{
			}
		}
		
		return false;
	}
	
	// Effectue la copie d'une stream d'entrée vers une stream de sortie.
	// Le contrôleur permet d'arrêter le transfert à tout moment ou de changer son débit max. 
	private boolean doCopy(InputStream is, OutputStream os, TransferController tc) throws IOException
	{
		// Transfère 1 Ko à la fois
		byte[] buf = new byte[1024];
		int len;
		
		// Continue à copier tant qu'il y a quelque chose à lire et que le transfert doit se poursuivre
		for (long startTime = System.currentTimeMillis(); (len = is.read(buf)) > 0 && !tc.stopped; startTime = System.currentTimeMillis())
		{
			os.write(buf, 0, len);
			
			if (tc.getMaxRate() > 0)
			{
				try
				{
					// Attend le temps nécessaire pour ne pas dépasser le débit max.
					long delay = (1000 - (System.currentTimeMillis() - startTime)) / tc.getMaxRate();

					Thread.sleep(delay > 0 ? delay : 0);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		
		return len == -1;
	}
	
	/**
	 * Retourne l'entête de la réponse.
	 * 
	 * @return l'entête de la réponse
	 */
	public HttpResponseHeader getHeader()
	{
		return this.header;
	}
	
	/**
	 * Retourne le contenu associé à cette réponse.
	 * 
	 * @return le contenu associé à cette réponse
	 */
	public byte[] getContent()
	{
		return this.content;
	}

	/**
	 * Programme le contenu associé à la réponse.
	 * 
	 * @param content contenu à associer à la réponse
	 */
	public void setContent(byte[] content)
	{
		if (content != null)
		{
			this.content = content;
			this.header.setField("Content-Length", this.content.length + "");
		}
	}

	/**
	 * Programme le contenu associé à la réponse.
	 * 
	 * @param content contenu à associer à la réponse
	 * @param cs encodage des caractères  
	 */
	public void setContent(String content, Charset cs)
	{
		if (content != null)
		{
			this.setContent(content.getBytes(cs));
		}
	}

	/**
	 * Retourne le chemin le du fichier à envoyer ou recevoir
	 * 
	 * @return chemin le du fichier à envoyer ou recevoir
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * Programme le chemin le du fichier à envoyer ou recevoir
	 * 
	 * @param fileName chemin le du fichier à envoyer ou recevoir
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Indique si la partie contenu doit être envoyée avec la réponse. 
	 * 
	 * @return vrai si le contenu doit être envoyé, faux sion 
	 */
	public boolean isContentSendable()
	{
		return isContentSendable;
	}

	/**
	 *  Détermine si la partie contenu doit être envoyée avec la réponse.
	 * 
	 * @param isContentSendable vrai pour que le contenu soit envoyé avec la réponse, faux pour qu'il ne le soit pas
	 */
	public void setContentSendable(boolean isContentSendable)
	{
		this.isContentSendable = isContentSendable;
	}
}
