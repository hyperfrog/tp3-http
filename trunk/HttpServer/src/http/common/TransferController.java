package http.common;

/**
 * Un TransferController est simplement une structure contenant les propri�t�s d'un transfert.
 * 
 * @author Christian Lesage
 * @author Alexandre Tremblay
 *
 */
public class TransferController
{
	/**
	 * D�bit max. du transfert en Ko/s
	 */
	private int maxRate;
	
	/**
	 * Indique si le transfert doit s'arr�ter
	 */
	public boolean stopped;
	
	/**
	 * Construit un TransferController
	 * 
	 * @param maxRate D�bit max. du transfert en Ko/s
	 */
	public TransferController(int maxRate)
	{
		this.setMaxRate(maxRate);
		this.stopped = false;
	}

	/**
	 * Retourne le d�bit maximum du transfert.
	 * 
	 * @return d�bit maximum du transfert en Ko/s; 0 signifie que le d�bit n'est pas limit�.
	 */
	public int getMaxRate() 
	{
		return this.maxRate;
	}

	/**
	 * Programme le d�bit maximum du transfert.
	 * 
	 * @param maxRate d�bit maximum du transfert en Ko/s; 0 signifie que le d�bit n'est pas limit�. 
	 */
	public void setMaxRate(int maxRate) 
	{
		this.maxRate = Math.max(0, maxRate);
	}
}
