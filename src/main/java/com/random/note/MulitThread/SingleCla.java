package com.random.note.MulitThread;

public class SingleCla {

	private static volatile SingleCla instance;

	private SingleCla() {}

	public static SingleCla getInstance() {
		if (instance == null) {
			synchronized (SingleCla.class) {
				if (instance == null) {
					instance = new SingleCla();
				}
			}
		}
		return instance;
	}

}
