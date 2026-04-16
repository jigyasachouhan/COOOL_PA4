public class Test {
	public static void main(String[] args) {
		run();
	}
	public static void run() {
		Animal b = new Animal();
		b.fib(5);
	}
}


class Animal {
	int fib(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib2(n-1) + fib4(n-2);
	}
	int fib2(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib3(n-1) + fib3(n-2);
	}
	int fib3(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib(n-1) + fib(n-2);
	}
	int fib4(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib(n-1) + fib(n-2);
	}
}

class Cat extends Animal
{
	
}

