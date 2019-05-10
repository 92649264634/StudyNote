package com.random.note.MulitThread;

public class VoliteTest {

	public int a = 1;

	public boolean states = false;

	// 假设该方法在线程A中执行
	public void stateChange() {
		a = 2; // 1
		states = true; // 2
	}

	// 假设该方法在线程B中执行
	public void run() {
		if (states) {
			int b = a + 2;  // 3
			System.out.println(b); // 4
		}
	}

}
