package id.ac.ui.cs.advprog.papikos.notification.observer;

import id.ac.ui.cs.advprog.papikos.notification.model.Kos;
import id.ac.ui.cs.advprog.papikos.notification.model.Observer;
import id.ac.ui.cs.advprog.papikos.notification.service.NotificationManager;

public class WishlistObserver implements Observer {

    private NotificationManager notificationManager;

    public WishlistObserver(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Override
    public void notify(Object subject) {
        // notify setiap user yang suah menambahkan wishlist kos
        if (subject instanceof Kos kos) {

            if (kos.getAvailableRooms() > 0) {
                notificationService.notifyUsers(kos);
            }
        }
    }
}
