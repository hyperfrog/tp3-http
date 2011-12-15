package http.server.event;

public interface RequestEventProcessor
{
	/**
	 * M�thode appel�e quand le serveur re�oit une requ�te pour permettre un traitement diff�rent 
	 * du traitement par d�faut.
	 * 
	 * @param e �v�nement de la requ�te
	 */
	public void requestEventReceived(RequestEvent e);
}
