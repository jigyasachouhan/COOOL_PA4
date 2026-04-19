// In a method, if argument is of a type exhibiting polymorphism (has child classes), points-to analysis considers the case 
// where the object passed is of child class instead of given argument type
public class Test {
	public static void main(String[] args) {
        Animal examp = new Cat();
		run(examp);
	}
	public static void run(Animal a) {
		System.out.println(a.call(5));
	}
}


class Animal {
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
	public int call(int p){
        return p;
    }
}

