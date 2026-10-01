import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

class Process {
    String pid;
    int arrivalTime;
    int burstTime;
    int priority;
    int completionTime;
    int remainingTime;
    boolean completed = false;
    int turnaround() { return completionTime - arrivalTime; }
    int waiting() { return turnaround() - burstTime; }
}

class Main {

    static ArrayList<Process> readProcesses(String filename) {
        ArrayList<Process> list = new ArrayList<>();
        List<String> lines;

        try {
            lines = Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("ERROR: cannot read file " + filename);
            return null;
        }

        ArrayList<String> errors = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            line = line.replace("\uFEFF", "");
            String[] parts = line.split("\\s+");
            if (parts[0].equalsIgnoreCase("PID")) continue;
            String where = "Line " + (i + 1) + ": ";

            if (parts.length < 4) {
                errors.add(where + "missing field(s), expected 4 (PID Arrival_Time Burst_Time Priority) but found " + parts.length);
                continue;
            }
            if (parts.length > 4) {
                errors.add(where + "too many fields, expected 4 but found " + parts.length);
                continue;
            }

            int arrival, burst, priority;
            try {
                arrival = Integer.parseInt(parts[1]);
                burst = Integer.parseInt(parts[2]);
                priority = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                errors.add(where + "Arrival_Time, Burst_Time and Priority must be whole numbers");
                continue;
            }

            boolean ok = true;
            if (arrival < 0) {
                errors.add(where + "negative arrival time for " + parts[0]); ok = false;
            }
            if (burst <= 0) {
                errors.add(where + "burst time must be positive for " + parts[0]); ok = false;
            }
            if (!seen.add(parts[0])) {
                errors.add(where + "duplicate PID " + parts[0]); ok = false;
            }

            if (ok) {
                Process p = new Process();
                p.pid = parts[0];
                p.arrivalTime = arrival;
                p.burstTime = burst;
                p.priority = priority;
                list.add(p);
            }
        }
        
        if (errors.isEmpty() && list.isEmpty()) errors.add("no processes found in file");
        if (!errors.isEmpty()) {
            System.out.println("ERROR: invalid input in '" + filename + "':");
            for (String e : errors) {
                System.out.println("  - " + e);
            }
            return null;
        }
        return list;
    }

    static void resetProcesses(ArrayList<Process> list) {
        for (Process p : list) {
            p.remainingTime = p.burstTime;
            p.completionTime = 0;
            p.completed = false;
        }
    }

    static void runSJF(ArrayList<Process> list, ArrayList<String> labels, ArrayList<Integer> times) {
        resetProcesses(list);
        int timeCheck = 0;
        int finished = 0;
        times.add(0);
 
        while (finished < list.size()) {
            Process bestProcess = null;
            for (Process p : list) {
                if (p.completed) continue;
                if (p.arrivalTime > timeCheck) continue;
                if (bestProcess == null
                        || p.burstTime < bestProcess.burstTime
                        || (p.burstTime == bestProcess.burstTime && p.arrivalTime < bestProcess.arrivalTime)) {
                    bestProcess = p;
                }
            }
            if (bestProcess == null) {
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : list) {
                    if (!p.completed && p.arrivalTime < nextArrival) nextArrival = p.arrivalTime;
                }
                labels.add("IDLE");
                times.add(nextArrival);
                timeCheck = nextArrival;
            } else {
                timeCheck += bestProcess.burstTime;
                labels.add(bestProcess.pid);
                times.add(timeCheck);
                bestProcess.completed = true;
                bestProcess.completionTime = timeCheck;
                finished++;
            }
        }
    }

    static void runRR(ArrayList<Process> list, int time, ArrayList<String> labels, ArrayList<Integer> times) {
        resetProcesses(list);

        ArrayList<Process> byArrival = new ArrayList<>(list);
        byArrival.sort((a, b) -> a.arrivalTime - b.arrivalTime);
 
        ArrayDeque<Process> queue = new ArrayDeque<>();
        int next = 0;
        int finished = 0;
        int timeCheck = 0;
        times.add(0);
 
        while (finished < list.size()) {
            while (next < byArrival.size() && byArrival.get(next).arrivalTime <= timeCheck) {
                queue.add(byArrival.get(next));
                next++;
            }

            if (queue.isEmpty()) {
                int nextArrival = byArrival.get(next).arrivalTime;
                labels.add("IDLE");
                times.add(nextArrival);
                timeCheck = nextArrival;
                continue;
            }
 
            Process p = queue.poll();
            int run = Math.min(time, p.remainingTime);
            timeCheck += run;
            p.remainingTime -= run;
            labels.add(p.pid);
            times.add(timeCheck);
 
            while (next < byArrival.size() && byArrival.get(next).arrivalTime <= timeCheck) {
                queue.add(byArrival.get(next));
                next++;
            }

            if (p.remainingTime > 0) {
                queue.add(p);
            } else {
                p.completionTime = timeCheck;
                p.completed = true;
                finished++;
            }
        }
    }

    static void bestFit(ArrayList<Integer> blocks, ArrayList<Integer> requests) {
        System.out.println();
        System.out.println("=== Contiguous Memory Allocation: Best-Fit ===");
        System.out.println("Initial blocks: " + blocks);

        ArrayList<Integer> remaining = new ArrayList<>(blocks);

        for (int x = 0; x < requests.size(); x++) {
            int size = requests.get(x);
            int best = -1;

            for (int y = 0; y < remaining.size(); y++) {
                if (remaining.get(y) >= size && (best == -1 || remaining.get(y) < remaining.get(best))) {
                        best = y;
                }
            }

            if (best == -1) {
                System.out.println("Request " + (x + 1) + " (" + size + ") -> UNALLOCATED");
            } else {
                int before = remaining.get(best);
                remaining.set(best, before - size);
                System.out.println("Request " + (x + 1) + " (" + size + ") -> Block " + (best + 1)
                        + " (" + before + " -> " + (before - size) + " left)");
            }
        }
        System.out.println("Remaining block sizes: " + remaining);
    }

    static void printResults(String title, ArrayList<Process> list, ArrayList<String> labels, ArrayList<Integer> times) {
        System.out.println();
        System.out.printf("%-6s %8s %6s %6s %6s %6s%n", "PID", "Arrival", "Burst", "CT", "TAT", "WT");
        for (Process p : list) {
            System.out.printf("%-6s %8d %6d %6d %6d %6d%n", p.pid, p.arrivalTime, p.burstTime,
            p.completionTime, p.turnaround(), p.waiting());
        }

        double[] m = calculateMetrics(list);
        System.out.println();
        System.out.println("Average WT      : " + String.format("%.2f", m[0]));
        System.out.println("Average TAT     : " + String.format("%.2f", m[1]));
        System.out.println("CPU Utilization : " + String.format("%.2f", m[2]) + "%");
    }

   static double[] calculateMetrics(ArrayList<Process> list) {
       double totalWT = 0;
       double totalTAT = 0;
       int busy = 0;
       int lastCompletion = 0;
       for (Process p : list) {
           totalWT += p.waiting();
           totalTAT += p.turnaround();
           busy += p.burstTime;
           lastCompletion = Math.max(lastCompletion, p.completionTime);
       }
       return new double[] { totalWT / list.size(), totalTAT / list.size(), 100.0 * busy / lastCompletion };
   }

    static void lru(int frameCount, ArrayList<Integer> refs) {
        System.out.println();
        System.out.println("=== Paging: LRU (" + frameCount + " frames) ===");

        int[] frames = new int[frameCount];
        int[] lastUsed = new int[frameCount];
        for (int x = 0; x < frameCount; x++) frames[x] = -1;
        int hits = 0, faults = 0, clock = 0;

        for (int page : refs) {
            clock++;
            int where = -1;

            for (int x = 0; x < frameCount; x++) if (frames[x] == page) where = x;

            String result;
            if (where != -1) {
                hits++;
                lastUsed[where] = clock;
                result = "HIT";
            } else {
                faults++;
                int victim = -1;
                for (int f = 0; f < frameCount; f++) if (frames[f] == -1) { victim = f; break; }
                if (victim == -1) {
                    victim = 0;
                    for (int f = 1; f < frameCount; f++) if (lastUsed[f] < lastUsed[victim]) victim = f;
                }
                frames[victim] = page;
                lastUsed[victim] = clock;
                result = "FAULT";
            }

            StringBuilder sb = new StringBuilder("[");
            for (int x = 0; x < frameCount; x++) {
                if (x > 0) sb.append(' ');
                sb.append(frames[x] == -1 ? "-" : String.valueOf(frames[x]));
            }
            sb.append("]");
            System.out.printf("%-4d %-14s %s%n", page, sb, result);
        }

        int total = hits + faults;
        System.out.println();
        System.out.println("Total references: " + total);
        System.out.println("Hits            : " + hits);
        System.out.println("Faults          : " + faults);
        System.out.println("Hit ratio       : " + String.format("%.2f", (double) hits / total));
        System.out.println("Fault ratio     : " + String.format("%.2f", (double) faults / total));
    }

    static boolean readMemory(String filename, ArrayList<Integer> blocks, ArrayList<Integer> requests) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("ERROR: cannot read file " + filename);
            return false;
        }
        ArrayList<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.replace("\uFEFF", "");
            String[] parts = line.replace(',', ' ').trim().split("\\s+");
            String key = parts[0].toUpperCase();
            String where = "Line " + (i + 1) + ": ";

            boolean isBlocks = key.equals("BLOCKS") || key.equals("BLOCK");
            boolean isRequests = key.equals("REQUESTS") || key.equals("REQUEST");

            if (!isBlocks && !isRequests) {
                errors.add("Line " + (i + 1) + ": expected a BLOCKS or REQUESTS line");
                continue;
            }
            if (parts.length < 2) {
                errors.add("Line " + (i + 1) + ": no numbers after " + key);
                continue;
            }

            for (int k = 1; k < parts.length; k++) {
                try {
                    int size = Integer.parseInt(parts[k]);
                    if (size <= 0) throw new NumberFormatException();
                    if (isBlocks) {
                        blocks.add(size);
                    } else {
                        requests.add(size);
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + (i + 1) + ": '" + parts[k] + "' is not a positive whole number");
                }
            }
        }
        if (errors.isEmpty() && blocks.isEmpty()) errors.add("no BLOCKS line found");
        if (errors.isEmpty() && requests.isEmpty()) errors.add("no REQUESTS line found");
        if (!errors.isEmpty()) {
            System.out.println("ERROR: invalid input in '" + filename + "':");
            for (String e : errors) System.out.println("  - " + e);
            return false;
        }
        return true;
    }

    static int readPaging(String filename, ArrayList<Integer> refs) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("ERROR: cannot read file " + filename);
            return -1;
        }
        ArrayList<String> errors = new ArrayList<>();
        int frames = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.replace("\uFEFF", "");
            String[] parts = line.replace(',', ' ').trim().split("\\s+");
            String key = parts[0].toUpperCase();

            if (key.equals("FRAMES") || key.equals("FRAME")) {
                if (parts.length != 2) {
                    errors.add("Line " + (i + 1) + ": FRAMES needs exactly one number");
                    continue;
                }
                try {
                    frames = Integer.parseInt(parts[1]);
                    if (frames <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    errors.add("Line " + (i + 1) + ": frame count must be a positive whole number");
                    frames = 0;
                }
            } else if (key.equals("REFERENCES") || key.equals("REFERENCE") || key.equals("REFS")) {
                if (parts.length < 2) {
                    errors.add("Line " + (i + 1) + ": no page numbers given");
                    continue;
                }
                for (int k = 1; k < parts.length; k++) {
                    try {
                        int page = Integer.parseInt(parts[k]);
                        if (page < 0) throw new NumberFormatException();
                        refs.add(page);
                    } catch (NumberFormatException e) {
                        errors.add("Line " + (i + 1) + ": '" + parts[k] + "' is not a valid page number");
                    }
                }
            } else {
                errors.add("Line " + (i + 1) + ": expected a FRAMES or REFERENCES line");
            }
        }

        if (errors.isEmpty() && frames == 0) errors.add("no valid FRAMES line found");
        if (errors.isEmpty() && refs.isEmpty()) errors.add("no REFERENCES line found");
        if (!errors.isEmpty()) {
            System.out.println("ERROR: invalid input in '" + filename + "':");
            for (String e : errors) System.out.println("  - " + e);
            return -1;
        }
        return frames;
    }

    static String ask(Scanner in, String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) return null;
        return in.nextLine().trim();
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println();
            System.out.println("1) Round Robin   2) SJF   3) Compare RR vs SJF");
            System.out.println("4) Best-Fit memory   5) LRU paging   0) Exit");
            System.out.print("Choose: ");
            if (!in.hasNextLine()) break;
            String choice = in.nextLine().trim();
            if (choice.equals("0")) break;

            if (choice.equals("1") || choice.equals("2") || choice.equals("3")) {
                String file = ask(in, "Process file: ");
                if (file == null) break;

                int quantum = 0;
                if (!choice.equals("2")) {
                    String text = ask(in, "Time quantum: ");
                    if (text == null) break;
                    try {
                        quantum = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        quantum = 0;
                    }
                    if (quantum <= 0) {
                        System.out.println("ERROR: time quantum must be a positive whole number (got '" + text + "')");
                        continue;
                    }
                }

                ArrayList<Process> list = readProcesses(file);
                if (list == null) continue;
                
                if (choice.equals("1") || choice.equals("3")) {
                    ArrayList<String> labels = new ArrayList<>();
                    ArrayList<Integer> times = new ArrayList<>();
                    runRR(list, quantum, labels, times);
                    printResults("Round Robin (quantum " + quantum + ")", list, labels, times);
                }
                if (choice.equals("2") || choice.equals("3")) {
                    ArrayList<String> labels = new ArrayList<>();
                    ArrayList<Integer> times = new ArrayList<>();
                    runSJF(list, labels, times);
                    printResults("SJF (non-preemptive)", list, labels, times);
                }
            } else if (choice.equals("4")) {
                String file = ask(in, "Memory file: ");
                if (file == null) break;
                ArrayList<Integer> blocks = new ArrayList<>();
                ArrayList<Integer> requests = new ArrayList<>();
                if (readMemory(file, blocks, requests)) {
                    bestFit(blocks, requests);
                }
            } else if (choice.equals("5")) {
                String file = ask(in, "Paging file: ");
                if (file == null) break;
                ArrayList<Integer> refs = new ArrayList<>();
                int frameCount = readPaging(file, refs);
                if (frameCount != -1) {
                    lru(frameCount, refs);
                }
            } else {
                System.out.println("Please enter a number from 0 to 5.");
            }
        }
    }
}