package http.common;

import http.common.HttpResponse.TransferController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class HttpResponse
{
	public class TransferController
	{
		public int maxRate;
		public boolean stopped;
		
		public TransferController(int maxRate, boolean stopped)
		{
			this.maxRate = maxRate;
			this.stopped = stopped;
		}
	}
	
	private HttpResponseHeader header;
	private byte[] content;
	private String fileName;
	private boolean isCacheable;
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
	 * @param tc vitesse max. du t�l�versement en ko/s
	 * @throws IOException
	 */
	public void send(OutputStream os, TransferController tc) throws IOException, BadHeaderException
	{
		// Envoie le header
		this.header.send(os);
		
		if (this.isContentSendable 
				&& this.header.getField("Content-Length") != null 
				&& !this.header.getField("Content-Length").equals("0"))
		{
			if (this.content == null )
			{
				// Open the file   
				FileInputStream fis = new FileInputStream(new File(this.fileName));

				byte[] buf = new byte[1024];
				int len;

				while ((len = fis.read(buf)) > 0 && !tc.stopped)
				{
					os.write(buf, 0, len);
					
					if (tc.maxRate > 0)
					{
						try
						{
							Thread.sleep(1000 / tc.maxRate);
						}
						catch (InterruptedException e)
						{
						}
					}
				}

				fis.close();
				os.flush();
			}
			else
			{
				os.write(this.content);
				os.flush();
			}
		}
	}
	
	/**
	 * Re�oit la partie contenu d'une r�ponse HTTP.
	 * 
	 * @param is InputStream pour la lecture du contenu de la r�ponse
	 * @param tc vitesse max. du t�l�chargement en ko/s 
	 * @return vrai si le t�l�chargement a �t� compl�t�, faux sinon
	 * @throws IOException
	 */
	public boolean receiveContent(InputStream is, TransferController tc) throws IOException
	{
		if (this.header.getField("Content-Length") != null && !this.header.getField("Content-Length").equals("0"))
		{
			File tempName = new File(this.fileName);
			
			File outputFile = File.createTempFile(tempName.getName(), ".tmp");
			
			FileOutputStream fos = new FileOutputStream(outputFile);
			
			byte[] buf = new byte[1024];
			int len;
			
			while ((len = is.read(buf)) > 0 && !tc.stopped)
			{
				fos.write(buf, 0, len);

				if (tc.maxRate > 0)
				{
					try
					{
						Thread.sleep(1000 / tc.maxRate);
					}
					catch (InterruptedException e)
					{
					}
				}
			}
			
			fos.close();
			
			if (outputFile.length() == Integer.parseInt(this.header.getField("Content-Length")))
			{
				outputFile.renameTo(new File(this.fileName));
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return
	 */
	public HttpResponseHeader getHeader()
	{
		return this.header;
	}
	
	/**
	 * @return the content
	 */
	public byte[] getContent()
	{
		return this.content;
	}

	/**
	 * @param content the content to set
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
	 * @param content the content to set
	 */
	public void setContent(String content, Charset cs)
	{
		if (content != null)
		{
			this.setContent(content.getBytes(cs));
		}
	}

	/**
	 * @return the fileName
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * @return the cacheable
	 */
	public boolean isCacheable()
	{
		return isCacheable;
	}

	/**
	 * @param isCacheable the cacheable to set
	 */
	public void setCacheable(boolean isCacheable)
	{
		this.header.setCacheable(isCacheable);
		this.isCacheable = isCacheable;
	}

	/**
	 * @return the sendContent
	 */
	public boolean isContentSendable()
	{
		return isContentSendable;
	}

	/**
	 * @param isContentSendable the sendContent to set
	 */
	public void setContentSendable(boolean isContentSendable)
	{
		this.isContentSendable = isContentSendable;
	}
}
