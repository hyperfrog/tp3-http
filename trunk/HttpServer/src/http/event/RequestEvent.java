package http.event;

import java.util.EventObject;
import http.HttpServer;

public class RequestEvent extends EventObject
{
	private final Thread thread;
	public boolean cancel = false;
	
	public RequestEvent(Object source, Thread t)
	{
		super(source);
		this.thread = t;
	}
	
	public Thread getThread()
	{
		return this.thread;
	}
}
