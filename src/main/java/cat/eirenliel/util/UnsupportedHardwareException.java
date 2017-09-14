package cat.eirenliel.util;

public class UnsupportedHardwareException extends RuntimeException {

	private static final long serialVersionUID = 3416202699301337984L;

	public UnsupportedHardwareException(String cause) {
		super(cause);
	}
	
	public UnsupportedHardwareException(String cause, Throwable t) {
		super(cause, t);
	}
	
	public UnsupportedHardwareException(Throwable t) {
		super(t);
	}
}
