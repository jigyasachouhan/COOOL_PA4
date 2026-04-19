// In case of big nested function calls, the big function is static-ized instead of being inlined
// Any opportunity of monomorphization inside the big function (wrt interprocedural information) is lost because of 
// imprecision of our analysis
public class Test {
	public static void main(String[] args) {
        Fruit mango = new Fruit();
		Animal examp = new Cat();
        for(int i=0; i<100; i++){
            System.out.println(mango.call(examp));
        }
	}
    
}

class Fruit{
    public int foo(Animal a){
        return a.fib(5);
    }
    public int call(Animal a){
        int p = a.fib(8);
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

    public int fib(int n){
        return n;
    }
}

