# Packet Router Problem - a Java concurrency study

### Conclusions

The main conclusion is most java systems should be using Agrona
for concurrent queue operations.

[Agrona SPSC](https://github.com/real-logic/agrona) can run more than 10x the speed of the Java Concurent Queues.

A broader conclusion can be drawn regarding modern CPU architecture.

Single threaded handoffs on mondern CPUs are the fastest way for threads
to communicate.

### How to run / reproduce

**./runPerfTest.sh**

needs only 'mvn' and java in your path to run

### Sample results
[perftest_laptop.txt](https://github.com/crodier/packet-router/blob/master/perftest_laptop.txt)

## Background

This problem is related to packet switching, also 
the current CFS Linux scheduler:

https://en.wikipedia.org/wiki/Run_queue

----

This test harness explores Java concurrent Queues, implementation performance.

Message priorities are handled.

In this harness, there are four priority levels of the messages.

1.  Management message:  urgent / low
2.  Normal Priority:  urgent / low

Concurrent Queues are necessary for 'sequential problems.'

Sequential processing is the most common type of message processing,
because message *fairness* is almost always a requirement.

We show the JDK queues are typically half the speed
of two libraries using unsafe and ring-buffer techniques:

- Agrona (Martin Thompson, Aeron)
- JCTools (Apache)

The JDK ConcurrentLinkedQueue proves it is great for general purpose work,
as it can outperform in very high concurrency scenarios; however,
high levels of concurrency are rarely observed.


## Code review

Please see **PacketRouterTest**  "**main**" method as an entry point.

The main method tests each of the PacketRouter implementations,
once each with the *Busy Wait* and *Blocking Wait* strategies.

---

This project performs, Java, concurrent Queue performance evaluations,
dealing with minor priority differences in the messages.

See the problem:  **[problem.txt](https://github.com/crodier/packet-router/blob/master/problem.txt)** (suitable for interviews)

This problem, explores Java from the perspective of the work of Leslie Lamport, 
inventor of 'Byzantine Generals' and other concurrent theory:

- https://en.wikipedia.org/wiki/Leslie_Lamport
- http://lamport.azurewebsites.net/pubs/pubs.html

### Problem Summary
- With message priorities, then, ordered
- test the performance of Java Queues for **ordered handoff** of messages, *in-process*
- evaluate the available java libraries, across different strategies
- e.g. Single Producer Single Consumer (SPSC)
- e.g. Multiple Producer Single consumer (MPSC)

### Results

The fastest queues are ring buffers, using transactional memory.

- **(winner) Agrona** - ring buffer based (+ unsafe access), Martin Thompson, HFT finance
- (close second) **JCTools collections**, Apache, with custom spin wait strategy
- 2x slower:  Java Lists, with custom Wait strategies
- 3x slower:  Java Priority Queue is the slowest approach tested

The winner:  **Agrona**, from Martin Thompson, of Aeron fame

- Agrona is a library used in Aeron, which is reliable messaging over UDP and IPC framework for messaging.
    - Aeron is difficult to use, but is the performance champion, and is used in HFT
- Agrona is a producer-consumer winner for ordered message handoff

### Summary Findings

- "Agrona", library is the winner:  
    https://github.com/real-logic/Agrona
- Single Producer, Single Consumer (SPSC), has the highest throughput!  Beating out, using multiple threads.
- Follow up question:  What does Agrona tell us about, Java and our chip speed, for the operations? (how fast is the machine L1/L2, based on this test)

### More findings

A close second is JCTools SPSC queue, followed by other JCTools queues.

The JDK queues lag behind, 2 - 3x slower for this problem.
While they improve  under high contention cases, 
are  still inferior to JCTools (java concurrency tools.)

Useful blog articles: 
- http://psy-lob-saw.blogspot.com/2013/03/single-producerconsumer-lock-free-queue.html
- http://psy-lob-saw.blogspot.com/p/lock-free-queues.html
- https://psy-lob-saw.blogspot.com/2015/01/mpmc-multi-multi-queue-vs-clq.html

## My solution

To explore Java Queue performance:

Performance tests are the right way to evaluate, which is the fastest.
With a complex interaction of software, L1/L2 caches and the chip, it is too easy to make a mistake reasoning, about performance.
Performance may also change depending on the hardware you are running on,
but the results are typically stable across machines.

## Notes on Priority messaging

Due to the relaxed ordering of the problem specification, as far as non-atomic read/writes of priority.
I use four queues to handle priority (one for each of mgmt large, mgmt, user large, user),
instead of a Priority Blocking Queue, the natural selection from Java (but I also test Java Priority Queue for reference.)

While experimental low-lock queues exist, lists of Queues are expected to perform better, with either Busy and Blocking waits, and I try both approaches.

Using multiple Queues perform better than one Priority Queue, 

The alternative is any Priority Queue.
But a priority queue, 
 always needs the remove operation to swim up
  the new maximum element, 
  and maintain the other elements in order.

Checking each Queue, instead of popping the top of a PriorityQueue, is more work for the readers;
however, the four queues, interestingly, reduces locking, blocking, and reduces the lock contention
by distributing against multiple Queues instead of, contending for one.
The reduction in a single point of contention dominates 
the performance vs. doing a few more operations with less contention.

## Building and Running

Either Eclipse or IntelliJ may be used to open the Maven pom.xml file, as a project file.

The test output shows the strengths and weaknesses of the different approaches.

### JCTools review

JCTools has extensive Queue type implementations, 
for wait-free, unsafe and other approaches.

Most of these JCTools queues, use Ring buffers, avoiding false sharing on L1, and other "mechanical sympathy" techniques.

I tested with SPSC, MPSC, and MPMC from JCTools. 
There are many and may be worth reviewing for any high performance Java system.

At the time of writing, the JCTools queues are size 8,000,000 and don't grow 
(they must be changed with the test, if we increase the test message size.)

JCTools also offer collections where the ring buffers grow. 
These introduce, a small but meaningful overhead to the sized ring buffers.
Presumably this is due to the memory moving in and out of L2 and into L1 instead of being pinned
in the L2 cache.

In my JCTools experiments, 
I am using static sized ring size buffers, to find the best throughput,
for expected message rates.

## Code Notes

Provided are two approaches for handling thread contention.
 
One is *Busy Wait*, the other, a *Locking Wait* strategy.  

The results are as expected.  Locking outperforms, only under very high contention.

As contention increases, we can see the *Locking Wait* winning tests (but not always, even in high contention.)

## Java

java version "1.8.0_60"
Java(TM) SE Runtime Environment (build 1.8.0_60-b27)
Java HotSpot(TM) 64-Bit Server VM (build 25.60-b23, mixed mode)

## My old (ubuntu) i7 laptop

processor       : 0
vendor_id       : GenuineIntel
cpu family      : 6
model           : 58
model name      : Intel(R) Core(TM) i7-3630QM CPU @ 2.40GHz
stepping        : 9
microcode       : 0x17
cpu MHz         : 1210.593
cache size      : 6144 KB
physical id     : 0
siblings        : 8
core id         : 0
cpu cores       : 4
apicid          : 0
initial apicid  : 0
fpu             : yes
fpu_exception   : yes
cpuid level     : 13
wp              : yes

