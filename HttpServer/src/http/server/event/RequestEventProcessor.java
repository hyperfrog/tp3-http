package http.server.event;

public interface RequestEventProcessor
{
	/**
	 * Méthode appelée quand le serveur reçoit une requête pour permettre un traitement différent 
	 * du traitement par défaut.
	 * 
	 * @param e évènement de la requête
	 */
	public void requestEventReceived(RequestEvent e);
}
