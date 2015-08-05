/**
 * User: Cying
 * Date: 2015/8/5
 * Time: 17:58
 */
public class TT {
	static class A{
		int num;
		B b;
	}
	static class B{
		int num;
		A a;
	}
	public static void main(String args[]){
		A a=new A();
		B b=new B();
		a.b=b;
		b.a=a;
		a.num=8;
		System.out.print(b.a.num);
	}
}
