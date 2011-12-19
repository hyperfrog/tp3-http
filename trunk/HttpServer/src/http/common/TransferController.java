package http.common;

/**
 * Un TransferController est simplement une structure contenant les propriétés d'un transfert.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class TransferController
{
	/**
	 * Débit max. du transfert en Ko/s
	 */
	private int maxRate;
	
	/**
	 * Indique si le transfert doit s'arrêter
	 */
	public boolean stopped;
	
	/**
	 * Construit un TransferController
	 * 
	 * @param maxRate Débit max. du transfert en Ko/s
	 */
	public TransferController(int maxRate)
	{
		this.setMaxRate(maxRate);
		this.stopped = false;
	}

	/**
	 * Retourne le débit maximum du transfert.
	 * 
	 * @return débit maximum du transfert en Ko/s; 0 signifie que le débit n'est pas limité.
	 */
	public int getMaxRate() 
	{
		return this.maxRate;
	}

	/**
	 * Programme le débit maximum du transfert.
	 * 
	 * @param maxRate débit maximum du transfert en Ko/s; 0 signifie que le débit n'est pas limité. 
	 */
	public void setMaxRate(int maxRate) 
	{
		this.maxRate = Math.max(0, maxRate);
	}
}
