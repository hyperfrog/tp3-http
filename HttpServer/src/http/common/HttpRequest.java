package http.common;

import java.io.IOException;
import java.io.OutputStream;

public class HttpRequest
{
	private HttpRequestHeader header;
	private byte[] content; // Pas utilisé; servirait si POST ou PUT étaient implémentés

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
		
		// Rien d'autres à envoyer tant que POST et PUT ne sont pas implémentés
		
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
