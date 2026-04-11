public class Test {
	public static void main(String[] args) {
		run();
	}
	public static void run() {
		Animal a = new Cat();
		a.call();
		Animal b = new Animal();
		b.call();
	}
}


class Animal {
	void call()
	{
		int x = 1000;
		// System.out.println(x);
	}
}

class Cat extends Animal
{
	@Override
	void call()
	{
		int y =1212;
		// System.out.println(y);
	}
}

