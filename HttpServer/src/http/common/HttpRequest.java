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
	 * @throws IOException
	 */
	public void send(OutputStream os) throws IOException, BadHeaderException
	{
		// Envoie le header
		this.header.send(os);
		
		// Rien d'autre � envoyer tant que POST et PUT ne sont pas impl�ment�s
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
