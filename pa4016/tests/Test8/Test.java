// In case of small nested function calls, here foo is first inlined (because it is small and monomorphizable at line 5)
// Then, in the second iterative analysis, examp (a because foo was inlined) is detected of a single type
// Thus, the examp.fib(5) call is also inlined (fib is small)
public class Test {
	public static void main(String[] args) {
        Fruit mango = new Fruit();
		Animal examp = new Cat();
        for(int i=0; i<100; i++){
            System.out.println(mango.foo(examp));
        }
	}
    
}

class Fruit{
    public int foo(Animal a){
        return a.fib(5);
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
    public int call(int p){
        int ans = 0;
        for(int i = 0; i < 100; i++) {
            if(i % 2 == 0) {
                ans += i;
            }
            else{
                ans -= i;
            }
        }
		int sum = 0;
		while(p>0)
		{
			sum+=p%10;
			p/=10;

		}
		if(sum > 20)
		{
			ans += sum;
		}
		else{
			sum = 0;
			while(p>0)
			{
				sum+=(p%10)*(p%10);
				p/=10;
			}
			ans += sum;
		}
		String s = "Hiiiiiii";
        return ans * 57;
    }
}

class Cat extends Animal
{
    int height;
    Cat(){
        height = 4;
    }

    public int fib(int n){
        return n;
    }
}

