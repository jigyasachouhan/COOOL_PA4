public class Test {
	void bar(){
		int y = 2000;
		// System.out.println(y);
	}
	static void bar2(){
		int z = 3000;
		// System.out.println(z);
	}

	public static void main(String[] args) {
		run();
	}
	public static void run() {

		Node a = new Node(); // O10
		a.foo();
		a.foo();
		a.foo();
		a.foo();
		a.foo();

		bar2();
		Node.foo2(a);
	}
}


class Node {
	Node f1;
	Node f2;
	Node g;
	Node() {}
	void foo() {
		int x = 1000;
		// System.out.println(x);
	}
	static void foo2(Node a) {
		int x = 1000;
		// System.out.println(x);
	}
}

