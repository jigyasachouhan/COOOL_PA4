// Short functions are inlined in the caller if the target is singleton
public class Test {
	public static void main(String[] args) {
		run();
	}
	public static void run() {
		Animal a = new Cat();
		a.call();
		Animal b = new Animal();
		b.call();
		for(int i=0; i<100; i++){
			int result = b.sq(i);
			System.out.println(result);
		}
	}
}


class Animal {
	void call()
	{
		int x = 1000;
		System.out.println(x);;
	}
	int sq(int m)
	{
		return m*m;
	}
}

class Cat extends Animal
{
	@Override
	void call()
	{
		int x =1212;
		int y = x + 1000;
	}
}

