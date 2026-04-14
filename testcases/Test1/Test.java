public class Test {
	public static void main(String[] args) {
		run();
	}
	public static void run() {
		Animal a = new Cat();
		a.call();
		Animal b = new Animal();
		b.call();
		b.fib(5);
		b.sq(5);
	}
}


class Animal {
	void call()
	{
		int x = 1000;
	}
	int sq(int m)
	{
		return m*m;
	}
	int fib(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib2(n-1) + fib2(n-2);
	}
	int fib2(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib(n-1) + fib(n-2);
	}
}

class Cat extends Animal
{
	@Override
	void call()
	{
		int y =1212;
	}
}

