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
	 * @param stopped Indique si le transfert doit s'arrêter (mettre à false à la création)
	 */
	public TransferController(int maxRate, boolean stopped)
	{
		this.setMaxRate(maxRate);
		this.stopped = stopped;
	}

	/**
	 * Retourne le débit maximum du transfert.
	 * 
	 * @return débit maximum du transfert en Ko/s; 0 signifie que le débit n'est pas limité.
	 */
	public int getMaxRate() 
	{
		return maxRate;
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
