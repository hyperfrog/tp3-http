/**
 * Une exception de type BadHeaderExcetption est g�n�r�e quand une ent�te HTTP ne peut pas �tre cr��e ou analys�e. 
 */
package http.common;

/**
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class BadHeaderException extends Exception
{

	/**
	 * Cr�e une exception de type BadHeaderException.
	 */
	public BadHeaderException()
	{
		super();
	}

	/**
	 * Cr�e une exception de type BadHeaderException avec un message associ�.
	 * 
	 * @param msg message associ�e � l'exception cr��e
	 */
	public BadHeaderException(String msg)
	{
		super(msg);
	}
}
