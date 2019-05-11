# 多线程

##  1. volatile有什么作用？
volatile是Java提供的一种轻量级的同步机制。在Java中的内存模型中(JMM)，每一个线程都有自己的本地内存区（虚拟机栈，本地方法栈和程序计数器）。类的共享变量存储在主内存中（方法区和堆区）。本地内存保存了当前线程所使用的主内存的副本，线程对变量的所有操作都是保存在本地内存中的，而不能直接读写主内存的变量。
![enter image description here](https://raw.githubusercontent.com/92649264634/ImageAll/master/images/StudyNote/MulitThread/neicunjiaohuan.png)
对一个变量声明为volatile，具有两种特性：

> 特性一：保证共享变量对所有的线程可见性；

a. 当写一个volatile变量时，JMM会把该线程对应的本地内存中的变量强制刷新到主内存中去;
b. 这个写会操作会导致其他线程中的缓存无效;
注意：但是volatile是有局限性的，对于复合操作，给变量声明为volatile并不能解决共享变量在多线程模式下的安全问题。例如 i++操作（分解为读取、加一、赋值），即该操作不是原子性的。

> 特性二：禁止指令重排序优化

重排序是指编译器和处理器为了优化程序性能而对指令序列进行排序的一种手段
示例代码：
```java
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
			int b = a + 2; // 3
			System.out.println(b); // 4
		}
	}
}
```
上面我们提到过，为了提供程序并行度，编译器和处理器可能会对指令进行重排序，而上例中的1和2由于不存在数据依赖关系，则有可能会被重排序，先执行status=true再执行a=2。而此时线程B会顺利到达4处，而线程A中a=2这个操作还未被执行，所以b=a+1的结果也有可能依然等于3；

***问题一：为什么用volatile声明变量后，就可以对线程立即可见？***

> ​	首先，我们应该了解Java内存模型中，线程的工作内存与主内存的数据的交换协议。
> ​	Java内存模型规定了所有的变量都存储在主内存中（虚拟机内存的一部分），每条线程都有自己的工作内存，保存了该线程使用到的变量的主内存副本拷贝，线程对变量的所有操作（读取、赋值等）都必须在工作内存中进行，不能直接读写主内存中的变量。不同线程无发直接访问对方工作内存中的变量，必须通过主内存这个媒介才能实现。
>
> ​	Java内存模型定义了8种操作来完成主内存与工作内存之间的交互。
>
> ​	-- 作用于主内存中的变量：lock（锁定）、unlock（解锁）、read（读）、write（写入）
>
> ​	-- 作用于工作内存中的变量：load（载入）、use（使用）、assign（赋值）、store（存储）
>
> ​	当一个变量用volatile声明后，load、use指令必须连续一起出现（保证在工作内存中，使用变量之前必须先从主内存中刷新最新的值），assign、store指令必须连续一起出现（保证在工作内存中，每次修改变量之后都必须立刻同步到主内存中，以便其他线程可以读取到最新的值）。其中，最为主要的原因是，当一个使用volatile声明的变量在发生赋值操作时，会多执行一个lock操作（汇编指令，不是上文中的lock），它的作用是使得本CPU的Cache写入了内存，该写入动作会引起别的CPU或者别的内核无效化其Cache，所以volatile修饰的变量的修改对其他线程立即可见。

<font color=red>声明：无论是普通变量还是volatile变量，都是基于主内存媒介实现线程间数据的交换，而与volatile变量的区别是，普通变量的修改操作不能立即同步到主内存中（assign与store指令直接可能有其他的操作），并且也没有lock指令来保证其他CPU的缓存有效性</font>

***问题二：标准的单例模式中，变量为什么要用volatile修饰？***

单例模式的代码如下：

```java
public class SingleCla {

	private static volatile SingleCla instance;

	private SingleCla() {}

	public static SingleCla getInstance() {
		if (instance == null) {
			synchronized (SingleCla.class) {
				if (instance == null) {
					instance = new SingleCla();  // 问题的根源
				}
			}
		}
		return instance;
	}

}
```

“instance = new SingleCla();”这行代码可以拆分为以下3行伪代码

> memory = allocate();       // 1：分配对象的内存空间
> ctorInstance(memory);   // 2：初始化对象
> instance = memory;        // 3：设置instance指向刚分配的内存地址 

​	如果instance变量不使用volatile声明，这3行伪代码就会产生指令重排序（可能为1->3->2）。这种情况下，其他线程就有可能获取到一个未初始化的对象，导致调用失败。



# 2. sychronized有什么作用？

syschronized是Java提供一种重量级的同步锁。
syschronized属于可重入锁，即在同一锁程中，线程不需要再次获取同一把锁。
执行同步代码块后首先要先执行monitorenter指令，退出的时候monitorexit指令。通过分析之后可以看出，使用Synchronized进行同步，其关键就是必须要对对象的监视器monitor进行获取，当线程获取monitor后才能继续往下执行，否则就只能等待。而这个获取的过程是互斥的，即同一时刻只有一个线程能够获取到monitor。

- syschronized作用在方法上--通过标识符

  ```java
   public synchronized void test();
      descriptor: ()V
      flags: ACC_PUBLIC, ACC_SYNCHRONIZED
      Code:
        stack=2, locals=1, args_size=1
           0: getstatic     #2                  // Field 
           3: ldc           #3                  // String test
           5: invokevirtual #4                  // Method 
           8: return
        LineNumberTable:
          line 6: 0
          line 7: 8
  ```

  

- syschronized作用在代码块上--通过指令

  ```java
   public void test1();
      descriptor: ()V
      flags: ACC_PUBLIC
      Code:
        stack=2, locals=3, args_size=1
           0: ldc           #5                  // class com/random/note/AppTest
           2: dup
           3: astore_1
           4: monitorenter
           5: getstatic     #2                  // Field 
           8: ldc           #6                  // String test1
          10: invokevirtual #4                  // Method java/io/PrintStream.println:
          13: aload_1
          14: monitorexit
          15: goto          23
          18: astore_2
          19: aload_1
          20: monitorexit
          21: aload_2
          22: athrow
          23: return
        Exception table:
           from    to  target type
               5    15    18   any
              18    21    18   any
  ```

  


当线程进入到synchronized方法或者synchronized代码块时，线程切换到的是BLOCKED状态，而使用java.util.concurrent.locks下lock进行加锁的时候线程切换的是WAITING或者TIMED_WAITING状态，因为lock会调用LockSupport的方法

## 3. synchronized的优化

Synchronized最大的特征就是在同一时刻只有一个线程能够获得对象的监视器（monitor），从而进入到同步代码块或者同步方法之中，即表现为互斥性（排它性）。
优化：加快锁的获取速度
![enter image description here](https://raw.githubusercontent.com/92649264634/ImageAll/master/images/StudyNote/MulitThread/6461646861613.png)
Java SE 1.6中，锁一共有4种状态，级别从低到高依次是：无锁状态、偏向锁状态、轻量级锁状态和重量级锁状态，这几个状态会随着竞争情况逐渐升级。锁可以升级但不能降级，意味着偏向锁升级成轻量级锁后不能降级成偏向锁。这种锁升级却不能降级的策略，目的是为了提高获得锁和释放锁的效率。

***偏向锁（乐观锁）***

- 【作用】：偏向锁是为了消除无竞争情况下的同步原语，进一步提升程序性能。
- 【与轻量级锁的区别】：轻量级锁是在无竞争的情况下使用CAS操作来代替互斥量的
  使用，从而实现同步；而偏向锁是在无竞争的情况下完全取消同步。
- 【与轻量级锁的相同点】：它们都是乐观锁，都认为同步期间不会有其他线程竞争
- 【原理】：当线程请求到锁对象后，将锁对象的状态标志位改为01，即偏向模
  式。然后使用CAS操作将线程的ID记录在锁对象的Mark Word中。以后该线程可以直接进入同步块，连CAS操作都不需要。但是，一旦有第二条线程需要竞争锁，那么偏向模式立即结束，进入轻量级锁的状态。
- 【优点】：偏向锁可以提高有同步但没有竞争的程序性能。但是如果锁对象时常
  被多条线程竞争，那偏向锁就是多余的。
- 【其他】：偏向锁可以通过虚拟机的参数来控制它是否开启。
  ![偏向锁的活得和撤销流程](https://raw.githubusercontent.com/92649264634/ImageAll/master/images/StudyNote/MulitThread/15458646131342.png)



***轻量级锁（乐观锁）***

- 【本质】：使用CAS取代互斥同步。
- 【背景】：『轻量级锁』是相对于『重量级锁』而言的，而重量级锁就是传统的锁。
- 【轻量级锁与重量级锁的比较】：重量级锁是一种悲观锁，它认为总是有多条线程要竞争锁，所以它每次处理共享数据时，不管当前系统中是否真的有线程在竞争锁，它都会使用互斥同步来保证线程的安全；而轻量级锁是一种乐观锁，它认为锁存在竞争的概率比较小，所以它不使用互斥同步，而是使用CAS操作来获得锁，这样能减少互斥同步所使用的『互斥量』带来的性能开销。
- 【实现原理】：对象头称为『Mark Word』，虚拟机为了节约对象的存储空间，对象处于不同的状态下，Mark Word中存储的信息也所有不同。Mark Word中有个标志位用来表示当前对象所处的状态。当线程请求锁时，若该锁对象的Mark Word中标志位为01（未锁定状态），则在该线程的栈帧中创建一块名为『锁记录』的空间，然后将锁对象的Mark Word拷贝至该空间；最后通过CAS操作将锁对象的Mark Word指向该锁记录；若CAS操作成功，则轻量级锁的上锁过程成功；若CAS操作失败，再判断当前线程是否已经持有了该轻量级锁；若已经持有，则直接进入同步块；若尚未持有，则表示该锁已经被其他线程占用，此时轻量级锁就要膨胀成重量级锁。
- 【前提】：轻量级锁比重量级锁性能更高的前提是，在轻量级锁被占用的整个同步周期内，不存在其他线程的竞争。若在该过程中一旦有其他线程竞争，那么就会膨胀成重量级锁，从而除了使用互斥量以外，还额外发生了CAS操作，因此更慢！
  ![轻量级锁及膨胀流程图](https://raw.githubusercontent.com/92649264634/ImageAll/master/images/StudyNote/MulitThread/23678641613215.png)




## 3. sychronized和loclk有什么区别？

4. 线程的安全性问题
出现线程安全的主要来源于JMM的设计，主要集中在主内存和线程的工作内存而导致的内存可见性问题，以及重排序导致的问题。
4. 造成死锁的原因？
5. 加锁会带来哪些性能问题？如何解决？
6. HashMap是线程安全的么？HashTable呢？ConcurrentHashMap有什么了解？