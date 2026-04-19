
public class yash {
    public static void main(String[] args) {
		long start = System.nanoTime();
		for(int i = 0; i < 10000; i++) {
			// do nothing, just to consume some time
			Test.main(args);

		}
		long end = System.nanoTime();
		long duration = end - start;
		long avg = duration / 10000;
		System.err.println("Average execution time per iteration: " + avg + " nanoseconds");
	}
}