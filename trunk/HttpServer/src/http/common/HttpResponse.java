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
 * La classe HttpResponse mod�lise une r�ponse HTTP. 
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class HttpResponse
{
	// Ent�te de la r�ponse
	private HttpResponseHeader header;
	
	// Contenu associ� � la r�ponse
	private byte[] content;
	
	// Chemin du fichier � envoyer ou recevoir
	private String fileName;
	
	// Indique si la r�ponse peut �tre mise en cache par le client ou les proxys
	private boolean isCacheable;
	
	// Indique si le contenu doit �tre envoy�
	private boolean isContentSendable;
	
	/**
	 * Construit une r�ponse HTTP.
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
	 * Envoie la r�ponse HTTP sur la stream de sortie pass�e en param�tre. 
	 * 
	 * @param os OutputStream pour l'�criture de la r�ponse 
	 * @param tc Contr�leur du transfert
	 * @return vrai si le t�l�versement a �t� compl�t�, faux sinon 
	 * @throws IOException
	 */
	public boolean send(OutputStream os, TransferController tc) throws IOException, BadHeaderException
	{
		boolean success = false;
		
		// Envoie le header
		this.header.send(os);
		
		// Envoie le contenu s'il y en a un et s'il doit �tre envoy� 
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
	 * Re�oit la partie contenu d'une r�ponse HTTP.
	 * 
	 * @param is InputStream pour la lecture du contenu de la r�ponse
	 * @param tc Contr�leur du transfert  
	 * @return vrai si le t�l�chargement a �t� compl�t�, faux sinon
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
	
	// Effectue la copie d'une stream d'entr�e vers une stream de sortie.
	// Le contr�leur permet d'arr�ter le transfert � tout moment ou de changer son d�bit max. 
	private boolean doCopy(InputStream is, OutputStream os, TransferController tc) throws IOException
	{
		// Transf�re 1 Ko � la fois
		byte[] buf = new byte[1024];
		int len;
		
		// Continue � copier tant qu'il y a quelque chose � lire et que le transfert doit se poursuivre
		for (long startTime = System.currentTimeMillis(); (len = is.read(buf)) > 0 && !tc.stopped; startTime = System.currentTimeMillis())
		{
			os.write(buf, 0, len);
			
			if (tc.getMaxRate() > 0)
			{
				try
				{
					// Attend le temps n�cessaire pour ne pas d�passer le d�bit max.
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
	 * Retourne l'ent�te de la r�ponse.
	 * 
	 * @return l'ent�te de la r�ponse
	 */
	public HttpResponseHeader getHeader()
	{
		return this.header;
	}
	
	/**
	 * Retourne le contenu associ� � cette r�ponse.
	 * 
	 * @return le contenu associ� � cette r�ponse
	 */
	public byte[] getContent()
	{
		return this.content;
	}

	/**
	 * Programme le contenu associ� � la r�ponse.
	 * 
	 * @param content contenu � associer � la r�ponse
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
	 * Programme le contenu associ� � la r�ponse.
	 * 
	 * @param content contenu � associer � la r�ponse
	 * @param cs encodage des caract�res  
	 */
	public void setContent(String content, Charset cs)
	{
		if (content != null)
		{
			this.setContent(content.getBytes(cs));
		}
	}

	/**
	 * Retourne le chemin le du fichier � envoyer ou recevoir
	 * 
	 * @return chemin le du fichier � envoyer ou recevoir
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * Programme le chemin le du fichier � envoyer ou recevoir
	 * 
	 * @param fileName chemin le du fichier � envoyer ou recevoir
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Indique si la partie contenu doit �tre envoy�e avec la r�ponse. 
	 * 
	 * @return vrai si le contenu doit �tre envoy�, faux sion 
	 */
	public boolean isContentSendable()
	{
		return isContentSendable;
	}

	/**
	 *  D�termine si la partie contenu doit �tre envoy�e avec la r�ponse.
	 * 
	 * @param isContentSendable vrai pour que le contenu soit envoy� avec la r�ponse, faux pour qu'il ne le soit pas
	 */
	public void setContentSendable(boolean isContentSendable)
	{
		this.isContentSendable = isContentSendable;
	}
}
