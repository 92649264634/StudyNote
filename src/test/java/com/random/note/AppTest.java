package com.random.note;

public class AppTest {

	public synchronized void test() {
		System.out.println("test");
	}
	
	public void test1() {
		synchronized (AppTest.class) {
			System.out.println("test1");
		}
	}
}
