
public class yash {
    public static void main(String[] args) {
		long start = System.nanoTime();
		Node a = new Node();
		for(int i = 0; i < 10000; i++) {
			// do nothing, just to consume some time
			Test.run();

		}
		long end = System.nanoTime();
		long duration = end - start;
		long avg = duration / 10000;
		System.out.println("Average execution time per iteration: " + avg + " nanoseconds");
	}
}