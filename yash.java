
public class yash {
    public static void main(String[] args) {
		long start = System.nanoTime();
		for(int i = 0; i < 1000000; i++) {
			// do nothing, just to consume some time
			Test.main(args);

		}
		long end = System.nanoTime();
		long duration = end - start;
		long avg = duration / 1000000;
		System.out.println("Average execution time per iteration: " + avg + " nanoseconds");
	}
}