package http.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class HttpResponse
{
	private static final int KB_PER_SECOND = 50; 

	private HttpResponseHeader header;
	private byte[] content;
	private String fileName;
	private boolean isCacheable;
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
	 * Envoie la réponse HTTP dans la stream de sortie passée en paramètre. 
	 * 
	 * @param os stream de sortie
	 * @return vrai si la réponse a été envoyée avec succès, faux sinon
	 * @throws IOException
	 */
	public boolean send(OutputStream os) throws IOException
	{
		if (!this.header.send(os))
		{
			return false;
		}
		
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

				while ((len = fis.read(buf)) > 0)
				{
					os.write(buf, 0, len);
					try
					{
						Thread.sleep(1000/KB_PER_SECOND);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
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
		return true;
	}
	
	/**
	 * 
	 * @param os
	 * @return
	 * @throws IOException
	 */
	public boolean receiveContent(InputStream is) throws IOException
	{
		if (this.header.getField("Content-Length") != null && !this.header.getField("Content-Length").equals("0"))
		{
			File outputFile = new File(this.fileName + ".tmp");
			
			FileOutputStream fos = new FileOutputStream(outputFile);
			
			byte[] buf = new byte[1024];
			int len;
			
			while ((len = is.read(buf)) > 0)
			{
				fos.write(buf, 0, len);
				
				try
				{
					Thread.sleep(1000/KB_PER_SECOND);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			fos.close();
			
			outputFile.renameTo(new File(this.fileName));
		}
		
		return true;
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
		this.content = content;
		this.header.setField("Content-Length", this.content.length + "");
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(String content, Charset cs)
	{
		this.setContent(content.getBytes(cs));
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
