// Big methods are replaced with static versions if target is singleton
public class Test {
	public static void main(String[] args) {
		run();
	}
	public static void run() {
		Animal b = new Animal();
		for(int i=0; i<100; i++){
			int res = b.call(i);
			System.out.println(res);
		}
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
		while(p>0){
			sum+=p%10;
			p/=10;
		}
		if(sum > 20) ans += sum;
		else{
			sum = 0;
			while(p>0){
				sum+=(p%10)*(p%10);
				p/=10;
			}
			ans += sum;
		}
        return ans * 57;
    }

}


