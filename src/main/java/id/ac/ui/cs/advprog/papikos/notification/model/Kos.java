package id.ac.ui.cs.advprog.papikos.notification.model;
import java.util.List;
import java.util.ArrayList;

public class Kos {
    public String name;
    public int amount;
    public List<Observer> observers = new ArrayList<Observer>();

    public Kos(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }
    public void setAvailableRooms(int amount) {
        this.amount = amount;
    }
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

}
