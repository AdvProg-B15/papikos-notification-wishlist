package id.ac.ui.cs.advprog.papikos.notification.observer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import id.ac.ui.cs.advprog.papikos.notification.model.Kos;
import id.ac.ui.cs.advprog.papikos.notification.service.NotificationManager;

class ObserverTest {
    private Kos kos;
    private WishlistObserver observer;
    private NotificationManager notificationService;

    @BeforeEach
    void setup() {
        notificationService = mock(NotificationManager.class);
        observer = new WishlistObserver(notificationService);

        kos = new Kos("Kos Gemilang", 3);
        kos.addObserver(observer);
    }


    @Test
    void testShouldNotNotifyIfKosNotInWishlist() {
        kos.setAvailableRooms(2);

        verify(notificationService, never()).notifyUsers(any());
    }


    @Test
    void testShouldNotifyUsersWhenRoomBecomesAvailable() {
        kos.setAvailableRooms(0);

        kos.setAvailableRooms(1);

        verify(notificationService, times(1)).notifyUsers(kos);
    }

    @Test
    void testShouldNotNotifyWhenRoomRemainsUnavailable() {
        kos.setAvailableRooms(0);
        kos.setAvailableRooms(0);

        verify(notificationService, never()).notifyUsers(kos);
    }
}
