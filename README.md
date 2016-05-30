# Packet Router Problem, ICE

The best way to explore the problem like many threading / data structure problems is testing.

*./runPerfTest.sh*

...will build, run tests, and kick off a performance analysis (only need Maven in your PATH.)

Either Eclipse or IntelliJ may be used to open the Maven pom.xml file, as a project file.

The test output shows the strengths and weaknesses of the different approaches, which focus on using four Queues to handle priority.

Due to the relaxed ordering of the problem specification, as far as non-atomic read/writes of priority.
I use four queues to handle priority (one for each of mgmt large, mgmt, user large, user),
instead of a Priority Blocking Queue, the natural selection from Java (but I also test Java Priority Queue for reference.)
While experimental low-lock queues exist, lists of Queues are expected to perform better, with either Busy and Blocking waits, and I try both approaches.

Queue performance evaluations:

(best) JCTools collections, with custom wait strategies
Java Lists, with custom Wait strategies
Java Priority Queue is the slowest approach tested

Provided is a busy wait, and a locking wait strategy.  As contention increases, we can see the Locking Wait winning tests (but not always, even in high contention.)
I am unable to achive high contention on my laptop, but can see the beginings of locking performance catching up with 8 cores in use, as expected.

The best result is SPSC, with the JCTools SPSC queue, followed by other JCTools queues.

The java queues lag behind, improving under high contention cases, but still inferior to JCTools (java concurrency tools.)

JCTools website and documentation, have a detailed discussion of thread contention issues on queues, and wait-free, different approaches.

Queues are expected to perform better, as the work is only to add to the end,
where any Priority Queue needs the remove operation to swim up the new maximum element, and maintain the other elements in order.

This makes slightly more work for the readers, checking each Queue, instead of popping the top of a PriorityQueue
But the four queues, interestingly, reduces locking, blocking, and reduces the lock contention
by distributing against multiple Queues instead of threads contending for one.

JCTools has extensive Queue implementations, for wait-free, unsafe and other approaches.

Many or most of these JCTools queues, use Ring buffers, avoiding false sharing on L1, and other "mechanical sympathy" techniques.

I tested with SPSC, MPSC, and MPMC from JCTools, but there are many and may be worth reviewing for any high performance Java system.

At the time of writing, the JCTools queues are sized at 8,000,000 and don't grow (they must be changed with the test, if we increase the test size.)
Growable JCTools collections exist, at a small marginal but meaningful overhead to the sized ring buffers.
I am using the ungrowable ring size buffers, too see what the best throughput looks like.

## Code review

Please see *PacketRouterTest* main method as an entry point.

The main method tests each of the PacketRouter implementations,
with the Busy Wait and Blocking Wait strategies.

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
