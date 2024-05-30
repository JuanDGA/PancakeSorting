import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

record PancakeStack(int[] stack) {
  @Override
  public int hashCode() {
    return Arrays.hashCode(stack);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PancakeStack that = (PancakeStack) o;
    return Arrays.equals(stack, that.stack);
  }
}

public class PancakeSorting {
  public static void main(String[] args) throws IOException {
    int[][] cases = PancakeSorting.getCases("P3.in");
    int[][] results = new int[cases.length][];

    PancakeSorting.initialMessage();

    long totalTime = System.currentTimeMillis();
    execute(cases, results);
    totalTime = System.currentTimeMillis() - totalTime;

    System.out.println("=".repeat(30) + " STATS " + "=".repeat(30));
    System.out.printf("Took %fs to solve all the cases.\n", totalTime / 1000.0);
    PancakeSorting.showStatistics(cases, results);
    System.out.println("=".repeat(67));

    PancakeSorting.exportResults(results, "P3.out");

    Visualization.createVisualization(cases[0]);
  }

  private static void showStatistics(int[][] cases, int[][] results) {
    float maximumFlips = Integer.MIN_VALUE;
    int maximumFlipsCase = 0;

    float minimumFlips = Integer.MAX_VALUE;
    int minimumFlipsCase = 0;

    float flips = 0;

    for (int i = 0; i < results.length; i++) {
      float flipsForCase = results[i].length / (float) cases[i].length;
      flips += flipsForCase;
      if (flipsForCase > maximumFlips) {
        maximumFlips = flipsForCase;
        maximumFlipsCase = i;
      }
      if (flipsForCase < minimumFlips) {
        minimumFlips = flipsForCase;
        minimumFlipsCase = i;
      }
    }

    flips /= cases.length;

    System.out.printf("The case that took more flips (%fn) was the case %d.\n", maximumFlips, maximumFlipsCase + 1);
    System.out.printf("The case that took less flips (%fn) was the case %d.\n", minimumFlips, minimumFlipsCase + 1);
    System.out.println("The average flips per case was " + flips + "n");
  }

  private static int reversedBinarySearch(int[] array, int value) {
    int left = 0;
    int right = array.length - 1;

    while (left <= right) {
      int mid = left + (right - left) / 2;

      if (array[mid] == value) {
        return mid;
      }

      if (array[mid] > value) {
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }

    return -1;
  }

  public static int[] flip(int[] array, int start) {
    int[] newArray = Arrays.copyOf(array, array.length);

    int end = array.length - 1;
    while (start < end) {
      int temp = newArray[start];
      newArray[start] = newArray[end];
      newArray[end] = temp;

      start += 1;
      end -= 1;
    }
    return newArray;
  }

  // The target stack is in fact the ordered stack.
  private static boolean isValidPair(int pancakeA, int pancakeB, int[] targetStack) {
    int pancakeAPosition = PancakeSorting.reversedBinarySearch(targetStack, pancakeA);
    int pancakeBPosition = PancakeSorting.reversedBinarySearch(targetStack, pancakeB);

    // One of the checked pancakes is not in the stack
    if (pancakeAPosition < 0 || pancakeBPosition < 0) return false;

    return Math.abs(pancakeAPosition - pancakeBPosition) == 1;
  }

  public static int getPairedNeighbors(int[] stack, int[] targetStack) {
    int distance = targetStack[0] != stack[0] ? 1 : 0;
    for (int i = 0; i < stack.length - 1; i++) {
      if (!isValidPair(stack[i], stack[i + 1], targetStack)) distance += 1;
    }
    return distance;
  }

  public static int getPairedNeighbors(int[] stack, int[] targetStack, int flippingAt, int prevDistance) {
    int distance = prevDistance;
    if (flippingAt == 0) {
      distance += stack[stack.length - 1] == targetStack[0] ? -1 : stack[0] == targetStack[0] ? 1 : 0;
      return distance;
    }
    if (isValidPair(stack[stack.length - 1], stack[flippingAt - 1], targetStack)) {
      distance -= 1;
    }
    if (isValidPair(stack[flippingAt], stack[flippingAt - 1], targetStack)) {
      distance += 1;
    }
    return distance;
  }

  private static List<Integer> search(int[] stack) throws IOException {
    int[] target = Arrays.copyOf(stack, stack.length);
    Arrays.sort(target);
    target = PancakeSorting.flip(target, 0);
    PancakeStack targetStack = new PancakeStack(target);
    PancakeStack start = new PancakeStack(stack);
    Map<PancakeStack, Integer> distances = new HashMap<>();
    PriorityQueue<PancakeStack> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));
    // (A, (B, IDX)) where IDX is the required index-flip to arrive from A to B
    Map<PancakeStack, Map<PancakeStack, Integer>> flipIndices = new HashMap<>();
    Map<PancakeStack, PancakeStack> prev = new HashMap<>();

    distances.put(start, PancakeSorting.getPairedNeighbors(stack, target));
    flipIndices.put(start, new HashMap<>());
    queue.add(start);
    prev.put(start, null);

    while (!queue.isEmpty()) { // N² (Near to)
      PancakeStack head = queue.poll(); // log(N²)

      if (head.equals(targetStack)) break;

      for (int i = 0; i < head.stack().length - 1; i++) { // N
        PancakeStack next = new PancakeStack(PancakeSorting.flip(head.stack(), i)); // N
        if (next.equals(prev.get(head))) continue;
        if (!distances.containsKey(next)) {
          flipIndices.putIfAbsent(next, new HashMap<>());
          flipIndices.get(next).put(head, i);
          distances.put(next, PancakeSorting.getPairedNeighbors(head.stack(), target, i, distances.get(head))); // log(n)
          prev.put(next, head);
          queue.add(next);
        }
      }
    }

    List<Integer> steps = new ArrayList<>();

    for (PancakeStack at = targetStack; at != null; at = prev.get(at)) {
      if (prev.get(at) == null) continue;
      steps.add(flipIndices.get(at).get(prev.get(at)));
    }
    Collections.reverse(steps);

    return steps;
  }

  public static int[][] getCases(String inputPath) throws FileNotFoundException {
    Scanner sc = new Scanner(new File(inputPath));
    int amount = Integer.parseInt(sc.nextLine().strip());
    int[][] cases = new int[amount][0];

    for (int caseIdx = 0; caseIdx < amount; caseIdx++) {
      cases[caseIdx] = Arrays.stream(sc.nextLine().strip().split(" ")).mapToInt(Integer::parseInt).toArray();
    }

    return cases;
  }

  private static void initialMessage() {
    System.out.println("*** This is a project addressing the pancake sorting problem. ***");
    System.out.println("*** The file P3.in must be present in order to run the project. ***");
    System.out.println("=".repeat(68));
    System.out.println("=".repeat(20) + " Juan David Guevara Arévalo " + "=".repeat(20));
    System.out.println("=".repeat(68));

    System.out.println("\nThis project uses multithreading to solve multiple cases at the same time.\n");

    System.out.println("The visualizations will be exported in a folder called \"visualizations\" in .dot format");
    System.out.println(".dot files can be rendered using the GraphViz command line or using online tools.");

    System.out.println("=".repeat(69));
    System.out.println("=".repeat(30) + " SOLVING " + "=".repeat(30));
    System.out.println("=".repeat(69));
  }

  private static void execute(int[][] cases, int[][] results) {
    int availableCores = Runtime.getRuntime().availableProcessors();
    int threadsToUse = Math.max(1, availableCores - 2);

    ExecutorService executorService = Executors.newFixedThreadPool(threadsToUse);

    System.out.println("Using " + threadsToUse + " threads...");

    for (int i = 0; i < cases.length; i++) {
      int idx = i;
      executorService.submit(() -> {
        try {
          results[idx] = PancakeSorting.search(cases[idx]).stream().mapToInt(it -> it).toArray();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }

    executorService.shutdown();
    try {
      boolean finished = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      if (!finished) System.err.println("Something went wrong");
    } catch (InterruptedException e) {
      e.printStackTrace(System.err);
    }
  }

  private static void exportResults(int[][] results, String file) throws IOException {
    System.out.println("Exporting results to [" + file + "]...");

    FileWriter writer = new FileWriter(file);

    for (int[] result : results) {
      if (result.length == 0) {
        writer.write("ORDENADO\n");
        continue;
      }
      String line = Arrays.stream(result)
          .mapToObj(String::valueOf)
          .collect(Collectors.joining(" "));
      writer.write(line + "\n");
    }

    writer.close();
  }
}

class Visualization {
  private static void cleanDirectory() {
    File folder = new File("visualizations");

    if (!folder.exists()) {
      if (folder.mkdir()) System.out.println("Created visualizations directory");
    }

    for (File file : Objects.requireNonNull(folder.listFiles())) {
      if (!file.delete()) break;
    }
  }

  private static String createNode(PancakeStack state, PancakeStack start, PancakeStack target, Set<PancakeStack> path) {
    String color;
    if (Arrays.equals(state.stack(), start.stack())) {
      color = "lightgreen";
    } else if (Arrays.equals(state.stack(), target.stack())) {
      color = "orange";
    } else if (path.contains(state)) {
      color = "yellow";
    } else {
      color = "lightblue";
    }
    return "\"" + Arrays.toString(state.stack()).replace(" ", "") + "\" [style=filled, color=" + color + "];";
  }

  private static String createEdge(PancakeStack from, PancakeStack to, int steps, int distance, int flipIdx) {
    return "\"" + Arrays.toString(from.stack()).replace(" ", "") + "\" -> \"" + Arrays.toString(to.stack()).replace(" ", "") +
        String.format("\" [label=\"%s;%s|%s\"];\n", steps, distance, flipIdx);
  }

  private static void createView(
      Set<PancakeStack> visited,
      Map<PancakeStack, Integer> flipsTo,
      Map<PancakeStack, Map<PancakeStack, Integer>> flipIndices,
      Map<PancakeStack, PancakeStack> prevStack,
      Map<PancakeStack, Integer> distances,
      PancakeStack start,
      PancakeStack target,
      Set<PancakeStack> currentPath,
      int step
  ) throws IOException {
    int factorial = java.util.stream.IntStream.rangeClosed(1, start.stack().length).reduce(1, (a, b) -> a * b);
    StringBuilder text = new StringBuilder(String.format("""
        digraph G {
          ranksep=0.5
          nodesep=0.05
          label="Visited %d out of %d states.";
          labelloc=top;
          labeljust=left;
        """, visited.size() - 1, factorial - 1));
    for (PancakeStack stack : visited) {
      text.append("\t").append(createNode(stack, start, target, currentPath)).append("\n");
    }

    for (PancakeStack stack : visited) {
      if (prevStack.get(stack) == null) continue;
      text.append("\t")
          .append(createEdge(prevStack.get(stack), stack, flipsTo.get(stack), distances.get(stack), flipIndices.get(stack).get(prevStack.get(stack))));
    }

    text.append("}\n");

    FileWriter writer = new FileWriter(String.format("visualizations/stepNo%d.dot", step));
    writer.write(text.toString());
    writer.close();
  }

  private static void doSearch(PancakeStack start, PancakeStack target) throws IOException {
    Map<PancakeStack, Set<PancakeStack>> paths = new HashMap<>();
    Map<PancakeStack, PancakeStack> prevStack = new HashMap<>();
    Map<PancakeStack, Integer> flipsTo = new HashMap<>();
    Map<PancakeStack, Map<PancakeStack, Integer>> flipIndices = new HashMap<>();
    Map<PancakeStack, Integer> distances = new HashMap<>();
    Set<PancakeStack> visited = new HashSet<>();

    PriorityQueue<PancakeStack> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

    Set<PancakeStack> initialPath = new LinkedHashSet<>();
    initialPath.add(start);
    distances.put(start, PancakeSorting.getPairedNeighbors(start.stack(), target.stack()));
    paths.put(start, initialPath);
    flipsTo.put(start, 0);
    visited.add(start);
    prevStack.put(start, null);
    queue.add(start);

    int step = 0;
    while (!queue.isEmpty()) {
      step++;
      PancakeStack head = queue.poll();
      createView(visited, flipsTo, flipIndices, prevStack, distances, start, target, paths.get(head), step);

      if (head.equals(target)) break;

      for (int i = 0; i < head.stack().length - 1; i++) {
        PancakeStack next = new PancakeStack(PancakeSorting.flip(head.stack(), i));
        if (!visited.contains(next)) {
          visited.add(next);
          flipsTo.put(next, flipsTo.get(head) + 1);
          flipIndices.putIfAbsent(next, new HashMap<>());
          flipIndices.get(next).put(head, i);
          distances.put(next, PancakeSorting.getPairedNeighbors(next.stack(), target.stack()));
          Set<PancakeStack> currentPath = new LinkedHashSet<>(paths.get(head));
          currentPath.add(next);
          prevStack.put(next, head);
          paths.put(next, currentPath);
          queue.add(next);
        }
      }
    }
  }

  public static void createVisualization(int[] caseToVisualize) throws IOException {
    cleanDirectory();

    int[] target = Arrays.copyOf(caseToVisualize, caseToVisualize.length);
    Arrays.sort(target);
    target = PancakeSorting.flip(target, 0);
    PancakeStack start = new PancakeStack(caseToVisualize);
    PancakeStack targetStack = new PancakeStack(target);

    doSearch(start, targetStack);

    System.out.println("Visualization created successfully");
  }
}