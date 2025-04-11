package id.ac.ui.cs.advprog.papikos.notification.model;

import id.ac.ui.cs.advprog.papikos.notification.model.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class KosTest {
    private Kos kos;
    private Observer observerMock;

    @BeforeEach
    void setUp() {
        kos = new Kos("Kos Gemilang", 6);
        observerMock = mock(Observer.class);
    }

    @Test
    void testAddObserverTriggersUpdateWhenRoomAvailable() {
        // misalnya tidak ada room yang tersedia
        kos.setAvailableRooms(0);

        kos.addObserver(observerMock);

        // ada kamar kos yang tersedia
        kos.setAvailableRooms(1); // tersedia kamar

        // seharusnya ada observerMock dipanggil satu kali, sebab kita notify ke user
        verify(observerMock, times(1)).notify(kos);
    }

    @Test
    void testRemoveObserverDisablesUpdate() {
        kos.addObserver(observerMock);
        kos.removeObserver(observerMock);
        kos.setAvailableRooms(1);

        // observer dari kos tidak ada, jadi tidak pernah dipanggil
        verify(observerMock, never()).notify(kos);
    }

    @Test
    void testNoUpdateWhenRoomCountUnchanged() {
        kos.setAvailableRooms(1);
        kos.addObserver(observerMock);
        kos.setAvailableRooms(1);

        // perubahan kamar dari 1 ke 1 seharusnya tidak mengirimkan notifikasi.
        verify(observerMock, never()).notify(kos);
    }

    @Test
    void testNotifyOnlyWhenChangingFromFullToAvailable() {
        kos.setAvailableRooms(0);
        kos.addObserver(observerMock);

        // Ini harus trigger observer karena status berubah dari penuh ke available
        kos.setAvailableRooms(2);

        verify(observerMock, times(1)).notify(kos);
    }
}
