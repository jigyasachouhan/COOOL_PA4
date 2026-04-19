// Here fib (recursive function) is inlined until thr threshold is reached and then converted into a static call
public class Test {
	public static void main(String[] args) {
		Animal examp = new Cat();
        System.out.println(examp.fib(5));
	}
}


class Animal {
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
}

class Cat extends Animal
{
    int height;
    Cat(){
        height = 4;
    }
	int fib(int n){
        if(n==0) return 1;
        if(n==1) return 2;
        return fib(n-1) + fib(n-2);
    }
}