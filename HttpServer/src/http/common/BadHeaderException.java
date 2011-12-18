/**
 * Une exception de type BadHeaderExcetption est générée quand une entête HTTP ne peut pas être créée ou analysée. 
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
	 * Crée une exception de type BadHeaderException.
	 */
	public BadHeaderException()
	{
		super();
	}

	/**
	 * Crée une exception de type BadHeaderException avec un message associé.
	 * 
	 * @param msg message associée à l'exception créée
	 */
	public BadHeaderException(String msg)
	{
		super(msg);
	}
}
