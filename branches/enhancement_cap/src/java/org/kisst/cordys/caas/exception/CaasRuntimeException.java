package org.kisst.cordys.caas.exception;

/**
 *This class used to throw the runtime exceptions that may occur while executing the Caas framework code
 *
 *@author galoori 
 */
public class CaasRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public CaasRuntimeException(String message) 
	{
		super(message);
	}
	public CaasRuntimeException(Exception e) 
	{
		super(e);
	}
	public CaasRuntimeException(Throwable exception) 
	{
		super(exception);
	}
}
