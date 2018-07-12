package com.htt.app.cache.exception;
 
public class LockException extends RuntimeException {
	//~ Static fields/initializers -----------------------------------------------------------------

    private static final long serialVersionUID = -6751840717046373725L;

    protected String message;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ServiceException object.
     *
     * @param message
     */
    public LockException(String message) {
    	this.message = message;
    }

    /**
     * Creates a new ServiceException object.
     *
     * @param message
     * @param cause
     */
    public LockException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

	public LockException(Exception re) {
		super(re);
	}

	@Override
	public String toString() {
		return this.message;
	}

	/**
	 * @see Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return this.message;
	}
    
}
