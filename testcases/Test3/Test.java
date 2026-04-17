public class Test {
	public static void main(String[] args) {
		run();
	}
	public static void run() {
		Animal b = new Animal();
		// b.fib(5);
		// b.fib2(5);
		// b.fib3(5);
		// b.fib4(5);

        for(int i=0; i<100; i++){
			b.call(i);
		}

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
	
}

