package vj;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Elevator {
    public static final int DOWN = -1;
    public static final int WAITING = 0;
    public static final int UP = 1;
    private static final int FLOOR_INTERVAL = 2000;
    private static final int DOOR_INTERVAL = 3000;

    private static Elevator elevator = null;
    private Integer[] requests;
    private int currentFloor;
    private int direction;
    private int lowestFloor;
    private int highestFloor;

    private Elevator(){
        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Set up new elevator:");
            System.out.print("\nNumber of floors: ");
            int totalFloors = Integer.parseInt(bufferedReader.readLine());
            System.out.print("\nStarting floors: ");
            lowestFloor = Integer.parseInt(bufferedReader.readLine());
            highestFloor = totalFloors + lowestFloor - 1;
            System.out.println("\nNew elevator setup with " + totalFloors + " floors, starting from " + lowestFloor + " ending at " + highestFloor);
            requests = new Integer[totalFloors];
            start();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    static Elevator getInstance(){
        if (elevator == null){
            elevator = new Elevator();
        }
        return elevator;
    }
    // convert floor # to queue index
    private int floorToIndex(int floor) {
        return (floor - this.lowestFloor);
    }
    // convert queue index to floor #
    private int indexToFloor(int index) {
        return (this.lowestFloor + index);
    }


    // add addFloorToRequests to requests queue
    private void addFloorToRequests(int source, int direction){
        synchronized (requests) {
            source = floorToIndex(source);
            if (requests[source] == null) {
                requests[source] = direction;
            }
        }
    }

    // call the elevator from a floor
    public void call(final int source, final int direction){
        System.out.println("Calling elevator from floor:" + source + " direction:" + direction);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                Elevator elevator = Elevator.getInstance();
                try{
                    elevator.addFloorToRequests(source, direction);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        new Thread(task, "RequestThread").start();
    }

    // select a floor to go to from inside the elevator
    public void select(){
        long timeOut = System.currentTimeMillis() + DOOR_INTERVAL;
        System.out.println("Select a floor to go to");
        while (System.currentTimeMillis()<timeOut) {
            String floorNumberStr = null;
            try {
                // Read input from console
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                floorNumberStr = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if ((floorNumberStr != null) && floorNumberStr.matches("-?\\d*")) {
                int floor = Integer.parseInt(floorNumberStr);
                if (floor >= lowestFloor && floor <= highestFloor) {
                    addFloorToRequests(Integer.parseInt(floorNumberStr), 0);
                }
            }
        }
    }

    public void start(){
        synchronized (requests) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    System.out.println("Elevator started, waiting ...");
                    while (true){
                        if(Arrays.asList(requests).contains(-1)
                                || Arrays.asList(requests).contains(0)
                                || Arrays.asList(requests).contains(1)){
                            try {
                                toNextFloor();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };
            new Thread(task, "RunningService").start();
        }
    }
    private Integer setDirection(){
        for(int i = 0; i < (highestFloor-lowestFloor+1); i++){
            if(requests[i]!=null && requests[i]!=0){
                return requests[i];
            }
        }
        return null;
    }
    private void toNextFloor() throws InterruptedException{

        if(direction == Elevator.WAITING) {
            direction = setDirection();
        }
        if (direction == Elevator.UP && currentFloor<highestFloor){
            Thread.sleep(Elevator.FLOOR_INTERVAL);
            currentFloor++;
            System.out.println("Floor: " + currentFloor);
            arriveToFloor(currentFloor, direction);
        } else if(direction == Elevator.DOWN && currentFloor>lowestFloor){
            Thread.sleep(Elevator.FLOOR_INTERVAL);
            currentFloor--;
            System.out.println("Floor: " + currentFloor);
            arriveToFloor(currentFloor, direction);
        }
    }

    private void arriveToFloor(int floor, int currentDirection) throws NullPointerException, InterruptedException{
        int floorIndex = floorToIndex(floor);
        int changeDirection = -1;


        if(currentDirection==Elevator.UP){
            while (changeDirection==-1 && floorIndex < highestFloor-lowestFloor){
                floorIndex++;
                if(requests[floorIndex]!=null){
                    changeDirection=1;
                }
            }
        } else if (currentDirection==Elevator.DOWN) {
            while (changeDirection == -1 && floorIndex > 0) {
                floorIndex--;
                if (requests[floorIndex] != null) {
                    changeDirection = 1;
                }
            }
        }

        floorIndex = floorToIndex(floor);
        // if floor is requested -> open door
        if((requests[floorIndex] != null)
                && ((requests[floorIndex] == 0) || (requests[floorIndex] == currentDirection)
                || (floor == highestFloor) || floor == lowestFloor || changeDirection==-1)){
            open(floorIndex);
        }
        // if not, continue travelling
        if(floor == highestFloor || floor == lowestFloor){
            changeDirection=-1;
        }
        direction = currentDirection * changeDirection;

    }
    private void open(int floorIndex) throws InterruptedException{
        System.out.println("Door opened.");
        select();
        requests[floorIndex] = null;
        System.out.println("Door closed.");
    }

}
