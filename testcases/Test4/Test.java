public class Test {
	public static void main(String[] args) {
		Animal examp = new Cat();
        System.out.println(examp.fib(5));

        if(examp.height < 6){
            examp = new Animal();
        }
        else{
            examp = new Cat();
        }
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
	int fib(int n){
        if(n==0) return 1;
        if(n==1) return 2;
        return fib(n-1) + fib(n-2);
    }
}

