package org.kisst.cordys.caas.exception;

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
