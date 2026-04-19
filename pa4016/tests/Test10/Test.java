// Monomorphization is also done for methods in interfaces
public class Test {
	public static void main(String[] args) {
		Cat examp = new Cat();
        examp.move();
	}
    
}

interface Mobile{
    public void move();
}

class Animal implements Mobile{
    int height;
    Animal(){
        height = 10;
    }
	int fib(int n)
	{
		if(n==1 || n ==0)
			return 1;
		return fib(n-1) + fib(n-2);
	}
    public void move(){
        System.out.println("Animal moving");
    }
}

class Cat extends Animal implements Mobile
{
    int height;
    Cat(){
        height = 4;
    }

    public int fib(int n){
        return n;
    }

    public void move(){
        System.out.println("Catwalking");
    }
}

