import java.util.*;

public class BookMyStayApp {

    // ---------------- EXCEPTION ----------------
    static class InvalidBookingException extends Exception {
        public InvalidBookingException(String msg) {
            super(msg);
        }
    }

    // ---------------- INVENTORY ----------------
    static class RoomInventory {

        private Map<String, Integer> inventory = new HashMap<>();

        public RoomInventory() {
            inventory.put("Single Room", 1);
            inventory.put("Double Room", 1);
        }

        public int getAvailability(String type) {
            return inventory.getOrDefault(type, 0);
        }

        public void reduce(String type) throws InvalidBookingException {
            int count = inventory.getOrDefault(type, 0);
            if (count <= 0) {
                throw new InvalidBookingException("No rooms available for " + type);
            }
            inventory.put(type, count - 1);
        }

        public void increase(String type) {
            inventory.put(type, inventory.getOrDefault(type, 0) + 1);
        }

        public void display() {
            System.out.println("\nInventory:");
            inventory.forEach((k,v)-> System.out.println(k+" -> "+v));
        }
    }

    // ---------------- RESERVATION ----------------
    static class Reservation {
        String guestName;
        String roomType;

        public Reservation(String guestName, String roomType) {
            this.guestName = guestName;
            this.roomType = roomType;
        }
    }

    // ---------------- QUEUE ----------------
    static class BookingRequestQueue {
        Queue<Reservation> queue = new LinkedList<>();

        public void add(Reservation r) {
            queue.offer(r);
        }

        public Reservation next() {
            return queue.poll();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    // ---------------- BOOKING SERVICE ----------------
    static class BookingService {

        private RoomInventory inventory;

        // Track reservationId -> roomType
        private Map<String, String> confirmedBookings = new HashMap<>();

        private int counter = 100;

        public BookingService(RoomInventory inventory) {
            this.inventory = inventory;
        }

        private String generateId(String type) {
            counter++;
            return type.replace(" ", "").toUpperCase() + "-" + counter;
        }

        public Map<String, String> getConfirmedBookings() {
            return confirmedBookings;
        }

        public void process(BookingRequestQueue queue) {

            System.out.println("\nProcessing Bookings...");
            System.out.println("----------------------------------");

            while (!queue.isEmpty()) {

                Reservation r = queue.next();

                try {
                    if (r.guestName == null || r.guestName.isEmpty()) {
                        throw new InvalidBookingException("Invalid guest name");
                    }

                    if (inventory.getAvailability(r.roomType) <= 0) {
                        throw new InvalidBookingException("Room unavailable: " + r.roomType);
                    }

                    String id = generateId(r.roomType);

                    inventory.reduce(r.roomType);

                    confirmedBookings.put(id, r.roomType);

                    System.out.println("CONFIRMED: " + r.guestName + " -> " + id);

                } catch (InvalidBookingException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
        }
    }

    // ---------------- CANCELLATION SERVICE ----------------
    static class CancellationService {

        private RoomInventory inventory;
        private Map<String, String> bookings;

        // LIFO rollback structure
        private Stack<String> rollbackStack = new Stack<>();

        public CancellationService(RoomInventory inventory, Map<String, String> bookings) {
            this.inventory = inventory;
            this.bookings = bookings;
        }

        public void cancel(String reservationId) {

            System.out.println("\nAttempting cancellation for: " + reservationId);

            // Validate existence
            if (!bookings.containsKey(reservationId)) {
                System.out.println("ERROR: Reservation not found or already cancelled.");
                return;
            }

            String roomType = bookings.get(reservationId);

            // Push to rollback stack
            rollbackStack.push(reservationId);

            // Remove booking
            bookings.remove(reservationId);

            // Restore inventory
            inventory.increase(roomType);

            System.out.println("CANCELLED: " + reservationId + " | Room restored: " + roomType);
        }

        public void showRollbackStack() {
            System.out.println("\nRollback Stack (LIFO): " + rollbackStack);
        }
    }

    // ---------------- MAIN ----------------
    public static void main(String[] args) {

        RoomInventory inventory = new RoomInventory();

        BookingRequestQueue queue = new BookingRequestQueue();

        queue.add(new Reservation("Alice", "Single Room"));
        queue.add(new Reservation("Bob", "Double Room"));

        BookingService bookingService = new BookingService(inventory);

        // Process bookings
        bookingService.process(queue);

        inventory.display();

        // Cancellation
        CancellationService cancelService =
                new CancellationService(inventory, bookingService.getConfirmedBookings());

        // Get one reservation ID to cancel
        List<String> ids = new ArrayList<>(bookingService.getConfirmedBookings().keySet());

        if (!ids.isEmpty()) {
            cancelService.cancel(ids.get(0)); // valid cancel
            cancelService.cancel("INVALID-ID"); // invalid cancel
        }

        cancelService.showRollbackStack();

        // Final state
        inventory.display();
    }
}