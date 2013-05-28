package camml.core;

public class Has64 {
	public static void main(String[] args) {
		if (System.getProperty("sun.arch.data.model").equals("64")) {
			System.exit(0);
		}
		else {
			System.exit(1);
		}
	}
}