package http.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class HttpResponse
{
	private static final int KB_PER_SECOND = 50; 

	private HttpHeader header;
	private byte[] content;
	private String fileName;
	private boolean isCacheable;
	private boolean contentSendable;
	
	/**
	 * Construit une réponse HTTP.
	 */
	public HttpResponse()
	{
		this.header = new HttpHeader();
		this.content = null;
		this.fileName = "";
		this.header.setProtocol("HTTP/1.1");
		this.header.setStatusCode(0);
		this.header.setField("Server", "CLES 0.1");
		this.header.setField("Connection", "close");
		this.isCacheable = true;
		this.contentSendable = true;
	}

	/**
	 * Fabrique un header HTTP pour la réponse.
	 * 
	 * @return vrai si le header a pu être fabriqué, faux sinon
	 */
	public boolean makeHeader()
	{
		return this.header.makeResponseHeader(this.isCacheable);
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
		if (this.header.getText() == null || this.header.getText().isEmpty())
		{
			if (!this.header.makeResponseHeader(this.isCacheable))
			{
				return false;
			}
		}

		OutputStreamWriter osw = new OutputStreamWriter(os);

		osw.write(this.header.getText());
		osw.flush();

		if (this.contentSendable && this.header.getField("Content-Length") != null && !this.header.getField("Content-Length").equals("0"))
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
	 * @return
	 */
	public HttpHeader getHeader()
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
		this.isCacheable = isCacheable;
	}

	/**
	 * @return the sendContent
	 */
	public boolean isContentSendable()
	{
		return contentSendable;
	}

	/**
	 * @param contentSendable the sendContent to set
	 */
	public void setContentSendable(boolean contentSendable)
	{
		this.contentSendable = contentSendable;
	}
	
	
}
