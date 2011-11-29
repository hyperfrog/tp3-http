package http.server.event;

import java.util.EventObject;

public class RequestEvent extends EventObject
{
	public boolean cancel = false;
	
	public RequestEvent(Object source)
	{
		super(source);
	}
}
