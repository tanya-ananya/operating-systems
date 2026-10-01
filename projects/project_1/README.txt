Project One 
Author: Taaruni Ananya
Class: MW 12:45pm - 2:30pm

1. LANGUAGE/VERSION
-----------------------
Language : Java

2. COMPILATION COMMAND
----------
Run the following (contains src/, input/, output/):
    javac -d bin src/Main.java

3. RUN
------
    java -cp bin Main

Menu:
    1. Round Robin                  asks for: process file, time quantum
    2. SJF (non-preemptive)         asks for: process file
    3. Compare RR vs SJF            asks for: process file, time quantum (both algorithms run on the same data)
    4. Best-Fit memory allocation   asks for: memory file
    5. LRU paging                   asks for: paging file
    0. Exit

4. EXECUTION EXAMPLES:
    $ java -cp bin Main
    Choose: 1
    Process file: input/scheduled_roundrobin.txt
    Time quantum: 3
    ... Gantt chart, table, averages ...
    Choose: 0

Execution example (non-interactive, answers read from a file):
    java -cp bin Main < input/menu_scripts/rr_q3.txt

4. IMPLEMENTED ALGORITHMS
-------------------------
CPU scheduling   : Round Robin (required) and non-preemptive SJF
Memory           : Best-Fit contiguous allocation
Page replacement : LRU (Least Recently Used)

5. ASSUMPTIONS
---------------------------------
  - SJF has ties following the early arrivals and then the file order
  - Round Robin: the unfinished processes are requeued after every process that has arrived joins the queue when a slice ends
  - When there aren't any processes ready, the time will jump to the next arrival
  - Best-Fit will choose the smallest free block that is large enough, reduce this block according the request size. Requests that don't fit single blocks are unallocated 
  - LRU uses fixed frame slots where the empty frames are filled first, then the least recently used page will be replaced. Each hit will count.