import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Класс Main решает задачу минимального числа шагов для четырёх роботов,
 * чтобы собрать все ключи в лабиринте 2D-сетки со стенами, дверями и ключами.
 *
 * Алгоритм использует BFS + Дейкстра т.к. время работы обычным BFS или дейкстрой на больших картах превышало ограничения
 * Алгоритм разбит на 3 этапа:
 *
 * 1) Нахождение точек интереса
 *    - Пробегаем весь лабиринт и фиксируем позиции четырёх роботов и всех ключей
 *    - Каждому ключу присваиваем уникальный индекс 0..K-1 в порядке появления
 *    - Результат: массив graphNodes, где первые 4 элемента — старты роботов,
 *      следующие K — позиции ключей
 *
 * 2) Предварительный обход (BFS) из каждой точки интереса
 *    - Для каждой точки из graphNodes запускаем BFS по исходному лабиринту.
 *    - Во время обхода накапливаем:
 *        distanceMatrix[i][k]     — минимальное число шагов от точки i до ключа k,
 *        requiredKeysMatrix[i][k] — битовую маску ключей, которые нужно иметь.
 *    - Это позволяет один раз просчитать все пути между «интересными» узлами, а затем не лазить
 *      по всему лабиринту при поиске оптимального маршрута.
 *
 * 3) Поиск по пространству состояний (Дейкстра)
 *    - Состояние описывается классом State: позиции четырёх роботов и битовая маска уже собранных ключей.
 *    - Граф состояний описывает позицию каждого робота (узел из graphNodes)
 *      и маску собранных ключей.
 *    - Алгоритм Дейкстры использует предварительно вычисленные матрицы для оценки веса переходов
 *      и доступности путей.
 *
 */

public class run2 {
    private static final char[] KEY_CHARACTERS  = new char[26];
    private static final char[] DOOR_CHARACTERS = new char[26];

    static {
        for (int i = 0; i < 26; i++) {
            KEY_CHARACTERS[i]  = (char)('a' + i);
            DOOR_CHARACTERS[i] = (char)('A' + i);
        }
    }

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    private static class Position {
        final int row, column;
        Position(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }

    private static class State {
        final int[] robotNodeIndices;
        final int keysMask;

        State(int[] robotNodeIndices, int keysMask) {
            this.robotNodeIndices = robotNodeIndices.clone();
            this.keysMask = keysMask;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State other = (State)o;
            return keysMask == other.keysMask &&
                    Arrays.equals(robotNodeIndices, other.robotNodeIndices);
        }

        @Override
        public int hashCode() {
            return 31 * keysMask + Arrays.hashCode(robotNodeIndices);
        }
    }

    private static class SearchNode {
        final State state;
        final int steps;
        SearchNode(State state, int steps) {
            this.state = state;
            this.steps = steps;
        }
    }

    public static void main(String[] args) throws IOException {
        char[][] maze = readMazeFromInput();
        int result = computeMinimalStepsToCollectAllKeys(maze);
        if (result == Integer.MAX_VALUE) {
            System.out.println("No solution found");
        } else {
            System.out.println(result);
        }
    }

    private static char[][] readMazeFromInput() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            lines.add(line);
        }
        char[][] maze = new char[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
            maze[i] = lines.get(i).toCharArray();
        }
        return maze;
    }


    private static int computeMinimalStepsToCollectAllKeys(char[][] maze) {
        int rowCount    = maze.length;
        int columnCount = maze[0].length;


        List<Position> robotStarts         = new ArrayList<>();
        Map<Character,Integer> keyToIndex  = new HashMap<>();
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char ch = maze[r][c];
                if (ch == '@') {
                    robotStarts.add(new Position(r, c));
                } else if (ch >= 'a' && ch <= 'z') {
                    keyToIndex.computeIfAbsent(ch, __ -> keyToIndex.size());
                }
            }
        }
        int robotCount = robotStarts.size();
        int keyCount = keyToIndex.size();
        int nodeCount = robotCount + keyCount;

        Position[] graphNodes = new Position[nodeCount];
        for (int i = 0; i < robotCount; i++) {
            graphNodes[i] = robotStarts.get(i);
        }
        for (Map.Entry<Character,Integer> entry : keyToIndex.entrySet()) {
            char keyChar = entry.getKey();
            int keyIdx = entry.getValue();
            graphNodes[robotCount + keyIdx] = findPositionOf(maze, keyChar);
        }

        int[][] distanceMatrix     = new int[nodeCount][keyCount];
        int[][] requiredKeysMatrix = new int[nodeCount][keyCount];
        for (int src = 0; src < nodeCount; src++) {
            Arrays.fill(distanceMatrix[src], -1);
            bfsFromNode(
                    graphNodes[src],
                    maze,
                    rowCount, columnCount,
                    keyToIndex,
                    distanceMatrix[src],
                    requiredKeysMatrix[src]
            );
        }

        int allKeysMask = (1 << keyCount) - 1;
        PriorityQueue<SearchNode> queue =
                new PriorityQueue<>(Comparator.comparingInt(n -> n.steps));
        Map<State,Integer> bestDistanceForState = new HashMap<>();

        int[] startRobotIndices = new int[robotCount];
        for (int i = 0; i < robotCount; i++) {
            startRobotIndices[i] = i;
        }
        State initialState = new State(startRobotIndices, 0);
        queue.add(new SearchNode(initialState, 0));
        bestDistanceForState.put(initialState, 0);

        while (!queue.isEmpty()) {
            SearchNode node = queue.poll();
            State state = node.state;
            int stepsSoFar = node.steps;
            Integer knownBest = bestDistanceForState.get(state);

            if (knownBest < stepsSoFar) continue;

            if (state.keysMask == allKeysMask) {
                return stepsSoFar;
            }

            for (int robotIdx = 0; robotIdx < robotCount; robotIdx++) {
                int currentNode = state.robotNodeIndices[robotIdx];
                for (int keyIdx = 0; keyIdx < keyCount; keyIdx++) {
                    int keyBit = 1 << keyIdx;
                    if ((state.keysMask & keyBit) != 0) {
                        // ключ уже собран
                        continue;
                    }
                    int distToKey     = distanceMatrix[currentNode][keyIdx];
                    int doorsRequired = requiredKeysMatrix[currentNode][keyIdx];
                    if (distToKey < 0 || (doorsRequired & ~state.keysMask) != 0) {
                        continue;
                    }

                    int[] nextRobotIndices = state.robotNodeIndices.clone();
                    nextRobotIndices[robotIdx] = robotCount + keyIdx;
                    int nextKeysMask = state.keysMask | keyBit;
                    State nextState  = new State(nextRobotIndices, nextKeysMask);
                    int nextSteps    = stepsSoFar + distToKey;

                    Integer bestSoFar = bestDistanceForState.get(nextState);
                    if (bestSoFar == null || nextSteps < bestSoFar) {
                        bestDistanceForState.put(nextState, nextSteps);
                        queue.add(new SearchNode(nextState, nextSteps));
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
    }
    private static void bfsFromNode(
            Position start,
            char[][] maze,
            int rowCount, int columnCount,
            Map<Character,Integer> keyToIndex,
            int[] outDistance,
            int[] outRequiredKeysMask
    ) {
        boolean[][] visited = new boolean[rowCount][columnCount];
        Deque<int[]> deque  = new ArrayDeque<>();
        visited[start.row][start.column] = true;
        deque.add(new int[]{start.row, start.column, 0, 0});
        while (!deque.isEmpty()) {
            int[] entry = deque.poll();
            int r = entry[0], c = entry[1], dist = entry[2], mask = entry[3];
            char cell = maze[r][c];
            if (cell >= 'a' && cell <= 'z') {
                int kIdx = keyToIndex.get(cell);
                outDistance[kIdx] = dist;
                outRequiredKeysMask[kIdx] = mask;
            }
            for (int[] dir : DIRECTIONS) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr < 0 || nr >= rowCount || nc < 0 || nc >= columnCount) continue;
                if (visited[nr][nc]) continue;
                char nextCell = maze[nr][nc];
                if (nextCell == '#') continue;
                int nextMask = mask;
                if (nextCell >= 'A' && nextCell <= 'Z') {
                    char requiredKey = Character.toLowerCase(nextCell);
                    Integer idx = keyToIndex.get(requiredKey);
                    if (idx == null) {
                        continue;
                    }
                    nextMask |= 1 << idx;
                }
                visited[nr][nc] = true;
                deque.add(new int[]{nr, nc, dist + 1, nextMask});
            }
        }
    }

    private static Position findPositionOf(char[][] maze, char target) {
        for (int r = 0; r < maze.length; r++) {
            for (int c = 0; c < maze[r].length; c++) {
                if (maze[r][c] == target) {
                    return new Position(r, c);
                }
            }
        }
        throw new IllegalStateException("Символ “" + target + "” не найден");
    }
}
