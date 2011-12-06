package http.common;

import java.io.IOException;
import java.io.OutputStream;

public class HttpRequest
{
	private HttpRequestHeader header;
	private byte[] content; // Pas utilis�; servirait si POST ou PUT �taient impl�ment�s

	/**
	 * 
	 */
	public HttpRequest()
	{
		this.header = new HttpRequestHeader();
	}
	
	/**
	 * @param os
	 * @return
	 * @throws IOException
	 */
	public boolean send(OutputStream os) throws IOException
	{
		// Envoie le header
		if (!this.header.send(os))
		{
			return false;
		}
		
		// Rien d'autres � envoyer tant que POST et PUT ne sont pas impl�ment�s
		
		return true;
	}
	
	/**
	 * @return
	 */
	public HttpRequestHeader getHeader()
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
}
