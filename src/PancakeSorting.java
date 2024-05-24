import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PancakeSorting {
  private static int[] flip(int[] array, int start) {
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
    int pancakeAPosition = Arrays.binarySearch(targetStack, pancakeA);
    int pancakeBPosition = Arrays.binarySearch(targetStack, pancakeB);

    // One of the checked pancakes is not in the stack
    if (pancakeAPosition < 0 || pancakeBPosition < 0) return false;

    return Math.abs(pancakeAPosition - pancakeBPosition) == 1;
  }

  private static int getDistance(int[] stack, int[] targetStack) {
    int distance = 0;
    for (int i = 0; i < stack.length - 1; i++) {
      if (isValidPair(stack[i], stack[i + 1], targetStack)) continue;
      distance += 1;
    }
    return distance;
  }

  private static List<Integer> search(int[] stack) {
    int[] targetStack = Arrays.copyOf(stack, stack.length);
    Arrays.sort(targetStack);
    targetStack = PancakeSorting.flip(targetStack, 0);
    Map<Integer, int[]> stacks = new HashMap<>(stack.length * stack.length); // Stack-By-HashCode

    int originKey = Arrays.hashCode(stack);
    int targetKey = Arrays.hashCode(targetStack);

    stacks.put(originKey, stack);
    stacks.put(targetKey, targetStack);

    Map<Integer, Integer> distances = new HashMap<>(stack.length * stack.length); // Distance-By-HashCode
    Map<Integer, Map<Integer, Integer>> flipIndex = new HashMap<>();
    Map<Integer, Integer> prev = new HashMap<>(stack.length * stack.length);

    PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(distances::get));

    distances.put(originKey, 0);
    prev.put(originKey, null);
    pq.add(originKey);

    while (!pq.isEmpty()) {
      int headKey = pq.poll();
      int[] head = stacks.get(headKey);

      if (Arrays.equals(head, targetStack)) break;

      for (int i = 0; i < head.length - 1; i++) {
        int[] next = PancakeSorting.flip(head, i);
        int key = Arrays.hashCode(next);
        if (!distances.containsKey(key)) {
          distances.put(key, PancakeSorting.getDistance(next, targetStack));
          stacks.put(key, next);
          flipIndex.putIfAbsent(key, new HashMap<>());
          flipIndex.get(key).put(headKey, i);
          prev.put(key, headKey);
          pq.add(key);
        }
      }
    }

    List<Integer> steps = new ArrayList<>();

    for (Integer at = targetKey; at != null; at = prev.get(at)) {
      if (prev.get(at) != null) steps.add(flipIndex.get(at).get(prev.get(at)));
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

  private static int indexOfShortestList(List<List<Integer>> lists, int[][] toDistribute) {
    int minimum = Integer.MAX_VALUE;
    int minimumIndex = 0;

    for (int i = 0; i < lists.size(); i++) {
      List<Integer> list = lists.get(i);
      int charge = list.stream().mapToInt(it -> toDistribute[it].length).sum();
      if (charge < minimum) {
        minimum = charge;
        minimumIndex = i;
      }
    }

    return minimumIndex;
  }

  public static int[][] distributeCases(int[][] cases, int amount) {
    System.out.println("Distributing " + cases.length + " cases in " + amount + " blocks...");
    int[][] toDistribute = Arrays.copyOf(cases, cases.length);
    Arrays.sort(toDistribute, Comparator.comparingInt(it -> -it.length));

    List<List<Integer>> blocks = new ArrayList<>();

    for (int i = 0; i < amount; i++) blocks.add(new ArrayList<>());

    for (int i = 0; i < toDistribute.length; i++) {
      blocks.get(PancakeSorting.indexOfShortestList(blocks, toDistribute)).add(i);
    }

    int[][] result = new int[amount][0];

    for (int i = 0; i < blocks.size(); i++) {
      result[i] = blocks.get(i).stream().mapToInt(it -> it).toArray();
    }

    return result;
  }

  private static void multipleSearch(int[] casesToSolve, int[][] cases, int[][] results) {
    System.out.printf("Working with %d cases in thread %d\n", casesToSolve.length, Thread.currentThread().getId());
    for (int caseIdx = 0; caseIdx < casesToSolve.length; caseIdx++) {
      results[caseIdx] = PancakeSorting.search(cases[caseIdx]).stream().mapToInt(it -> it).toArray();
    }
  }

  public static void main(String[] args) throws FileNotFoundException {
    int availableCores = Runtime.getRuntime().availableProcessors();
    int threadsToUse = Math.max(1, availableCores - 2);

    System.out.println("Using " + threadsToUse + " threads...");

    int[][] cases = PancakeSorting.getCases("P3.in");
    int[][] distributions = PancakeSorting.distributeCases(cases, threadsToUse);
    int[][] results = new int[cases.length][];

    ExecutorService executorService = Executors.newFixedThreadPool(threadsToUse);

    for (int i = 0; i < threadsToUse; i++) {
      final int idx = i;
      executorService.submit(() -> PancakeSorting.multipleSearch(distributions[idx], cases, results));
    }
    executorService.shutdown();
    try {
      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace(System.err);
    }
  }
}